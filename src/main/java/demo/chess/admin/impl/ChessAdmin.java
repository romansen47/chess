package demo.chess.admin.impl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import demo.chess.admin.Admin;
import demo.chess.definitions.board.impl.ChessBoard;
import demo.chess.definitions.engines.EvaluationEngine;
import demo.chess.definitions.engines.impl.EvaluationStockfishEngine;
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
		return new ChessGame(new ChessBoard(), new WhitePlayerImpl(moveList, "ChessGame"), new BlackPlayerImpl(moveList, "ChessGame"), moveList, this, time);
	}

    @Override
	@Bean
	@Scope("prototype")
	public Game simulation() {
    	MoveList moveList = new MoveListImpl();
		return new Simulation(new ChessBoard(), new WhitePlayerImpl(moveList, "Simulation"), new BlackPlayerImpl(moveList, "Simulation"), moveList);
	}

	@Override
	@Bean
	public EvaluationEngine evaluationEngine() throws Exception {
		EvaluationEngine engine;
		try {
			engine = new EvaluationStockfishEngine("C:\\Temp\\stockfish-windows-x86-64-avx2.exe"){
				@Override
				public String toString() {
					return "windows evaluationEngine";
				};
			};
		} catch(Exception e) {
			engine = new EvaluationStockfishEngine("/usr/games/stockfish"){
				@Override
				public String toString() {
					return "linux evaluationEngine";
				};
			};
		};
		return engine;
	}
}
