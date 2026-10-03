package demo.chess.definitions.engines.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import demo.chess.definitions.Color;
import demo.chess.definitions.engines.EngineConfig;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.engines.EvaluationEngine;
import demo.chess.definitions.engines.uci.UciInfoLine;
import demo.chess.game.Game;

/**
 * Shared infrastructure for finite and infinite native UCI analysis.
 *
 * <p>The class owns evaluation caching and conversion of parsed UCI PV
 * snapshots into CAT {@link EngineLine}s. Search orchestration stays in the
 * concrete live/deep-analysis adapters.</p>
 */
public abstract class AbstractUciAnalysisEngine extends ConsoleUciEngine implements EvaluationEngine {

    private final Map<String, List<EngineLine>> cachedBestLines = new ConcurrentHashMap<>();

    protected AbstractUciAnalysisEngine(String path) throws Exception {
        super(path);
    }

    @Override
    public final void clearCachedLines() {
        cachedBestLines.clear();
    }

    protected final String positionKey(Game game) {
        return UciPositionCommand.build(game);
    }

    protected final List<EngineLine> getCachedLines(String positionKey) {
        return cachedBestLines.get(positionKey);
    }

    protected final void cacheLines(String positionKey, List<EngineLine> lines) {
        cachedBestLines.put(
                positionKey,
                lines == null ? List.of() : List.copyOf(lines));
    }

    protected final void cacheEmptyLines(String positionKey) {
        cachedBestLines.put(positionKey, List.of());
    }

    protected List<EngineLine> parseBestLines(
            Color color,
            List<UciInfoLine> infoLines,
            EngineConfig config) {
        return parseBestLines(
                color,
                infoLines,
                config,
                Math.max(0, config.getDepth()));
    }

    protected List<EngineLine> parseBestLinesAtHighestDepth(
            Color color,
            List<UciInfoLine> infoLines,
            EngineConfig config) {
        int maxDepth = infoLines.stream()
                .mapToInt(UciInfoLine::depth)
                .max()
                .orElse(0);
        if (maxDepth == 0) return List.of();

        List<UciInfoLine> highestDepthLines = infoLines.stream()
                .filter(line -> line.depth() == maxDepth)
                .toList();
        return parseBestLines(color, highestDepthLines, config, 0);
    }

    protected List<EngineLine> parseBestLines(
            Color color,
            List<UciInfoLine> infoLines,
            EngineConfig config,
            int minimumDepth) {
        int requestedVariants = Math.max(1, config.getIntOption("MultiPV", 1));
        TreeMap<Integer, Map<Integer, EngineLine>> linesByDepth = new TreeMap<>();

        for (UciInfoLine info : infoLines) {
            int currentDepth = info.depth();
            if (currentDepth < minimumDepth) continue;
            int multiPv = info.multiPv();
            if (multiPv < 1 || multiPv > requestedVariants) continue;

            Map<Integer, EngineLine> depthLines =
                    linesByDepth.computeIfAbsent(currentDepth, ignored -> new TreeMap<>());

            double parsedValue;
            Integer mateDistance = null;
            if (info.isMateScore()) {
                int mateScore = info.mateScore();
                if (mateScore == 0) {
                    depthLines.remove(multiPv);
                    logger.debug(
                            "Ignoring transient UCI score mate 0 at depth {} multipv {}",
                            currentDepth,
                            multiPv);
                    continue;
                }
                parsedValue = Integer.signum(mateScore) * 99d;
                mateDistance = Math.abs(mateScore);
            } else {
                parsedValue = info.centipawnScore() / 100.0;
            }

            double factor = color.equals(Color.BLACK) ? -1 : 1;
            depthLines.put(
                    multiPv,
                    new EngineLine(
                            factor * parsedValue,
                            currentDepth,
                            mateDistance,
                            info.pv()));
        }

        List<EngineLine> completeLines =
                selectHighestCompleteDepth(linesByDepth, requestedVariants);
        if (!completeLines.isEmpty()) return completeLines;

        int largestCompletedVariantCount = 0;
        for (Map<Integer, EngineLine> depthLines : linesByDepth.values()) {
            largestCompletedVariantCount = Math.max(
                    largestCompletedVariantCount,
                    countContiguousVariants(depthLines, requestedVariants));
        }
        if (largestCompletedVariantCount == 0) return List.of();
        return selectHighestCompleteDepth(
                linesByDepth,
                largestCompletedVariantCount);
    }

    private List<EngineLine> selectHighestCompleteDepth(
            TreeMap<Integer, Map<Integer, EngineLine>> linesByDepth,
            int expectedVariants) {
        for (Map.Entry<Integer, Map<Integer, EngineLine>> depthEntry
                : linesByDepth.descendingMap().entrySet()) {
            Map<Integer, EngineLine> depthLines = depthEntry.getValue();
            if (countContiguousVariants(depthLines, expectedVariants) < expectedVariants) continue;

            List<EngineLine> result = new ArrayList<>();
            for (int multiPv = 1; multiPv <= expectedVariants; multiPv++) {
                result.add(depthLines.get(multiPv));
            }
            return List.copyOf(result);
        }
        return List.of();
    }

    private int countContiguousVariants(
            Map<Integer, EngineLine> depthLines,
            int maxVariants) {
        int count = 0;
        for (int multiPv = 1; multiPv <= maxVariants; multiPv++) {
            if (!depthLines.containsKey(multiPv)) break;
            count++;
        }
        return count;
    }
}
