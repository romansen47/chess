package demo.chess.definitions.engines.impl;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import demo.chess.definitions.Color;
import demo.chess.definitions.engines.EngineConfig;
import demo.chess.definitions.engines.PlayerEngine;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;
import demo.chess.game.Game;

public class PlayerUciEngine extends ConsoleUciEngine implements PlayerEngine {

	public PlayerUciEngine(String path) throws Exception {
		super(path);
		logger.info("Creating new player engine from path {}", path);
	}

	@Override
	public Move getBestMove(Game chessGame, EngineConfig config)
			throws NoMoveFoundException, IOException, InterruptedException {

		StringBuilder command = new StringBuilder("");
		MoveList moveList = chessGame.getMoveList();
		for (Move move : moveList) {
			command.append(move.toString()).append(" ");
		}

		Color color = moveList.size() % 2 == 0 ? Color.WHITE : Color.BLACK;

		String postfixIncrement = "";
		long whiteTime = chessGame.getTimeForEachPlayer() * 1000l
				- chessGame.getWhitePlayer().getChessClock().getTime(TimeUnit.MILLISECONDS);
		long blackTime = chessGame.getTimeForEachPlayer() * 1000l
				- chessGame.getBlackPlayer().getChessClock().getTime(TimeUnit.MILLISECONDS);

		if (color.equals(Color.WHITE)) {
			postfixIncrement = " wtime " + whiteTime;
			if (chessGame.getIncrementForWhite() > 0) {
				postfixIncrement += " winc " + chessGame.getIncrementForWhite();
			}
		}
		if (color.equals(Color.BLACK)) {
			postfixIncrement = " btime " + blackTime;
			if (chessGame.getIncrementForBlack() > 0) {
				postfixIncrement += " binc " + chessGame.getIncrementForBlack();
			}
		}

		StringBuilder positionCommand = getCommandLineOptions(command, config).append(postfixIncrement);
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
			}
		}
		Thread.sleep(200);
		throw new NoMoveFoundException("No valid move found");
	}

	@Override
	protected StringBuilder getCommandLineOptions(StringBuilder command, EngineConfig config) {
		StringBuilder positionCommand = new StringBuilder();
		if (config.getThreads() > 0) {
			positionCommand.append("setoption name Threads value ").append(config.getThreads()).append("\n");
		}
		if (config.getHashSize() > 0) {
			positionCommand.append("setoption name Hash value ").append(config.getHashSize()).append("\n");
		}
		if (config.getContempt() > 0) {
			positionCommand.append("setoption name Contempt value ").append(config.getContempt()).append("\n");
		}
		if (config.getUciElo() > 0) {
			positionCommand.append("setoption name UCI_LimitStrength value true\nsetoption name UCI_Elo value ")
					.append(config.getUciElo()).append("\n");
		}
		positionCommand.append("position startpos moves ").append(command.toString()).append("\n");
		if (config.getDepth() > 0) {
			positionCommand.append("\ngo depth ").append(config.getDepth());
		} else {
			positionCommand.append("\ngo movetime ").append(config.getMoveOverhead() * 1000);
		}
		return positionCommand;
	}

	@Override
	public void stopEvaluation() {
		logger.info("{} stopping actual player evaluation", this);
		writer.println("stop");
		writer.flush();
	}

}
