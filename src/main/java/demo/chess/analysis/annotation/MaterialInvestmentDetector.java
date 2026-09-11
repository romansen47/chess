package demo.chess.analysis.annotation;

import demo.chess.definitions.Color;
import demo.chess.definitions.PieceType;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.Promotion;
import demo.chess.definitions.pieces.Piece;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.impl.Simulation;

final class MaterialInvestmentDetector {

    /**
     * Measures a material sacrifice that is causally linked to the piece moved
     * by the candidate root move.
     *
     * <p>The previous implementation simply looked for the worst material
     * balance anywhere in the next few plies. That could attribute an unrelated
     * later exchange to the root move. Here the moved piece itself must actually
     * be captured within the horizon. One immediate reply by the mover is then
     * allowed to recover material, so ordinary exchanges are not mistaken for
     * sacrifices.</p>
     */
    double calculate(
            Game rootPosition,
            EngineLine line,
            boolean whiteMover,
            int maxPlies) {
        if (rootPosition == null || line == null || line.getMoves() == null
                || line.getMoves().isBlank() || maxPlies < 2) {
            return 0.0;
        }

        try {
            Game simulation = Simulation.forkDummyFrom(rootPosition.getMoveList());
            String[] moves = line.getMoves().trim().split("\\s+");
            int count = Math.min(maxPlies, moves.length);
            if (count < 2) {
                return 0.0;
            }

            double rootBalance = materialBalanceForMover(simulation, whiteMover);

            Move rootMove = LegalMoveResolver.resolveUci(simulation, moves[0]);
            if (rootMove instanceof Promotion) {
                // Promotion replaces the pawn object, so the normal moved-piece
                // identity rule would report a false sacrifice.
                return 0.0;
            }

            Piece investedPiece = rootMove.getPiece();
            if (investedPiece == null || investedPiece.getType() == PieceType.KING) {
                return 0.0;
            }

            simulation.apply(rootMove);

            for (int index = 1; index < count; index++) {
                Move continuation = LegalMoveResolver.resolveUci(simulation, moves[index]);
                simulation.apply(continuation);

                if (investedPiece.getField() != null) {
                    continue;
                }

                double balanceAfterCapture =
                        materialBalanceForMover(simulation, whiteMover);
                double stabilizedBalance = balanceAfterCapture;

                // Give the sacrificing side one immediate reply to recover
                // material. This filters normal exchange sequences such as
                // Bxc6 dxc6 without erasing genuine temporary sacrifices whose
                // compensation lies deeper than a simple recapture.
                if (index + 1 < count) {
                    Move recovery = LegalMoveResolver.resolveUci(
                            simulation,
                            moves[index + 1]);
                    simulation.apply(recovery);
                    double balanceAfterRecovery =
                            materialBalanceForMover(simulation, whiteMover);

                    // Credit material that was recovered immediately, but do
                    // not attribute an unrelated additional loss on that reply
                    // to the original sacrifice.
                    stabilizedBalance = Math.max(
                            balanceAfterCapture,
                            balanceAfterRecovery);
                }

                return Math.max(0.0, rootBalance - stabilizedBalance);
            }

            return 0.0;
        } catch (Exception ignored) {
            // Annotation heuristics must never make DeepAnalysis fail merely
            // because an engine PV cannot be replayed completely.
            return 0.0;
        }
    }

    private double materialBalanceForMover(Game game, boolean whiteMover) {
        double whiteBalance = whiteMaterialBalance(game.getChessBoard());
        return whiteMover ? whiteBalance : -whiteBalance;
    }

    private double whiteMaterialBalance(Board board) {
        double result = 0.0;
        for (int file = 1; file <= 8; file++) {
            for (int rank = 1; rank <= 8; rank++) {
                Field field = board.getField(file, rank);
                Piece piece = field != null ? field.getPiece() : null;
                if (piece == null) {
                    continue;
                }
                double value = pieceValue(piece.getType());
                result += piece.getColor() == Color.WHITE ? value : -value;
            }
        }
        return result;
    }

    private double pieceValue(PieceType type) {
        if (type == null) {
            return 0.0;
        }
        return switch (type) {
            case PAWN -> 1.0;
            case KNIGHT, BISHOP -> 3.0;
            case ROOK -> 5.0;
            case QUEEN -> 9.0;
            case KING -> 0.0;
        };
    }
}
