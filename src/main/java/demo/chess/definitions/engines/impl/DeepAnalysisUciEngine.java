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
import demo.chess.game.Game;

public class DeepAnalysisUciEngine extends EvaluationUciEngine implements DeepAnalysisEngine {

    public DeepAnalysisUciEngine(String path) throws Exception {
        super(path);
    }

    @Override
    public synchronized List<EngineLine> getBestLines(Game chessGame, EngineConfig config)
            throws IOException, InterruptedException, ExecutionException {
        String moveListAsString = chessGame.getMoveList().toString();
        List<EngineLine> cachedLines = getCachedBestLines().get(moveListAsString);
        if (cachedLines != null) return cachedLines;
        return analyze(chessGame, config).getFinalLines();
    }

    @Override
    public synchronized DeepAnalysisResult analyze(Game chessGame, EngineConfig config)
            throws IOException, InterruptedException, ExecutionException {
        applyConfig(config);
        prepareForGame(chessGame);

        List<String> rawInfoLines = new ArrayList<>();
        String command = buildDeepAnalysisCommand(chessGame, config);
        logger.info("{} is starting finite deep analysis for move list {}", this, chessGame.getMoveList());
        getWriter().println(command);
        getWriter().flush();

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("info ") && line.contains(" depth ") && line.contains(" pv ")) {
                rawInfoLines.add(line);
            }
            if (line.startsWith("bestmove")) break;
        }

        Color sideToMove = chessGame.getPlayer() != null
                ? chessGame.getPlayer().getColor()
                : (chessGame.getMoveList().size() % 2 == 0 ? Color.WHITE : Color.BLACK);
        Map<Integer, List<EngineLine>> depthHistory = buildDepthHistory(sideToMove, rawInfoLines, config);
        List<EngineLine> finalLines = parseBestLinesAtHighestDepth(sideToMove, rawInfoLines, config);
        getCachedBestLines().put(chessGame.getMoveList().toString(), finalLines);
        return new DeepAnalysisResult(finalLines, depthHistory);
    }

    private Map<Integer, List<EngineLine>> buildDepthHistory(
            Color color,
            List<String> rawInfoLines,
            EngineConfig config) {
        TreeMap<Integer, List<String>> rawByDepth = new TreeMap<>();
        for (String rawLine : rawInfoLines) {
            if (!rawLine.contains(" depth ") || !rawLine.contains(" pv ")) continue;
            int depth;
            try {
                depth = Integer.parseInt(rawLine.split("depth ")[1].split(" ")[0]);
            } catch (RuntimeException ignored) {
                continue;
            }
            rawByDepth.computeIfAbsent(depth, ignored -> new ArrayList<>()).add(rawLine);
        }

        Map<Integer, List<EngineLine>> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<String>> entry : rawByDepth.entrySet()) {
            List<EngineLine> parsed = parseBestLinesAtHighestDepth(color, entry.getValue(), config);
            if (!parsed.isEmpty()) result.put(entry.getKey(), List.copyOf(parsed));
        }
        return Map.copyOf(result);
    }

    private String buildDeepAnalysisCommand(Game game, EngineConfig config) {
        StringBuilder command = new StringBuilder();
        command.append("ucinewgame\n");
        command.append(UciPositionCommand.build(game)).append('\n');
        if (config.getDepth() > 0) {
            command.append("go depth ").append(config.getDepth()).append('\n');
        } else {
            int moveTimeMillis = Math.max(1, config.getMoveTimeSeconds()) * 1000;
            command.append("go movetime ").append(moveTimeMillis).append('\n');
        }
        return command.toString();
    }

    @Override
    protected StringBuilder getCommandLineOptions(StringBuilder command, EngineConfig config) {
        StringBuilder result = new StringBuilder(UciPositionCommand.build(command)).append('\n');
        if (config.getDepth() > 0) result.append("go depth ").append(config.getDepth()).append('\n');
        else result.append("go movetime ").append(Math.max(1, config.getMoveTimeSeconds()) * 1000).append('\n');
        return result;
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
