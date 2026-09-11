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
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;

public class DeepAnalysisUciEngine extends EvaluationUciEngine implements DeepAnalysisEngine {

    /**
     * Creates a new DeepAnalysisUciEngine instance.
     * @param path the path
     */
    public DeepAnalysisUciEngine(String path) throws Exception {
        super(path);
    }

    /**
     * Returns the best lines for compatibility with EvaluationEngine callers.
     * DeepAnalysis consumers should use analyze() to keep final lines and
     * intermediate history bound to one result object.
     */
    @Override
    public synchronized List<EngineLine> getBestLines(Game chessGame, EngineConfig config)
            throws IOException, InterruptedException, ExecutionException {
        String moveListAsString = chessGame.getMoveList().toString();
        List<EngineLine> cachedLines = getCachedBestLines().get(moveListAsString);
        if (cachedLines != null) {
            return cachedLines;
        }
        return analyze(chessGame, config).getFinalLines();
    }

    /**
     * Runs one finite DeepAnalysis search.
     * @param chessGame current position
     * @param config engine configuration
     * @return final lines and depth history from the same search
     */
    @Override
    public synchronized DeepAnalysisResult analyze(Game chessGame, EngineConfig config)
            throws IOException, InterruptedException, ExecutionException {
        applyConfig(config);

        List<Move> moveList = new ArrayList<>(chessGame.getMoveList());
        List<String> rawInfoLines = new ArrayList<>();
        String command = buildDeepAnalysisCommand(moveList, config);

        logger.info("{} is starting finite deep analysis for move list {}", this, moveList);
        getWriter().println(command);
        getWriter().flush();

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("info ") && line.contains(" depth ") && line.contains(" pv ")) {
                rawInfoLines.add(line);
            }

            if (line.startsWith("bestmove")) {
                break;
            }
        }

        Color sideToMove = moveList.size() % 2 == 0 ? Color.WHITE : Color.BLACK;
        Map<Integer, List<EngineLine>> depthHistory =
                buildDepthHistory(sideToMove, rawInfoLines, config);
        List<EngineLine> finalLines =
                parseBestLinesAtHighestDepth(sideToMove, rawInfoLines, config);

        getCachedBestLines().put(chessGame.getMoveList().toString(), finalLines);
        return new DeepAnalysisResult(finalLines, depthHistory);
    }

    /**
     * Builds one parsed snapshot for every depth for which the engine emitted
     * at least one usable principal variation. This preserves information that
     * EvaluationUciEngine normally discards after selecting the final depth.
     *
     * @param color side to move
     * @param rawInfoLines raw UCI info lines from this finite search
     * @param config engine configuration
     * @return immutable depth snapshots in ascending depth order
     */
    private Map<Integer, List<EngineLine>> buildDepthHistory(
            Color color,
            List<String> rawInfoLines,
            EngineConfig config) {
        TreeMap<Integer, List<String>> rawByDepth = new TreeMap<>();
        for (String rawLine : rawInfoLines) {
            if (!rawLine.contains(" depth ") || !rawLine.contains(" pv ")) {
                continue;
            }

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
            if (!parsed.isEmpty()) {
                result.put(entry.getKey(), List.copyOf(parsed));
            }
        }
        return Map.copyOf(result);
    }

    /**
     * Builds the deep analysis command.
     * @param moveList the move list
     * @param config the config
     * @return the result of the operation
     */
    private String buildDeepAnalysisCommand(List<Move> moveList, EngineConfig config) {
        StringBuilder command = new StringBuilder();
        command.append("ucinewgame\n");
        command.append("position startpos");
        if (!moveList.isEmpty()) {
            command.append(" moves");
            for (Move move : moveList) {
                command.append(' ').append(move.toString());
            }
        }
        command.append('\n');

        if (config.getDepth() > 0) {
            command.append("go depth ").append(config.getDepth()).append('\n');
        } else {
            int moveTimeMillis = Math.max(1, config.getMoveTimeSeconds()) * 1000;
            command.append("go movetime ").append(moveTimeMillis).append('\n');
        }

        return command.toString();
    }

    /**
     * Returns the command line options.
     * @param command the command
     * @param config the config
     * @return the command line options
     */
    @Override
    protected StringBuilder getCommandLineOptions(StringBuilder command, EngineConfig config) {
        return new StringBuilder(buildDeepAnalysisCommand(List.of(), config));
    }

    /**
     * Stops the evaluation.
     */
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
