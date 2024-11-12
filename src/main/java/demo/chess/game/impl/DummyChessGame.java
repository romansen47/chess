package demo.chess.game.impl;

import java.io.IOException;

import demo.chess.admin.Admin;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.players.impl.DummyBlackPlayer;
import demo.chess.definitions.players.impl.DummyWhitePlayer;
import demo.chess.definitions.states.State;
import demo.chess.game.DummyGame;

public class DummyChessGame extends ChessGame implements DummyGame{

	public DummyChessGame(Board chessBoard, DummyWhitePlayer whitePlayer, DummyBlackPlayer blackPlayer, MoveList moveList,
			Admin chessAdmin){
		super(chessBoard, whitePlayer, blackPlayer, moveList, chessAdmin, 1000000);
	}

	@Override
	protected boolean checkForGameEnd() throws NoMoveFoundException, IOException {
		return false;
	}

	@Override
	protected boolean checkFor50MovesRule() {
		return false;
	}

	@Override
	protected boolean checkForThreefoldRepetition(int movesBeforeRule) {
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
