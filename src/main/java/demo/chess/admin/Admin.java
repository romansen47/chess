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

	/**
	 * Creates an instance of a simulation in order to be used for forecasts for move validation.
	 */
	Game simulation();
}
