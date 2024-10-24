package demo.chess.admin;

import java.util.Map;

import demo.chess.definitions.engines.EvaluationEngine;
import demo.chess.game.Game;

/**
 * Interface representing administrative functionalities for a chess game.
 */
public interface Admin {

	/**
	 * Returns the chess game instance managed by the admin.
	 *
	 * @return the chess game instance
	 * @throws Exception
	 */
	Game chessGame(int time) throws Exception;

	Map<String, EvaluationEngine> evaluationEngines() throws Exception;

	Game simulation();
}
