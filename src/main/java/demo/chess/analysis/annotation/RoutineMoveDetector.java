package demo.chess.analysis.annotation;

import java.util.List;
import java.util.Objects;

import demo.chess.definitions.Color;
import demo.chess.definitions.PieceType;
import demo.chess.definitions.moves.Castling;
import demo.chess.definitions.moves.EnPassant;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.Promotion;
import demo.chess.definitions.pieces.Piece;
import demo.chess.definitions.players.Player;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.impl.Simulation;

/**
 * Recognizes mechanically natural replies that should not receive a positive
 * annotation merely because alternatives evaluate badly.
 */
final class RoutineMoveDetector {

    boolean isRoutine(
            Game rootPosition,
            String playedMoveUci,
            boolean whiteMover) {
        if (rootPosition == null
                || playedMoveUci == null
                || playedMoveUci.isBlank()) {
            return false;
        }

        try {
            List<Move> legalMoves =
                    rootPosition.getPlayer().getValidMoves(rootPosition);
            if (legalMoves.size() == 1) {
                return true;
            }

            PreviousMoveContext previous = previousMove(rootPosition);
            Game afterMove = Simulation.forkSimulationFrom(rootPosition);
            Move candidate = LegalMoveResolver.resolveUci(
                    afterMove,
                    playedMoveUci);
            Piece captured = capturedPiece(candidate);
            int sourceFile = candidate.getSource().getFile();
            int sourceRank = candidate.getSource().getRank();
            int targetFile = candidate.getTarget().getFile();
            int targetRank = candidate.getTarget().getRank();

            if (candidate instanceof Promotion
                    || candidate instanceof Castling) {
                return false;
            }

            afterMove.apply(candidate);
            if (givesCheck(afterMove, whiteMover)) {
                return false;
            }

            if (captured != null
                    && isEvenImmediateRecapture(
                            previous,
                            candidate,
                            captured)) {
                return true;
            }

            if (captured == null
                    && previous != null
                    && newlyAttackedByPreviousMove(
                            rootPosition,
                            previous,
                            sourceFile,
                            sourceRank)
                    && !isLegallyCapturable(
                            afterMove,
                            targetFile,
                            targetRank,
                            opposite(whiteMover))) {
                return true;
            }
        } catch (Exception ignored) {
            // A heuristic must never prevent analysis from completing.
        }
        return false;
    }

    private boolean isEvenImmediateRecapture(
            PreviousMoveContext previous,
            Move candidate,
            Piece captured) {
        if (previous == null
                || previous.capturedType() == null
                || candidate instanceof EnPassant
                || candidate.getTarget().getFile() != previous.targetFile()
                || candidate.getTarget().getRank() != previous.targetRank()
                || captured.getType() != previous.movingType()) {
            return false;
        }

        double capturedValue = MaterialInvestmentDetector.pieceValue(
                previous.capturedType());
        double movingValue = MaterialInvestmentDetector.pieceValue(
                previous.movingType());
        return Math.abs(capturedValue - movingValue) < 0.01;
    }

    private boolean newlyAttackedByPreviousMove(
            Game rootPosition,
            PreviousMoveContext previous,
            int file,
            int rank) throws Exception {
        Color attacker = previous.movingColor();
        return !isLegallyCapturable(
                        previous.positionBeforeMove(),
                        file,
                        rank,
                        attacker)
                && isLegallyCapturable(
                        rootPosition,
                        file,
                        rank,
                        attacker);
    }

    private boolean isLegallyCapturable(
            Game position,
            int file,
            int rank,
            Color attackerColor) throws Exception {
        Game simulation = Simulation.forkSimulationFrom(position);
        Piece target = simulation.getChessBoard()
                .getField(file, rank)
                .getPiece();
        if (target == null || target.getType() == PieceType.KING) {
            return false;
        }

        Player attacker = attackerColor == Color.WHITE
                ? simulation.getWhitePlayer()
                : simulation.getBlackPlayer();
        simulation.setPlayer(attacker);
        for (Move move : attacker.getValidMoves(simulation)) {
            if (capturedPiece(move) == target) {
                return true;
            }
        }
        return false;
    }

    private PreviousMoveContext previousMove(Game rootPosition)
            throws Exception {
        if (rootPosition.getMoveList().isEmpty()) {
            return null;
        }

        Game before = Simulation.createSimulation(
                rootPosition.getStartingPosition());
        int lastIndex = rootPosition.getMoveList().size() - 1;
        for (int index = 0; index < lastIndex; index++) {
            Move replayed = before.getPlayer().getMoveInSimulation(
                    before,
                    rootPosition.getMoveList().get(index));
            before.apply(replayed);
        }

        Move lastMove = before.getPlayer().getMoveInSimulation(
                before,
                rootPosition.getMoveList().get(lastIndex));
        Piece captured = capturedPiece(lastMove);
        return new PreviousMoveContext(
                before,
                lastMove.getPiece().getColor(),
                lastMove.getPiece().getType(),
                captured != null ? captured.getType() : null,
                lastMove.getTarget().getFile(),
                lastMove.getTarget().getRank());
    }

    private boolean givesCheck(Game afterMove, boolean whiteMover) {
        Player mover = whiteMover
                ? afterMove.getWhitePlayer()
                : afterMove.getBlackPlayer();
        Player opponent = whiteMover
                ? afterMove.getBlackPlayer()
                : afterMove.getWhitePlayer();
        if (opponent.getKing() == null
                || opponent.getKing().getField() == null) {
            return false;
        }
        return mover.getSimpleMoves().stream()
                .map(Move::getTarget)
                .filter(Objects::nonNull)
                .anyMatch(opponent.getKing().getField()::equals);
    }

    private Color opposite(boolean whiteMover) {
        return whiteMover ? Color.BLACK : Color.WHITE;
    }

    private Piece capturedPiece(Move move) {
        if (move instanceof EnPassant enPassant) {
            return enPassant.getSlayedPiece();
        }
        return move != null && move.getTarget() != null
                ? move.getTarget().getPiece()
                : null;
    }

    private record PreviousMoveContext(
            Game positionBeforeMove,
            Color movingColor,
            PieceType movingType,
            PieceType capturedType,
            int targetFile,
            int targetRank) {
    }
}
