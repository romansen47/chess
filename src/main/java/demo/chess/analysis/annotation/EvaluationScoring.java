package demo.chess.analysis.annotation;

import java.util.ArrayList;
import java.util.Comparator;
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

    static List<EngineLine> rankLines(List<EngineLine> lines, boolean whiteMover) {
        List<EngineLine> ranked = new ArrayList<>();
        if (lines != null) {
            ranked.addAll(lines);
        }
        ranked.removeIf(line -> line == null || firstMove(line) == null);
        ranked.sort(Comparator.comparingDouble(
                (EngineLine line) -> moverScore(line.getEvaluation(), whiteMover))
                .reversed());
        return ranked;
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
