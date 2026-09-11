package demo.chess.analysis.annotation;

import java.util.List;

import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.EnPassant;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.Promotion;
import demo.chess.definitions.pieces.Piece;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.impl.Simulation;

/**
 * Detects an immediate material offer created or deliberately left available
 * by the candidate root move.
 *
 * <p>This complements {@link MaterialInvestmentDetector}: a brilliant move can
 * offer a piece other than the one that moved. Byrne-Fischer 1956
 * {@code 17...Be6!!} is the reference case: the bishop move deliberately leaves
 * the black queen on b6 available to {@code Bxb6}. The moved bishop itself is
 * not the sacrifice.</p>
 */
final class MaterialOfferDetector {

    double calculate(
            Game rootPosition,
            String playedMoveUci,
            boolean whiteMover) {
        if (rootPosition == null || playedMoveUci == null || playedMoveUci.isBlank()) {
            return 0.0;
        }

        try {
            Game afterRoot = Simulation.forkDummyFrom(rootPosition.getMoveList());
            double rootBalance =
                    MaterialInvestmentDetector.materialBalanceForMover(
                            afterRoot,
                            whiteMover);

            Move rootMove = LegalMoveResolver.resolveUci(afterRoot, playedMoveUci);
            afterRoot.apply(rootMove);

            List<Move> opponentMoves = afterRoot.getPlayer().getValidMoves(afterRoot);
            double largestOffer = 0.0;

            for (Move opponentMove : opponentMoves) {
                Piece capturedPiece = capturedPiece(opponentMove);
                if (capturedPiece == null
                        || capturedPiece.getColor()
                                == opponentMove.getPiece().getColor()) {
                    continue;
                }

                Game afterCapture =
                        Simulation.forkDummyFrom(afterRoot.getMoveList());
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

                largestOffer = Math.max(
                        largestOffer,
                        Math.max(0.0, rootBalance - stabilizedBalance));
            }

            return largestOffer;
        } catch (Exception ignored) {
            // Annotation heuristics must never make DeepAnalysis fail.
            return 0.0;
        }
    }

    private double directRecaptureValue(
            Game game,
            Piece capturingPiece)
            throws NoMoveFoundException, java.io.IOException {
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
        return move.getTarget() != null ? move.getTarget().getPiece() : null;
    }
}
