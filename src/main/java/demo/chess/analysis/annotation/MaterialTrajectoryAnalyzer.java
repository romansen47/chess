package demo.chess.analysis.annotation;

import java.util.ArrayList;
import java.util.List;

import demo.chess.definitions.PieceType;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.Promotion;
import demo.chess.definitions.pieces.Piece;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.impl.Simulation;

/** Replays the supplied PV once, without requesting any additional engine work. */
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
            Integer capturePly = null;
            for (int index = 0; index < moves.length; index++) {
                Move move = LegalMoveResolver.resolveUci(simulation, moves[index]);
                if (index == 0 && !(move instanceof Promotion)
                        && move.getPiece().getType() != PieceType.KING) {
                    investedPiece = move.getPiece();
                }
                simulation.apply(move);
                balances.add(MaterialInvestmentDetector.materialBalanceForMover(simulation, whiteMover));
                if (investedPiece != null && capturePly == null && investedPiece.getField() == null) {
                    capturePly = index + 1;
                }
            }
            return new Trajectory(List.copyOf(balances), List.of(moves), capturePly);
        } catch (Exception ignored) {
            // An incomplete/illegal PV is unknown, never proof of material recovery.
            return null;
        }
    }

    /** Index zero is the root balance; subsequent indices are one-based PV plies. */
    record Trajectory(List<Double> balances, List<String> moves, Integer movedPieceCapturePly) {
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

        double remainingInvestment(int acceptancePly, double initialInvestment) {
            // Once this investment has been recovered, a later unrelated loss
            // must not turn it into a sacrifice again.
            double remaining = initialInvestment;
            for (int ply = acceptancePly; ply < balances.size(); ply++) {
                remaining = Math.min(remaining, Math.max(0, balances.get(0) - balances.get(ply)));
            }
            return remaining;
        }
    }
}
