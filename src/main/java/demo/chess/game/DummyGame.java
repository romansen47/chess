package demo.chess.game;

import java.io.IOException;

import demo.chess.definitions.engines.impl.NoMoveFoundException;

public interface DummyGame extends Game {

	/**
	 * Checks the for game end.
	 * @return the result of the operation
	 */
	boolean checkForGameEnd() throws NoMoveFoundException, IOException;

	/**
	 * Checks the for50 moves rule.
	 * @return the result of the operation
	 */
	boolean checkFor50MovesRule();

	/**
	 * Checks the for threefold repetition.
	 * @param movesBeforeRule the moves before rule
	 * @return the result of the operation
	 */
	boolean checkForThreefoldRepetition(int movesBeforeRule);
}
