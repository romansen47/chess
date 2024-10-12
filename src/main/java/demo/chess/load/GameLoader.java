package demo.chess.load;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;

public class GameLoader {

	public void loadGame(String location, Game game) throws IOException, NoMoveFoundException {
		FileReader fileReader = new FileReader(location);
		BufferedReader bufferedReader = new BufferedReader(fileReader);

		String s = bufferedReader.readLine();
		while (s != null && !s.isBlank()) {
			Move finalMove = null;
	    	List<Move> moves = game.getPlayer().getValidMoves(game);
	    	for (Move move: moves) {
	    		if (move.toString().equals(s)) {
	    			finalMove = move;
	    			continue;
	    		}
	    	}
	    	game.apply(finalMove);
			s = bufferedReader.readLine();
	    }
		bufferedReader.close();
		fileReader.close();
	}

	public List<String> loadMoveList(String location) throws IOException{
		List<String> moveList = new ArrayList<>();
		FileReader fileReader = new FileReader(location);
		BufferedReader bufferedReader = new BufferedReader(fileReader);

		String s = bufferedReader.readLine();
		while (s != null && !s.isBlank()) {
	    	moveList.add(s);
			s = bufferedReader.readLine();
	    }
		bufferedReader.close();
		fileReader.close();
		return moveList;
	}
}
