package demo.chess.definitions.engines.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import demo.chess.definitions.Color;
import demo.chess.definitions.engines.EngineConfig;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.engines.UciPositionKey;
import demo.chess.definitions.engines.uci.UciInfoLine;
import demo.chess.definitions.engines.uci.UciInfoParser;
import demo.chess.game.Game;

public class EvaluationUciEngine extends AbstractUciAnalysisEngine {

    private Thread evaluationThread;
    private volatile BiConsumer<UciPositionKey, List<EngineLine>> evaluationUpdateListener;
    private volatile long evaluationGeneration;
    private int lastNotifiedDepth = -1;

    public EvaluationUciEngine(String path) throws Exception {
        super(path);
        logger.info("Creating new evaluation engine: {}", path);
    }

    public void setEvaluationUpdateListener(BiConsumer<UciPositionKey, List<EngineLine>> listener) {
        evaluationUpdateListener = listener;
    }

    @Override
    public synchronized List<EngineLine> getBestLines(Game chessGame, EngineConfig config)
            throws IOException, InterruptedException, ExecutionException {
        if (chessGame.getState() != null) return List.of();

        UciPositionKey key = positionKey(chessGame);
        List<EngineLine> cachedLines = getCachedLines(key);
        if (cachedLines != null) return cachedLines;

        cacheEmptyLines(key);
        startEvaluationEngine(chessGame, key, config);
        List<EngineLine> startedLines = getCachedLines(key);
        return startedLines != null ? startedLines : List.of();
    }

    public synchronized void startEvaluationEngine(
            Game chessGame,
            UciPositionKey positionKey,
            EngineConfig config) throws IOException {
        if (evaluationThread != null) stopEvaluation();
        if (chessGame.getState() != null) {
            logger.info("Game is decided. Not starting new infinite analysis...");
            return;
        }

        int moveCount = chessGame.getMoveList().size();
        logger.info(
                "{} is starting new infinite analysis for position {}",
                this,
                positionKey);

        long generation = ++evaluationGeneration;
        lastNotifiedDepth = -1;
        if (evaluationThread != null && !evaluationThread.isInterrupted()) {
            evaluationThread.interrupt();
        }

        evaluationThread = new Thread(() -> {
            try {
                restartProcess();
                applyConfig(config);
                prepareForGame(chessGame);
                final java.io.PrintWriter processWriter = writer;
                final java.io.BufferedReader processReader = reader;

                String evaluationCommand =
                        "stop\n" + UciPositionCommand.build(chessGame) + "\ngo infinite";
                logger.info(
                        "Starting new infinite analysis with {} threads",
                        config.getIntOption("Threads", 0));
                processWriter.println(evaluationCommand);
                processWriter.flush();

                List<UciInfoLine> bestLines = new ArrayList<>();
                int currentMaxDepth = 0;
                int requestedVariants = Math.max(1, config.getIntOption("MultiPV", 1));

                String line;
                while ((line = processReader.readLine()) != null) {
                    if (generation != evaluationGeneration || chessGame.getState() != null) return;

                    var parsed = UciInfoParser.parsePrincipalVariation(line);
                    if (parsed.isEmpty()) continue;

                    UciInfoLine info = parsed.get();
                    int depth = info.depth();
                    if (depth < currentMaxDepth) continue;

                    currentMaxDepth = depth;
                    bestLines.add(info);
                    if (requestedVariants == 1 || bestLines.size() >= requestedVariants) {
                        Color color = moveCount % 2 == 0 ? Color.WHITE : Color.BLACK;
                        List<EngineLine> newLines =
                                parseBestLines(color, bestLines, config);
                        if (!newLines.isEmpty()) {
                            cacheLines(positionKey, newLines);
                            notifyEvaluationUpdate(
                                    generation,
                                    positionKey,
                                    newLines);
                            bestLines.clear();
                        }
                    }
                }
                processReader.close();
            } catch (Exception e) {
                logger.debug("Evaluation reader stopped: {}", e.getMessage());
            }
        }, "uci-live-evaluation");

        evaluationThread.start();
    }

    private void notifyEvaluationUpdate(
            long generation,
            UciPositionKey positionKey,
            List<EngineLine> lines) {
        if (generation != evaluationGeneration || lines == null || lines.isEmpty()) return;

        BiConsumer<UciPositionKey, List<EngineLine>> listener;
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
            logger.debug(
                    "Evaluation update listener failed at depth {}: {}",
                    depth,
                    e.getMessage());
        }
    }

    @Override
    public void stopEvaluation() {
        logger.info("{} stopping actual infinite analysis", this);
        evaluationGeneration++;
        Thread activeThread = evaluationThread;
        if (activeThread != null && activeThread.isAlive()) {
            if (getWriter() != null) {
                getWriter().println("stop");
                getWriter().flush();
            }
            activeThread.interrupt();
        }
    }
}
