package demo.chess.analysis.annotation;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import demo.chess.definitions.Color;
import demo.chess.definitions.PieceType;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.EnPassant;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.pieces.Piece;
import demo.chess.definitions.players.Player;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.impl.Simulation;

/**
 * Detects causal passive material sacrifices.
 *
 * <p>A material offer only counts when the candidate move is genuinely related
 * to the loss:</p>
 * <ul>
 *   <li>the move newly exposes another piece to a legal material-winning
 *       capture; or</li>
 *   <li>the piece was already threatened, had a legal move to safety, and the
 *       candidate deliberately leaves it en prise.</li>
 * </ul>
 *
 * <p>Merely having some unrelated piece already hanging is not enough. All
 * captures after the candidate are generated from a normal simulation so
 * pseudo-legal moves that leave the opponent king in check cannot create false
 * sacrifice evidence.</p>
 */
final class MaterialOfferDetector {

    MaterialSacrificeEvidence find(
            Game rootPosition,
            String playedMoveUci,
            boolean whiteMover) {
        if (rootPosition == null
                || playedMoveUci == null
                || playedMoveUci.isBlank()) {
            return null;
        }

        try {
            Game afterRoot =
                    Simulation.forkSimulationFrom(rootPosition.getMoveList());
            double rootBalance =
                    MaterialInvestmentDetector.materialBalanceForMover(
                            afterRoot,
                            whiteMover);

            Move rootMove =
                    LegalMoveResolver.resolveUci(afterRoot, playedMoveUci);
            afterRoot.apply(rootMove);

            MaterialSacrificeEvidence best = null;
            List<Move> legalOpponentMoves =
                    afterRoot.getPlayer().getValidMoves(afterRoot);

            for (Move opponentMove : legalOpponentMoves) {
                Piece captured = capturedPiece(opponentMove);
                if (!isCandidateOfferedPiece(captured, rootMove, whiteMover)) {
                    continue;
                }

                double value = effectiveOfferValue(
                        afterRoot,
                        opponentMove,
                        whiteMover,
                        rootBalance);
                if (value
                        < MoveAnnotationPolicy.BRILLIANT_MATERIAL_INVESTMENT) {
                    continue;
                }

                Piece rootPiece = correspondingRootPiece(
                        rootPosition,
                        captured);
                if (rootPiece == null) {
                    continue;
                }

                MaterialSacrificeType type;
                if (!hasLegalHypotheticalCapture(
                        rootPosition,
                        rootPiece,
                        whiteMover)) {
                    type = MaterialSacrificeType.NEW_MATERIAL_OFFER;
                } else if (hasLegalMoveToSafety(
                        rootPosition,
                        rootPiece)) {
                    type = MaterialSacrificeType.DECLINED_MATERIAL_SAVE;
                } else {
                    /*
                     * The piece was already genuinely hanging and had no legal
                     * way out. Playing an unrelated move must not earn
                     * sacrifice credit for an unavoidable loss.
                     */
                    continue;
                }

                if (best == null || value > best.getValue()) {
                    best = new MaterialSacrificeEvidence(type, value);
                }
            }

            return best;
        } catch (Exception ignored) {
            // Annotation heuristics must never make DeepAnalysis fail.
            return null;
        }
    }

    double calculate(
            Game rootPosition,
            String playedMoveUci,
            boolean whiteMover) {
        MaterialSacrificeEvidence evidence =
                find(rootPosition, playedMoveUci, whiteMover);
        return evidence != null ? evidence.getValue() : 0.0;
    }

    private boolean isCandidateOfferedPiece(
            Piece captured,
            Move rootMove,
            boolean whiteMover) {
        if (captured == null
                || captured.getField() == null
                || captured.getType() == PieceType.KING) {
            return false;
        }

        boolean capturedBelongsToMover =
                whiteMover
                        ? captured.getColor() == Color.WHITE
                        : captured.getColor() == Color.BLACK;
        if (!capturedBelongsToMover) {
            return false;
        }

        /*
         * Sacrificing the piece that actually moved is handled by
         * MaterialInvestmentDetector. Passive offers concern another piece.
         */
        return captured != rootMove.getPiece();
    }

    private double effectiveOfferValue(
            Game afterRoot,
            Move opponentMove,
            boolean whiteMover,
            double rootBalance)
            throws NoMoveFoundException, IOException {
        Game afterCapture =
                Simulation.forkSimulationFrom(afterRoot.getMoveList());
        Move replayedCapture = LegalMoveResolver.resolveUci(
                afterCapture,
                opponentMove.toString());
        int captureFile = replayedCapture.getTarget().getFile();
        int captureRank = replayedCapture.getTarget().getRank();
        afterCapture.apply(replayedCapture);

        Piece capturingPiece = afterCapture.getChessBoard()
                .getField(captureFile, captureRank)
                .getPiece();

        double balanceAfterCapture =
                MaterialInvestmentDetector.materialBalanceForMover(
                        afterCapture,
                        whiteMover);
        double immediateRecovery =
                directRecaptureValue(afterCapture, capturingPiece);
        double stabilizedBalance =
                balanceAfterCapture + immediateRecovery;

        return Math.max(
                0.0,
                rootBalance - stabilizedBalance);
    }

    private Piece correspondingRootPiece(
            Game rootPosition,
            Piece afterRootPiece) {
        if (afterRootPiece == null || afterRootPiece.getField() == null) {
            return null;
        }

        Piece rootPiece = rootPosition.getChessBoard()
                .getField(
                        afterRootPiece.getField().getFile(),
                        afterRootPiece.getField().getRank())
                .getPiece();

        if (rootPiece == null
                || rootPiece.getColor() != afterRootPiece.getColor()
                || rootPiece.getType() != afterRootPiece.getType()) {
            return null;
        }
        return rootPiece;
    }

    /**
     * Tests whether the opponent could legally capture the piece in the root
     * position if it were their turn. This is used only to distinguish a newly
     * created offer from an already existing threat.
     */
    private boolean hasLegalHypotheticalCapture(
            Game rootPosition,
            Piece target,
            boolean whiteMover)
            throws NoMoveFoundException, IOException {
        Player opponent = whiteMover
                ? rootPosition.getBlackPlayer()
                : rootPosition.getWhitePlayer();

        for (Move candidate : opponent.getSimpleMoves()) {
            if (capturedPiece(candidate) != target) {
                continue;
            }
            if (isHypotheticalMoveKingSafe(
                    rootPosition,
                    candidate,
                    opponent.getColor())) {
                return true;
            }
        }

        return false;
    }

    private boolean isHypotheticalMoveKingSafe(
            Game rootPosition,
            Move candidate,
            Color opponentColor)
            throws NoMoveFoundException, IOException {
        Game simulation =
                Simulation.forkSimulationFrom(rootPosition.getMoveList());
        Player simulatedOpponent = opponentColor == Color.WHITE
                ? simulation.getWhitePlayer()
                : simulation.getBlackPlayer();
        simulation.setPlayer(simulatedOpponent);

        Move replayed =
                simulatedOpponent.getMoveInSimulation(simulation, candidate);
        simulation.apply(replayed);

        Piece king = simulatedOpponent.getKing();
        if (king == null || king.getField() == null) {
            return false;
        }

        return simulation.getPlayer().getSimpleMoves().stream()
                .map(Move::getTarget)
                .filter(Objects::nonNull)
                .noneMatch(king.getField()::equals);
    }

    /**
     * A pre-existing threat counts as a deliberate declined save only if the
     * threatened piece itself had at least one legal move that would have
     * removed the immediate capture threat.
     *
     * <p>This is intentionally conservative. Defensive alternatives by other
     * pieces are not credited here because doing so would make unrelated moves
     * look like sacrifices again.</p>
     */
    private boolean hasLegalMoveToSafety(
            Game rootPosition,
            Piece threatenedPiece)
            throws NoMoveFoundException, IOException {
        if (threatenedPiece == null
                || threatenedPiece.getField() == null
                || threatenedPiece.getType() == PieceType.KING) {
            return false;
        }

        int sourceFile = threatenedPiece.getField().getFile();
        int sourceRank = threatenedPiece.getField().getRank();

        for (Move candidate :
                rootPosition.getPlayer().getValidMoves(rootPosition)) {
            if (candidate.getSource() == null
                    || candidate.getSource().getFile() != sourceFile
                    || candidate.getSource().getRank() != sourceRank) {
                continue;
            }

            Game afterSave =
                    Simulation.forkSimulationFrom(rootPosition.getMoveList());
            Move replayed =
                    LegalMoveResolver.resolveUci(
                            afterSave,
                            candidate.toString());
            int targetFile = replayed.getTarget().getFile();
            int targetRank = replayed.getTarget().getRank();
            afterSave.apply(replayed);

            Piece savedPiece = afterSave.getChessBoard()
                    .getField(targetFile, targetRank)
                    .getPiece();
            if (savedPiece != null
                    && !isLegallyCapturable(afterSave, savedPiece)) {
                return true;
            }
        }

        return false;
    }

    private boolean isLegallyCapturable(
            Game position,
            Piece target)
            throws NoMoveFoundException, IOException {
        for (Move reply : position.getPlayer().getValidMoves(position)) {
            if (capturedPiece(reply) == target) {
                return true;
            }
        }
        return false;
    }

    private double directRecaptureValue(
            Game game,
            Piece capturingPiece)
            throws NoMoveFoundException, IOException {
        if (capturingPiece == null) {
            return 0.0;
        }

        for (Move reply : game.getPlayer().getValidMoves(game)) {
            Piece captured = capturedPiece(reply);
            if (captured == capturingPiece) {
                return MaterialInvestmentDetector.pieceValue(
                        capturingPiece.getType());
            }
        }

        return 0.0;
    }

    private Piece capturedPiece(Move move) {
        if (move instanceof EnPassant enPassant) {
            return enPassant.getSlayedPiece();
        }
        return move != null && move.getTarget() != null
                ? move.getTarget().getPiece()
                : null;
    }
}
