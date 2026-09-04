package demo.chess.admin.impl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import demo.chess.admin.Admin;
import demo.chess.definitions.board.impl.ChessBoard;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.moves.impl.MoveListImpl;
import demo.chess.definitions.players.impl.BlackPlayerImpl;
import demo.chess.definitions.players.impl.WhitePlayerImpl;
import demo.chess.game.Game;
import demo.chess.game.impl.ChessGame;

/**
 * Configuration class providing the chess game bean and implementing the Admin
 * interface.
 */
@Configuration
public class ChessAdmin implements Admin {

	/**
	 * Performs the chess game operation.
	 * @param time the time
	 * @return the result of the operation
	 */
	@Override
	@Bean
	@Scope("prototype")
	public Game chessGame(int time) {
		MoveList moveList = new MoveListImpl();
		return new ChessGame(new ChessBoard(), new WhitePlayerImpl(moveList, "ChessGame"),
				new BlackPlayerImpl(moveList, "ChessGame"), moveList, this, time);
	}
}
