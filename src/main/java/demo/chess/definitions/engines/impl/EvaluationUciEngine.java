package demo.chess.definitions.engines.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import demo.chess.definitions.Color;
import demo.chess.definitions.engines.EngineConfig;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.engines.EvaluationEngine;
import demo.chess.game.Game;

public class EvaluationUciEngine extends ConsoleUciEngine implements EvaluationEngine {

    private static final int MIN_LIVE_EVALUATION_DEPTH = 0;

    String bestMove;
    private Map<String, List<EngineLine>> cachedBestLines = new HashMap<>();
    private String lastPositionHash = "";
    private Thread evaluationThread;
    private volatile BiConsumer<String, List<EngineLine>> evaluationUpdateListener;
    private volatile long evaluationGeneration;
    private int lastNotifiedDepth = -1;

    public EvaluationUciEngine(String path) throws Exception {
        super(path);
        logger.info("Creating new evaluation engine: {}", path);
    }

    public void setEvaluationUpdateListener(BiConsumer<String, List<EngineLine>> listener) {
        evaluationUpdateListener = listener;
    }

    @Override
    public void clearChachedLines() {
        getCachedBestLines().clear();
    }

    @Override
    public synchronized List<EngineLine> getBestLines(Game chessGame, EngineConfig config)
            throws IOException, InterruptedException, ExecutionException {
        if (chessGame.getState() != null) return new ArrayList<>();
        String movelist = chessGame.getMoveList().toString();
        List<EngineLine> cachedLines = getCachedBestLines().get(movelist);
        if (cachedLines != null) return cachedLines;
        getCachedBestLines().put(movelist, new ArrayList<>());
        startEvaluationEngine(chessGame, movelist, config);
        return getCachedBestLines().get(movelist);
    }

    @Override
    protected StringBuilder getCommandLineOptions(StringBuilder command, EngineConfig config) {
        return new StringBuilder(UciPositionCommand.build(command)).append("\ngo infinite ");
    }

    protected boolean isPositionNew(Game chessGame) {
        String currentPositionHash = chessGame.getMoveList().toString();
        if (!currentPositionHash.equals(lastPositionHash)) {
            lastPositionHash = currentPositionHash;
            return true;
        }
        return false;
    }

    protected List<EngineLine> parseBestLines(Color color, List<String> bestLines, EngineConfig config) {
        return parseBestLines(color, bestLines, config, Math.max(0, config.getDepth()));
    }

    protected List<EngineLine> parseBestLinesAtHighestDepth(
            Color color,
            List<String> bestLines,
            EngineConfig config) {
        int maxDepth = bestLines.stream()
                .filter(line -> line.contains("info") && line.contains("depth") && line.contains(" pv "))
                .mapToInt(line -> Integer.parseInt(line.split("depth ")[1].split(" ")[0]))
                .max()
                .orElse(0);
        if (maxDepth == 0) return new ArrayList<>();
        List<String> highestDepthLines = bestLines.stream()
                .filter(line -> line.contains("depth "))
                .filter(line -> Integer.parseInt(line.split("depth ")[1].split(" ")[0]) == maxDepth)
                .toList();
        return parseBestLines(color, highestDepthLines, config, 0);
    }

    private List<EngineLine> parseBestLines(
            Color color,
            List<String> bestLines,
            EngineConfig config,
            int minimumDepth) {
        int requestedVariants = Math.max(1, config.getIntOption("MultiPV", 1));
        TreeMap<Integer, Map<Integer, EngineLine>> linesByDepth = new TreeMap<>();

        for (String chessLine : bestLines) {
            if (!chessLine.contains("info") || !chessLine.contains("depth") || !chessLine.contains(" pv ")) continue;
            int currentDepth = Integer.parseInt(chessLine.split("depth ")[1].split(" ")[0]);
            if (currentDepth < minimumDepth) continue;
            int multipv = chessLine.contains("multipv ")
                    ? Integer.parseInt(chessLine.split("multipv ")[1].split(" ")[0])
                    : 1;
            if (multipv < 1 || multipv > requestedVariants) continue;

            Map<Integer, EngineLine> depthLines = linesByDepth.computeIfAbsent(currentDepth, ignored -> new TreeMap<>());
            double parsedValue;
            Integer mateDistance = null;
            if (chessLine.contains(" score mate ")) {
                int mateScore = Integer.parseInt(chessLine.split(" score mate ")[1].split(" ")[0]);
                if (mateScore == 0) {
                    depthLines.remove(multipv);
                    logger.debug("Ignoring transient UCI score mate 0 at depth {} multipv {}", currentDepth, multipv);
                    continue;
                }
                parsedValue = Integer.signum(mateScore) * 99d;
                mateDistance = Math.abs(mateScore);
            } else if (chessLine.contains(" score cp ")) {
                parsedValue = Double.parseDouble(chessLine.split(" score cp ")[1].split(" ")[0]) / 100.0;
            } else {
                continue;
            }

            String uciEngineLine = chessLine.split(" pv ", 2)[1];
            double factor = color.equals(Color.BLACK) ? -1 : 1;
            depthLines.put(multipv, new EngineLine(
                    factor * parsedValue,
                    currentDepth,
                    mateDistance,
                    uciEngineLine));
        }

        List<EngineLine> completeLines = selectHighestCompleteDepth(linesByDepth, requestedVariants);
        if (!completeLines.isEmpty()) return completeLines;

        int largestCompletedVariantCount = 0;
        for (Map<Integer, EngineLine> depthLines : linesByDepth.values()) {
            largestCompletedVariantCount = Math.max(
                    largestCompletedVariantCount,
                    countContiguousVariants(depthLines, requestedVariants));
        }
        if (largestCompletedVariantCount == 0) return new ArrayList<>();
        return selectHighestCompleteDepth(linesByDepth, largestCompletedVariantCount);
    }

    private List<EngineLine> selectHighestCompleteDepth(
            TreeMap<Integer, Map<Integer, EngineLine>> linesByDepth,
            int expectedVariants) {
        for (Map.Entry<Integer, Map<Integer, EngineLine>> depthEntry : linesByDepth.descendingMap().entrySet()) {
            Map<Integer, EngineLine> depthLines = depthEntry.getValue();
            if (countContiguousVariants(depthLines, expectedVariants) < expectedVariants) continue;
            List<EngineLine> result = new ArrayList<>();
            for (int multipv = 1; multipv <= expectedVariants; multipv++) result.add(depthLines.get(multipv));
            return result;
        }
        return new ArrayList<>();
    }

    private int countContiguousVariants(Map<Integer, EngineLine> depthLines, int maxVariants) {
        int count = 0;
        for (int multipv = 1; multipv <= maxVariants; multipv++) {
            if (!depthLines.containsKey(multipv)) break;
            count++;
        }
        return count;
    }

    public synchronized void startEvaluationEngine(Game chessGame, String moveListAsString, EngineConfig config)
            throws IOException {
        if (evaluationThread != null) stopEvaluation();
        if (chessGame.getState() != null) {
            logger.info("Game is decided. Not starting new infinite analysis...");
            return;
        }

        int moveCount = chessGame.getMoveList().size();
        logger.info("{} is starting new infinite analysis for move list {}", this, chessGame.getMoveList());
        long generation = ++evaluationGeneration;
        lastNotifiedDepth = -1;
        if (evaluationThread != null && !evaluationThread.isInterrupted()) evaluationThread.interrupt();

        evaluationThread = new Thread(() -> {
            try {
                restartProcess();
                applyConfig(config);
                prepareForGame(chessGame);
                final java.io.PrintWriter processWriter = writer;
                final java.io.BufferedReader processReader = reader;

                String evaluationCommand = "stop\n" + UciPositionCommand.build(chessGame) + "\ngo infinite";
                logger.info("Starting new infinite analysis with {} threads", config.getIntOption("Threads", 0));
                processWriter.println(evaluationCommand);
                processWriter.flush();

                List<String> bestLines = new ArrayList<>();
                int currentMaxDepth = MIN_LIVE_EVALUATION_DEPTH;
                String line;
                while ((line = processReader.readLine()) != null) {
                    if (generation != evaluationGeneration || chessGame.getState() != null) return;
                    if (line.contains("info") && line.contains("depth") && !(line.split(" ").length == 3)) {
                        int depth = Integer.parseInt(line.split("depth ")[1].split(" ")[0]);
                        if (depth >= currentMaxDepth) {
                            currentMaxDepth = depth;
                            bestLines.add(line);
                            if (config.getIntOption("MultiPV", 1) == 1
                                    || bestLines.stream().filter(l -> l.contains("multipv")).count()
                                            >= config.getIntOption("MultiPV", 1)) {
                                Color color = moveCount % 2 == 0 ? Color.WHITE : Color.BLACK;
                                List<EngineLine> newLines = parseBestLines(color, bestLines, config);
                                synchronized (getCachedBestLines()) {
                                    getCachedBestLines().put(moveListAsString, newLines);
                                }
                                notifyEvaluationUpdate(generation, moveListAsString, newLines);
                                bestLines.clear();
                            }
                        }
                    }
                }
                processReader.close();
            } catch (Exception e) {
                logger.debug("Evaluation reader stopped: {}", e.getMessage());
            }
        });

        try {
            evaluationThread.start();
        } catch (NullPointerException ignored) {
            logger.debug("Thread was cancelled...");
        }
    }

    private void notifyEvaluationUpdate(long generation, String positionKey, List<EngineLine> lines) {
        if (generation != evaluationGeneration || lines == null || lines.isEmpty()) return;
        BiConsumer<String, List<EngineLine>> listener;
        int depth = lines.get(0).getDepth();
        synchronized (this) {
            if (generation != evaluationGeneration || depth <= lastNotifiedDepth) return;
            lastNotifiedDepth = depth;
            listener = evaluationUpdateListener;
        }
        if (listener == null) return;
        try {
            listener.accept(positionKey, List.copyOf(lines));
        } catch (RuntimeException e) {
            logger.debug("Evaluation update listener failed at depth {}: {}", depth, e.getMessage());
        }
    }

    @Override
    public void stopEvaluation() {
        logger.info("{} stopping actual infinite analysis", this);
        evaluationGeneration++;
        if (evaluationThread != null && evaluationThread.isAlive()) {
            getWriter().println("stop");
            getWriter().flush();
            evaluationThread.interrupt();
        }
    }

    protected List<EngineLine> sortLinesByColor(Color color, List<EngineLine> moves) {
        List<EngineLine> tmpLines = new ArrayList<>(moves);
        if (color.equals(Color.WHITE)) {
            tmpLines.sort((line1, line2) -> Double.compare(line2.getEvaluation(), line1.getEvaluation()));
        } else {
            tmpLines.sort((line1, line2) -> Double.compare(line1.getEvaluation(), line2.getEvaluation()));
        }
        return tmpLines;
    }

    @Override
    public Map<String, List<EngineLine>> getCachedBestLines() {
        return cachedBestLines;
    }
}
