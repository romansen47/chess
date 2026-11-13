package demo.chess.analysis.annotation;

import java.util.ArrayList;
import java.util.List;

import demo.chess.definitions.PieceType;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.moves.EnPassant;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.Promotion;
import demo.chess.definitions.pieces.Piece;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.impl.Simulation;

/**
 * Replays one engine PV and records material balances plus the causal exchange
 * around the root-moved piece.
 *
 * <p>This class describes what happened in the line. Thresholds and annotation
 * decisions remain the responsibility of {@link MaterialSacrificeDetector}.</p>
 */
final class MaterialTrajectoryAnalyzer {
    Trajectory analyze(Game rootPosition, EngineLine line, boolean whiteMover) {
        if (rootPosition == null || line == null || line.getMoves() == null
                || line.getMoves().isBlank()) {
            return null;
        }
        try {
            Game simulation = Simulation.forkDummyFrom(rootPosition.getMoveList());
            List<Double> balances = new ArrayList<>();
            balances.add(MaterialInvestmentDetector.materialBalanceForMover(simulation, whiteMover));
            String[] moves = line.getMoves().trim().split("\\s+");
            Piece investedPiece = null;
            Piece investedPieceCapturer = null;
            Integer capturePly = null;
            Integer capturerCapturePly = null;
            for (int index = 0; index < moves.length; index++) {
                Move move = LegalMoveResolver.resolveUci(simulation, moves[index]);
                if (index == 0 && !(move instanceof Promotion)
                        && move.getPiece().getType() != PieceType.KING) {
                    investedPiece = move.getPiece();
                }
                Piece capturedPiece = capturedPiece(move);
                simulation.apply(move);
                balances.add(MaterialInvestmentDetector.materialBalanceForMover(simulation, whiteMover));
                if (investedPiece != null && capturePly == null
                        && capturedPiece == investedPiece) {
                    capturePly = index + 1;
                    investedPieceCapturer = pieceOnTarget(simulation, move);
                } else if (investedPieceCapturer != null
                        && capturerCapturePly == null
                        && capturedPiece == investedPieceCapturer) {
                    capturerCapturePly = index + 1;
                }
            }
            return new Trajectory(
                    List.copyOf(balances),
                    List.of(moves),
                    capturePly,
                    capturerCapturePly);
        } catch (Exception ignored) {
            // An incomplete/illegal PV is unknown, never proof of material recovery.
            return null;
        }
    }

    private Piece capturedPiece(Move move) {
        if (move instanceof EnPassant enPassant) {
            return enPassant.getSlayedPiece();
        }
        return move != null && move.getTarget() != null
                ? move.getTarget().getPiece()
                : null;
    }

    private Piece pieceOnTarget(Game simulation, Move move) {
        if (move == null || move.getTarget() == null) {
            return null;
        }
        return simulation.getChessBoard()
                .getField(
                        move.getTarget().getFile(),
                        move.getTarget().getRank())
                .getPiece();
    }

    /** Index zero is the root balance; subsequent indices are one-based PV plies. */
    record Trajectory(
            List<Double> balances,
            List<String> moves,
            Integer movedPieceCapturePly,
            Integer movedPieceCapturerCapturePly) {
        double maximumDeficit() {
            return balances.stream().mapToDouble(balance -> Math.max(0, balances.get(0) - balance))
                    .max().orElse(0);
        }

        double persistentDeficit() {
            return Math.max(0, balances.get(0) - balances.get(balances.size() - 1));
        }

        double recoveredMaterial() {
            return maximumDeficit() - persistentDeficit();
        }

        Integer recoveryPly(int acceptancePly) {
            for (int ply = acceptancePly + 1; ply < balances.size(); ply++) {
                if (balances.get(ply) >= balances.get(0)) {
                    return ply;
                }
            }
            return null;
        }

        double remainingActiveInvestment(double initialInvestment) {
            if (movedPieceCapturePly == null
                    || movedPieceCapturerCapturePly == null) {
                return initialInvestment;
            }

            /*
             * Settle the exchange exactly when the piece that accepted the
             * sacrifice is captured. Material changes after this causal
             * closure belong to later play and must not rewrite the sacrifice.
             */
            double balanceAtClosure =
                    balances.get(movedPieceCapturerCapturePly);
            double deficitAtClosure =
                    Math.max(0, balances.get(0) - balanceAtClosure);
            return Math.min(initialInvestment, deficitAtClosure);
        }
    }
}
