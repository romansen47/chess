package demo.chess.admin;

import java.util.List;
import java.util.Map;

import demo.chess.definitions.engines.Engine;
import demo.chess.definitions.engines.EvaluationEngine;
import demo.chess.definitions.engines.PlayerEngine;
import demo.chess.game.Game;

/**
 * Interface representing administrative functionalities for a chess game.
 */
public interface Admin {

	/**
	 * Returns the chess game instance managed by the admin.
	 * @return the chess game instance
	 * @throws Exception
	 */
	Game chessGame(int time) throws Exception;

	Map<Engine, EvaluationEngine> evaluationEngines() throws Exception;

	Game simulation();
}
