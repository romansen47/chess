package demo.chess.definitions.engines.impl;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import demo.chess.definitions.engines.EngineConfig;
import demo.chess.definitions.engines.PlayerEngine;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;
import demo.chess.notation.UciMoveCodec;

public class PlayerUciEngine extends ConsoleUciEngine implements PlayerEngine {

    public PlayerUciEngine(String path) throws Exception {
        super(path);
        logger.info("Creating new player engine from path {}", path);
    }

    @Override
    public Move getBestMove(Game chessGame, EngineConfig config)
            throws NoMoveFoundException, IOException, InterruptedException {
        logger.debug("{} computing next move for movelist {}", this, chessGame.getMoveList());
        applyConfig(config);
        prepareForGame(chessGame);

        long whiteTimeMillis = Math.max(0L, chessGame.getTimeForEachPlayer() * 1000L
                - chessGame.getWhitePlayer().getChessClock().getTime(TimeUnit.MILLISECONDS));
        long blackTimeMillis = Math.max(0L, chessGame.getTimeForEachPlayer() * 1000L
                - chessGame.getBlackPlayer().getChessClock().getTime(TimeUnit.MILLISECONDS));
        long whiteIncrementMillis = Math.max(0L, chessGame.getIncrementForWhite() * 1000L);
        long blackIncrementMillis = Math.max(0L, chessGame.getIncrementForBlack() * 1000L);

        StringBuilder command = new StringBuilder(UciPositionCommand.build(chessGame)).append('\n');
        appendGoCommand(command, config, whiteTimeMillis, blackTimeMillis,
                whiteIncrementMillis, blackIncrementMillis);
        logger.debug("calling command: \n{}", command);
        writer.println(command.toString());
        writer.flush();

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("bestmove")) {
                String[] fields = line.trim().split("\\s+");
                if (fields.length < 2) continue;
                String bestMoveString = fields[1];
                for (Move move : chessGame.getPlayer().getValidMoves(chessGame)) {
                    if (UciMoveCodec.matches(chessGame, move, bestMoveString)) return move;
                }
                logger.info("No move {} found; frontend/backend may be out of sync", bestMoveString);
            }
        }
        throw new NoMoveFoundException("No valid move found");
    }

    @Override
    protected StringBuilder getCommandLineOptions(StringBuilder command, EngineConfig config) {
        StringBuilder result = new StringBuilder(UciPositionCommand.build(command)).append('\n');
        appendGoCommand(result, config, 0L, 0L, 0L, 0L);
        return result;
    }

    private void appendGoCommand(
            StringBuilder command,
            EngineConfig config,
            long whiteTimeMillis,
            long blackTimeMillis,
            long whiteIncrementMillis,
            long blackIncrementMillis) {
        if (config.getDepth() > 0) {
            command.append("go depth ").append(config.getDepth());
        } else if (config.getMoveTimeSeconds() > 0) {
            command.append("go movetime ").append(config.getMoveTimeSeconds() * 1000L);
        } else {
            command.append("go")
                    .append(" wtime ").append(Math.max(0L, whiteTimeMillis))
                    .append(" btime ").append(Math.max(0L, blackTimeMillis))
                    .append(" winc ").append(Math.max(0L, whiteIncrementMillis))
                    .append(" binc ").append(Math.max(0L, blackIncrementMillis));
        }
    }

    @Override
    public void stopEvaluation() {
        logger.info("{} stopping actual player evaluation", this);
        writer.println("stop");
        writer.flush();
    }
}
