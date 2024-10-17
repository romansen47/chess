package demo.chess.definitions.engines.impl;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import demo.chess.definitions.Color;
import demo.chess.definitions.engines.PlayerEngine;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.players.Player;
import demo.chess.game.Game;

public class PlayerUciEngine extends ConsoleUciEngine implements PlayerEngine {

	private final String name;

	public PlayerUciEngine(String path, String name) throws Exception {
		super(path);
		this.name = name;
	}

	@Override
    public Move getBestMove(Game chessGame) throws NoMoveFoundException, IOException, InterruptedException {
        // Erstelle das Kommando, um UciEngine die aktuelle Position mitzuteilen
        StringBuilder command = new StringBuilder(" ");
        MoveList moveList = chessGame.getMoveList();
        for (Move move : moveList) {
            command.append(move.toString()).append(" ");
        }

        Color color = moveList.size() % 2 == 0 ? Color.WHITE : Color.BLACK;
        
        // Sende den Befehl an UciEngine, um den besten Zug zu berechnen
        String postfixIncrement = "";
        long whiteTime = chessGame.getTimeForEachPlayer() * 1000l - chessGame.getWhitePlayer().getChessClock().getTime(TimeUnit.MILLISECONDS);
		long blackTime = chessGame.getTimeForEachPlayer() * 1000l - chessGame.getBlackPlayer().getChessClock().getTime(TimeUnit.MILLISECONDS);

        if (color.equals(Color.WHITE)){
        	postfixIncrement = "wtime " + whiteTime * 1000;
        	if (chessGame.getIncrementForWhite() > 0) {
        		postfixIncrement += " winc " + chessGame.getIncrementForWhite();
        	}
        }
        if (color.equals(Color.BLACK)){
        	postfixIncrement = "btime " + blackTime * 1000;
        	if (chessGame.getIncrementForBlack() > 0) {
        		postfixIncrement += " binc " + chessGame.getIncrementForBlack();
        	}
        }
        
        StringBuilder positionCommand = getCommandLineOptions(command).append(postfixIncrement);
        writer.println(positionCommand.toString());
        writer.flush();

        // Warte auf die Antwort von UciEngine
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("bestmove")) {
                String bestMoveString = line.split(" ")[1];
                // Finde den passenden Move und gib ihn zurück
                for (Move move : chessGame.getPlayer().getValidMoves(chessGame)) {
                    if (move.toString().equals(bestMoveString)) {
                        return move;
                    }
                }
                throw new NoMoveFoundException("Move " + bestMoveString + " not found...");
            }
        }
        Thread.sleep(200);
        throw new NoMoveFoundException("No valid move found");
    }

	@Override
	protected StringBuilder getCommandLineOptions(StringBuilder command) {
		StringBuilder positionCommand = new StringBuilder();
		if (this.getThreads() > 0) {
			positionCommand.append("setoption name Threads value ").append(this.getThreads()).append("\n");
		}
		if (this.getHashSize() > 0) {
			positionCommand.append("setoption name Hash value ").append(this.getHashSize()).append("\n");
		}
		if (this.getContempt() > 0) {
			positionCommand.append("setoption name Contempt value ").append(this.getContempt()).append("\n");
		}
		if (this.getUciElo() > 0) {
			positionCommand.append("setoption name UCI_LimitStrength value true\nsetoption name UCI_Elo value ")
					.append(this.getUciElo()).append("\n");
		}
		positionCommand.append("position startpos moves ").append(command.toString()).append("\n");
		if (this.getDepth() > 0) {
			positionCommand.append("\ngo depth ").append(this.getDepth());
		} else {
			positionCommand.append("\ngo movetime ").append(this.getMoveOverhead() * 1000);
		}
		return positionCommand;
	}

}
