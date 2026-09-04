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

	/**
	 * Creates a new DummyChessGame instance.
	 * @param chessBoard the chess board
	 * @param whitePlayer the white player
	 * @param blackPlayer the black player
	 * @param moveList the move list
	 */
	public DummyChessGame(Board chessBoard, DummyWhitePlayer whitePlayer, DummyBlackPlayer blackPlayer, MoveList moveList){
		super(chessBoard, whitePlayer, blackPlayer, moveList);
	}

	/**
	 * Checks the for game end.
	 * @return the result of the operation
	 */
	@Override
	public boolean checkForGameEnd() throws NoMoveFoundException, IOException {
		return false;
	}

	/**
	 * Checks the for50 moves rule.
	 * @return the result of the operation
	 */
	@Override
	public boolean checkFor50MovesRule() {
		return false;
	}

	/**
	 * Checks the for threefold repetition.
	 * @param movesBeforeRule the moves before rule
	 * @return the result of the operation
	 */
	@Override
	public boolean checkForThreefoldRepetition(int movesBeforeRule) {
		return false;
	}

	/**
	 * Sets the state.
	 * @param state the state
	 */
	@Override
	public void setState(State state) {
	}

	/**
	 * Returns the state.
	 * @return the state
	 */
	@Override
	public State getState() {
		return null;
	}

}
