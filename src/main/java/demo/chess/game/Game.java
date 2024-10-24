package demo.chess.game;

import java.io.IOException;
import java.util.List;

import demo.chess.definitions.board.Board;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.players.Player;
import demo.chess.definitions.states.State;

/**
 * Interface representing the core functionalities of a chess game.
 */
public interface Game {

	/**
	 * Returns the chessboard.
	 *
	 * @return the chessboard
	 */
	Board getChessBoard();

	/**
	 * Returns the list of moves.
	 *
	 * @return the list of moves
	 */
	MoveList getMoveList();

	/**
	 * Returns the white player.
	 *
	 * @return the white player
	 */
	Player getWhitePlayer();

	/**
	 * Returns the black player.
	 *
	 * @return the black player
	 */
	Player getBlackPlayer();

	/**
	 * Returns the current player.
	 *
	 * @return the current player
	 */
	Player getPlayer();

	/**
	 * Sets the current player.
	 *
	 * @param player the player to set as the current player
	 */
	void setPlayer(Player player);

	/**
	 * Creates and initializes the chess pieces for both players.
	 */
	void createPieces();

	/**
	 * Switches the current player to the other player.
	 */
	void switchPlayer();

	/**
	 * Applies the given move to the chess game.
	 *
	 * @param move the move to apply
	 * @throws IOException
	 * @throws NoMoveFoundException
	 */
	void apply(Move move) throws NoMoveFoundException, IOException;

	void setState(State stalemate);

	State getState();

	int getTimeForEachPlayer();

	int getIncrementForWhite();

	void setIncrementForWhite(int incrementForWhite);

	int getIncrementForBlack();

	void setIncrementForBlack(int incrementForBlack);

	List<String> getSanMoveList();

	void setSanMoveList(List<String> sanMoveList);
}
