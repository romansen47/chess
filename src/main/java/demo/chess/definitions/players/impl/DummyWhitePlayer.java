package demo.chess.definitions.players.impl;

import java.io.IOException;

import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;
import demo.chess.game.Game;

public class DummyWhitePlayer extends WhitePlayerImpl{

	public DummyWhitePlayer(MoveList moveList) {
		super(moveList, "dummy white player");
	}
	
	@Override
	protected boolean simulate(Game chessGame, Move move) throws NoMoveFoundException, IOException {
		return true;
	}

}
