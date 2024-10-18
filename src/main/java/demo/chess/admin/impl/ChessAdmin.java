package demo.chess.admin.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import demo.chess.definitions.engines.PlayerEngine;
import demo.chess.definitions.engines.impl.EvaluationUciEngine;
import demo.chess.definitions.engines.impl.PlayerUciEngine;
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
	public Map<Engine, EvaluationEngine> evaluationEngines(){
		Map<Engine, EvaluationEngine> engines = new HashMap<>();
		try {
			engines.put(Engine.STOCKFISH, new EvaluationUciEngine("/usr/games/stockfish", Engine.STOCKFISH.label()) {
				@Override
				public String toString() {
					return Engine.STOCKFISH.toString();
				}
			});
		} catch (Exception e) {
			logger.info("Failed to create player engine stockfish 16"); 
		}
		try {
			engines.put(Engine.GNUCHESS, new EvaluationUciEngine("/usr/games/gnuchessu", Engine.GNUCHESS.label()) {
				@Override
				public String toString() {
					return Engine.GNUCHESS.toString();
				}
			});
		} catch (Exception e) {
			logger.info("Failed to create player engine gnuchess"); 
		}
		return engines;
	} 
}
