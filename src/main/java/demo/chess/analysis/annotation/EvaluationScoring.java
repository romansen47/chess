package demo.chess.analysis.annotation;

import java.util.ArrayList;
import java.util.List;

import demo.chess.definitions.engines.EngineLine;

final class EvaluationScoring {

    private EvaluationScoring() {
    }

    static double moverScore(double whiteEvaluation, boolean whiteMover) {
        return whiteMover ? whiteEvaluation : -whiteEvaluation;
    }

    static double winPercentFromMoverScore(double score) {
        double centipawns = score * 100.0;
        return 50.0 + 50.0 * (2.0 / (1.0 + Math.exp(-0.00368208 * centipawns)) - 1.0);
    }

    static double winningChance(EngineLine line, boolean whiteMover) {
        if (line == null) {
            return 0.0;
        }
        return winPercentFromMoverScore(
                moverScore(line.getEvaluation(), whiteMover));
    }

    static double regret(
            EngineLine best,
            EngineLine played,
            boolean whiteMover) {
        return Math.max(
                0.0,
                winningChance(best, whiteMover)
                        - winningChance(played, whiteMover));
    }

    static double gap(
            EngineLine better,
            EngineLine worse,
            boolean whiteMover) {
        return winningChance(better, whiteMover)
                - winningChance(worse, whiteMover);
    }

    /**
     * Retains the engine's MultiPV order while removing unusable lines.
     *
     * <p>The UCI {@code multipv} index is the engine's authoritative ranking.
     * Numeric scores from different PV searches are not guaranteed to remain
     * strictly ordered, especially for WDL-based engines such as Leela. Sorting
     * the lines again by their displayed score can therefore replace the
     * engine's first choice with its second or third choice.</p>
     */
    static List<EngineLine> orderedLines(List<EngineLine> lines) {
        List<EngineLine> ordered = new ArrayList<>();
        if (lines != null) {
            ordered.addAll(lines);
        }
        ordered.removeIf(line -> line == null || firstMove(line) == null);
        return List.copyOf(ordered);
    }

    static String firstMove(EngineLine line) {
        if (line == null || line.getMoves() == null || line.getMoves().isBlank()) {
            return null;
        }
        return line.getMoves().trim().split("\\s+")[0].toLowerCase();
    }

    static int findMoveIndex(List<EngineLine> ranked, String playedMoveUci) {
        if (playedMoveUci == null) {
            return -1;
        }
        String wanted = playedMoveUci.trim().toLowerCase();
        for (int index = 0; index < ranked.size(); index++) {
            if (wanted.equals(firstMove(ranked.get(index)))) {
                return index;
            }
        }
        return -1;
    }
}
