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

    /**
     * Führt eine endliche Analyse für genau eine Stellung aus.
     *
     * Anders als EvaluationUciEngine verwendet diese Engine kein dauerhaftes
     * "go infinite" mit Hintergrundthread, sondern wartet synchron auf das
     * UCI-"bestmove" nach der konfigurierten Zeit bzw. Tiefe. Damit eignet sie
     * sich für Post-Game-Analyse/Replay-Jobs, ohne mit der Live-Evaluation zu
     * konkurrieren.
     */
    @Override
    public synchronized List<EngineLine> getBestLines(Game chessGame, EngineConfig config)
            throws IOException, InterruptedException, ExecutionException {
        String moveListAsString = chessGame.getMoveList().toString();
        List<EngineLine> cachedLines = getCachedBestLines().get(moveListAsString);
        if (cachedLines != null) {
            return cachedLines;
        }

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
        appendOption(command, "MultiPV", Math.max(1, config.getMultiPV()));
        appendOption(command, "Threads", Math.max(1, config.getThreads()));
        appendOption(command, "Hash", Math.max(1, config.getHashSize()));

        if (config.getContempt() != 0) {
            appendOption(command, "Contempt", config.getContempt());
        }

        if (config.getUciElo() > 0) {
            appendOption(command, "UCI_LimitStrength", "true");
            appendOption(command, "UCI_Elo", config.getUciElo());
        }

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
            int moveTimeMillis = Math.max(100, config.getMoveOverhead() * 1000);
            command.append("go movetime ").append(moveTimeMillis).append('\n');
        }

        return command.toString();
    }

    private void appendOption(StringBuilder command, String name, int value) {
        appendOption(command, name, Integer.toString(value));
    }

    private void appendOption(StringBuilder command, String name, String value) {
        command.append("setoption name ")
                .append(name)
                .append(" value ")
                .append(value)
                .append('\n');
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
        }
    }
}
