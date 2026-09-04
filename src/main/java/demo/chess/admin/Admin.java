package demo.chess.admin;

import demo.chess.game.Game;

/**
 * Interface representing administrative functionalities for a chess game.
 */
public interface Admin {

	/**
	 * Performs the chess game operation.
	 * @param time the time
	 * @return the result of the operation
	 */
	Game chessGame(int time);
}
