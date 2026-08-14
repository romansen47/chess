package demo.chess.definitions.engines.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import demo.chess.definitions.Color;
import demo.chess.definitions.engines.DeepAnalysisEngine;
import demo.chess.definitions.engines.EngineConfig;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.moves.Move;
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
        if (cachedLines != null) {
            return cachedLines;
        }

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
        List<EngineLine> parsedLines = parseBestLinesAtHighestDepth(sideToMove, rawInfoLines, config);
        getCachedBestLines().put(moveListAsString, parsedLines);
        return parsedLines;
    }

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
            int moveTimeMillis = Math.max(100, config.getMoveTimeSeconds() * 1000);
            command.append("go movetime ").append(moveTimeMillis).append('\n');
        }

        return command.toString();
    }

    @Override
    protected StringBuilder getCommandLineOptions(StringBuilder command, EngineConfig config) {
        return new StringBuilder(buildDeepAnalysisCommand(List.of(), config));
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
