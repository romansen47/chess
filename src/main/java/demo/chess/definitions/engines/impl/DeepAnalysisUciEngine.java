package demo.chess.definitions.engines.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import demo.chess.definitions.Color;
import demo.chess.definitions.engines.DeepAnalysisEngine;
import demo.chess.definitions.engines.DeepAnalysisResult;
import demo.chess.definitions.engines.EngineConfig;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.engines.UciPositionKey;
import demo.chess.definitions.engines.uci.UciInfoLine;
import demo.chess.definitions.engines.uci.UciInfoParser;
import demo.chess.game.Game;

public class DeepAnalysisUciEngine extends AbstractUciAnalysisEngine implements DeepAnalysisEngine {

    /*
     * Finite searches deliberately do not synchronize on the engine instance.
     * ConsoleUciEngine.close()/stopEvaluation() must be able to acquire that
     * monitor while analyze() is blocked in readLine(), so a game lifecycle
     * transition can interrupt a running search.
     */
    private final Object finiteSearchLock = new Object();

    public DeepAnalysisUciEngine(String path) throws Exception {
        super(path);
    }

    @Override
    public List<EngineLine> getBestLines(Game chessGame, EngineConfig config)
            throws IOException, InterruptedException, ExecutionException {
        synchronized (finiteSearchLock) {
            UciPositionKey key = positionKey(chessGame);
            List<EngineLine> cachedLines = getCachedLines(key);
            if (cachedLines != null) return cachedLines;
            return analyzeLocked(chessGame, key, config).getFinalLines();
        }
    }

    @Override
    public DeepAnalysisResult analyze(Game chessGame, EngineConfig config)
            throws IOException, InterruptedException, ExecutionException {
        synchronized (finiteSearchLock) {
            return analyzeLocked(
                    chessGame,
                    positionKey(chessGame),
                    config);
        }
    }

    private DeepAnalysisResult analyzeLocked(
            Game chessGame,
            UciPositionKey positionKey,
            EngineConfig config)
            throws IOException, InterruptedException, ExecutionException {
        applyConfig(config);
        prepareForGame(chessGame);

        List<UciInfoLine> infoLines = new ArrayList<>();
        String command = buildDeepAnalysisCommand(chessGame, config);
        logger.info(
                "{} is starting finite deep analysis for position {}",
                this,
                positionKey);
        getWriter().println(command);
        getWriter().flush();

        String line;
        while ((line = reader.readLine()) != null) {
            UciInfoParser.parsePrincipalVariation(line).ifPresent(infoLines::add);
            if (line.startsWith("bestmove")) break;
        }

        Color sideToMove = chessGame.getPlayer() != null
                ? chessGame.getPlayer().getColor()
                : (chessGame.getMoveList().size() % 2 == 0 ? Color.WHITE : Color.BLACK);

        Map<Integer, List<EngineLine>> depthHistory =
                buildDepthHistory(sideToMove, infoLines, config);
        List<EngineLine> finalLines =
                parseBestLinesAtHighestDepth(sideToMove, infoLines, config);
        cacheLines(positionKey, finalLines);
        return new DeepAnalysisResult(finalLines, depthHistory);
    }

    private Map<Integer, List<EngineLine>> buildDepthHistory(
            Color color,
            List<UciInfoLine> infoLines,
            EngineConfig config) {
        TreeMap<Integer, List<UciInfoLine>> byDepth = new TreeMap<>();
        for (UciInfoLine info : infoLines) {
            byDepth.computeIfAbsent(
                    info.depth(),
                    ignored -> new ArrayList<>()).add(info);
        }

        Map<Integer, List<EngineLine>> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<UciInfoLine>> entry : byDepth.entrySet()) {
            List<EngineLine> parsed =
                    parseBestLinesAtHighestDepth(
                            color,
                            entry.getValue(),
                            config);
            if (!parsed.isEmpty()) {
                result.put(entry.getKey(), List.copyOf(parsed));
            }
        }
        return Map.copyOf(result);
    }

    private String buildDeepAnalysisCommand(
            Game game,
            EngineConfig config) {
        StringBuilder command = new StringBuilder();
        command.append("ucinewgame\n");
        command.append(UciPositionCommand.build(game)).append('\n');
        if (config.getDepth() > 0) {
            command.append("go depth ").append(config.getDepth()).append('\n');
        } else {
            int moveTimeMillis =
                    Math.max(1, config.getMoveTimeSeconds()) * 1000;
            command.append("go movetime ").append(moveTimeMillis).append('\n');
        }
        return command.toString();
    }

    @Override
    public synchronized void stopEvaluation() {
        try {
            if (getWriter() != null) {
                getWriter().println("stop");
                getWriter().flush();
            }
        } catch (Exception e) {
            logger.debug("Could not stop deep analysis engine", e);
        } finally {
            super.close();
        }
    }
}
