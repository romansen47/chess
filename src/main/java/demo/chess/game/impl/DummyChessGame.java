package demo.chess.game.impl;

import java.io.IOException;

import demo.chess.definitions.board.Board;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.players.impl.DummyBlackPlayer;
import demo.chess.definitions.players.impl.DummyWhitePlayer;
import demo.chess.definitions.states.State;
import demo.chess.game.DummyGame;

public class DummyChessGame extends Simulation implements DummyGame{

	public DummyChessGame(Board chessBoard, DummyWhitePlayer whitePlayer, DummyBlackPlayer blackPlayer, MoveList moveList){
		super(chessBoard, whitePlayer, blackPlayer, moveList);
	}

	@Override
	public boolean checkForGameEnd() throws NoMoveFoundException, IOException {
		return false;
	}

	@Override
	public boolean checkFor50MovesRule() {
		return false;
	}

	@Override
	public boolean checkForThreefoldRepetition(int movesBeforeRule) {
		return false;
	}

	@Override
	public void setState(State state) {
	}

	@Override
	public State getState() {
		return null;
	}

}
