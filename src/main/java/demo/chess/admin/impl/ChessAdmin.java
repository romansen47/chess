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
	 * Returns the chess game instance managed by the admin.
	 * <p>
	 * This method is marked as a Spring bean, so it will be managed by the Spring
	 * container.
	 * </p>
	 *
	 * @return a new instance of {@link ChessGame}
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
