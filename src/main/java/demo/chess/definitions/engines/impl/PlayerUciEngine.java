package demo.chess.definitions.engines.impl;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import demo.chess.definitions.engines.EngineConfig;
import demo.chess.definitions.engines.PlayerEngine;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;
import demo.chess.game.Game;

public class PlayerUciEngine extends ConsoleUciEngine implements PlayerEngine {

    /**
     * Creates a new PlayerUciEngine instance.
     * @param path the path
     */
    public PlayerUciEngine(String path) throws Exception {
        super(path);
        logger.info("Creating new player engine from path {}", path);
    }

    /**
     * Returns the best move.
     * @param chessGame the chess game
     * @param config the config
     * @return the best move
     */
    @Override
    public Move getBestMove(Game chessGame, EngineConfig config)
            throws NoMoveFoundException, IOException, InterruptedException {

        logger.debug("{} computing next move for movelist {}", this, chessGame.getMoveList());
        applyConfig(config);

        StringBuilder command = new StringBuilder("");
        MoveList moveList = chessGame.getMoveList();
        if (moveList.isEmpty()) {
            command.append(" []");
        } else {
            for (Move move : moveList) {
                command.append(move.toString()).append(" ");
            }
        }

        long whiteTimeMillis = Math.max(0L, chessGame.getTimeForEachPlayer() * 1000L
                - chessGame.getWhitePlayer().getChessClock().getTime(TimeUnit.MILLISECONDS));
        long blackTimeMillis = Math.max(0L, chessGame.getTimeForEachPlayer() * 1000L
                - chessGame.getBlackPlayer().getChessClock().getTime(TimeUnit.MILLISECONDS));
        long whiteIncrementMillis = Math.max(0L, chessGame.getIncrementForWhite() * 1000L);
        long blackIncrementMillis = Math.max(0L, chessGame.getIncrementForBlack() * 1000L);

        StringBuilder positionCommand = getPlayerCommandLineOptions(
                command,
                config,
                whiteTimeMillis,
                blackTimeMillis,
                whiteIncrementMillis,
                blackIncrementMillis);
        logger.debug("calling command: \n{}", positionCommand);
        writer.println(positionCommand.toString());
        writer.flush();

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("bestmove")) {
                String bestMoveString = line.split(" ")[1];

                for (Move move : chessGame.getPlayer().getValidMoves(chessGame)) {
                    if (move.toString().equals(bestMoveString)) {
                        return move;
                    }
                }
                logger.info("No move {} found, frontend and backend might be out of sync due to user interaction...", bestMoveString);
                logger.info("movelist {}", chessGame.getMoveList());
                logger.info("valid moves: {}", chessGame.getPlayer().getValidMoves(chessGame));
            }
        }
        throw new NoMoveFoundException("No valid move found");
    }

    /**
     * Returns the command line options.
     * @param command the command
     * @param config the config
     * @return the command line options
     */
    @Override
    protected StringBuilder getCommandLineOptions(StringBuilder command, EngineConfig config) {
        return getPlayerCommandLineOptions(command, config, 0L, 0L, 0L, 0L);
    }

    /**
     * Returns the player command line options.
     * @param command the command
     * @param config the config
     * @param whiteTimeMillis the white time millis
     * @param blackTimeMillis the black time millis
     * @param whiteIncrementMillis the white increment millis
     * @param blackIncrementMillis the black increment millis
     * @return the player command line options
     */
    private StringBuilder getPlayerCommandLineOptions(
            StringBuilder command,
            EngineConfig config,
            long whiteTimeMillis,
            long blackTimeMillis,
            long whiteIncrementMillis,
            long blackIncrementMillis) {
        StringBuilder positionCommand = new StringBuilder();
        positionCommand.append("position startpos moves ").append(command.toString()).append("\n");
        if (config.getDepth() > 0) {
            positionCommand.append("go depth ").append(config.getDepth());
        } else if (config.getMoveTimeSeconds() > 0) {
            positionCommand.append("go movetime ").append(config.getMoveTimeSeconds() * 1000L);
        } else {
            positionCommand.append("go")
                    .append(" wtime ").append(Math.max(0L, whiteTimeMillis))
                    .append(" btime ").append(Math.max(0L, blackTimeMillis))
                    .append(" winc ").append(Math.max(0L, whiteIncrementMillis))
                    .append(" binc ").append(Math.max(0L, blackIncrementMillis));
        }
        return positionCommand;
    }

    /**
     * Stops the evaluation.
     */
    @Override
    public void stopEvaluation() {
        logger.info("{} stopping actual player evaluation", this);
        writer.println("stop");
        writer.flush();
    }
}
