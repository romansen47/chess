package demo.chess.admin;

import demo.chess.game.Game;

/**
 * Interface representing administrative functionalities for a chess game.
 */
public interface Admin {

	/**
	 * Returns the chess game instance managed by the admin.
	 *
	 * @return the chess game instance
	 */
	Game chessGame(int time);
}
