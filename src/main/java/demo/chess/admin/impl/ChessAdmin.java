package demo.chess.admin.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import demo.chess.admin.Admin;
import demo.chess.definitions.board.impl.ChessBoard;
import demo.chess.definitions.engines.Engine;
import demo.chess.definitions.engines.EvaluationEngine;
import demo.chess.definitions.engines.impl.EvaluationUciEngine;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.moves.impl.MoveListImpl;
import demo.chess.definitions.players.impl.BlackPlayerImpl;
import demo.chess.definitions.players.impl.WhitePlayerImpl;
import demo.chess.game.Game;
import demo.chess.game.impl.ChessGame;
import demo.chess.game.impl.Simulation;

/**
 * Configuration class providing the chess game bean and implementing the Admin
 * interface.
 */
@Configuration
public class ChessAdmin implements Admin {

	private static final Logger logger = LogManager.getLogger(ChessAdmin.class);

	/**
	 * Returns the chess game instance managed by the admin.
	 * <p>
	 * This method is marked as a Spring bean, so it will be managed by the Spring
	 * container.
	 * </p>
	 *
	 * @return a new instance of {@link ChessGame}
	 * @throws Exception
	 */
	@Override
	@Bean
	@Scope("prototype")
	public Game chessGame(int time) throws Exception {
		MoveList moveList = new MoveListImpl();
		return new ChessGame(new ChessBoard(), new WhitePlayerImpl(moveList, "ChessGame"),
				new BlackPlayerImpl(moveList, "ChessGame"), moveList, this, time);
	}

	@Override
	@Bean
	@Scope("prototype")
	public Game simulation() {
		MoveList moveList = new MoveListImpl();
		return new Simulation(new ChessBoard(), new WhitePlayerImpl(moveList, "Simulation"),
				new BlackPlayerImpl(moveList, "Simulation"), moveList);
	}

	@Bean
	@Override
	public Map<String, EvaluationEngine> evaluationEngines() {
		Map<String, EvaluationEngine> engines = new HashMap<>();
		for (Engine engine : Engine.values()) {
			try {
				engines.put(engine.toString(), new EvaluationUciEngine("/usr/games/" + engine.path()) {
					@Override
					public String toString() {
						return engine.toString();
					}
				});
			} catch (Exception e) {
				logger.info("Failed to create player engine {}", engine);
			}
		}
		return engines;
	}
}
