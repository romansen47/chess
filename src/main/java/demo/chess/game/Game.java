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
	 * Returns the chess board.
	 * @return the chess board
	 */
	Board getChessBoard();

	/**
	 * Returns the move list.
	 * @return the move list
	 */
	MoveList getMoveList();

	/**
	 * Returns the white player.
	 * @return the white player
	 */
	Player getWhitePlayer();

	/**
	 * Returns the black player.
	 * @return the black player
	 */
	Player getBlackPlayer();

	/**
	 * Returns the player.
	 * @return the player
	 */
	Player getPlayer();

	/**
	 * Sets the player.
	 * @param player the player
	 */
	void setPlayer(Player player);

	/**
	 * Creates the pieces.
	 */
	void createPieces();

	/**
	 * Performs the switch player operation.
	 */
	void switchPlayer();

	/**
	 * Performs the apply operation.
	 * @param move the move
	 */
	void apply(Move move) throws NoMoveFoundException, IOException;

	/**
	 * Sets the state.
	 * @param stalemate the stalemate
	 */
	void setState(State stalemate);

	/**
	 * Returns the state.
	 * @return the state
	 */
	State getState();

	/**
	 * Returns the time for each player.
	 * @return the time for each player
	 */
	int getTimeForEachPlayer();

	/**
	 * Returns the increment for white.
	 * @return the increment for white
	 */
	int getIncrementForWhite();

	/**
	 * Sets the increment for white.
	 * @param incrementForWhite the increment for white
	 */
	void setIncrementForWhite(int incrementForWhite);

	/**
	 * Returns the increment for black.
	 * @return the increment for black
	 */
	int getIncrementForBlack();

	/**
	 * Sets the increment for black.
	 * @param incrementForBlack the increment for black
	 */
	void setIncrementForBlack(int incrementForBlack);

	/**
	 * Returns the san move list.
	 * @return the san move list
	 */
	List<String> getSanMoveList();

	/**
	 * Sets the san move list.
	 * @param sanMoveList the san move list
	 */
	void setSanMoveList(List<String> sanMoveList);
}
