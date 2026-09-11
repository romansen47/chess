package demo.chess.analysis.annotation;

import demo.chess.definitions.Color;
import demo.chess.definitions.PieceType;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.pieces.Piece;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.impl.Simulation;

final class MaterialInvestmentDetector {

    double calculate(
            Game rootPosition,
            EngineLine line,
            boolean whiteMover,
            int maxPlies) {
        if (rootPosition == null || line == null || line.getMoves() == null
                || line.getMoves().isBlank() || maxPlies <= 0) {
            return 0.0;
        }

        try {
            Game simulation = Simulation.forkDummyFrom(rootPosition.getMoveList());
            double rootBalance = materialBalanceForMover(simulation, whiteMover);
            double lowestBalance = rootBalance;

            String[] moves = line.getMoves().trim().split("\\s+");
            int count = Math.min(maxPlies, moves.length);
            for (int index = 0; index < count; index++) {
                Move move = LegalMoveResolver.resolveUci(simulation, moves[index]);
                simulation.apply(move);
                lowestBalance = Math.min(
                        lowestBalance,
                        materialBalanceForMover(simulation, whiteMover));
            }

            return Math.max(0.0, rootBalance - lowestBalance);
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
