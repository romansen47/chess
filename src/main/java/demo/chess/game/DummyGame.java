package demo.chess.game;

import java.io.IOException;

import demo.chess.definitions.engines.impl.NoMoveFoundException;

/**
 * Marker interface for lightweight trusted-game replays.
 *
 * <p>Dummy games intentionally use dummy players whose move simulation step is
 * disabled. They are appropriate when a move sequence has already been accepted
 * elsewhere and only needs to be replayed cheaply, for example during bulk PGN
 * processing, database reconstruction or engine-line display.</p>
 *
 * <p>Callers must not interpret {@code getValidMoves()} on a dummy player as a
 * proof of full chess legality. In particular king safety and castling-through-
 * check validation are skipped. Code that needs a rule-dependent conclusion
 * must explicitly use a normal simulation for that conclusion.</p>
 */
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
