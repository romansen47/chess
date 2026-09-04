package demo.chess.definitions.players;

import java.io.IOException;
import java.util.List;

import demo.chess.definitions.Color;
import demo.chess.definitions.clocks.impl.ChessClock;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.pieces.Piece;
import demo.chess.game.Game;

/**
 * Interface representing a player in a chess game.
 */
public interface Player {

	/**
	 * Returns the color.
	 * @return the color
	 */
	Color getColor();

	/**
	 * Returns the name.
	 * @return the name
	 */
	String getName();

	/**
	 * Returns the pieces.
	 * @return the pieces
	 */
	List<Piece> getPieces();

	/**
	 * Returns the valid moves.
	 * @param chessGame the chess game
	 * @return the valid moves
	 */
	List<Move> getValidMoves(Game chessGame) throws NoMoveFoundException, IOException;

	/**
	 * Performs the replace by valid move operation.
	 * @param game the game
	 * @param move the move
	 * @return the result of the operation
	 */
	Move replaceByValidMove(Game game, Move move) throws NoMoveFoundException, IOException;

	/**
	 * Performs the reset operation.
	 */
	void reset();

	/**
	 * Returns the move list.
	 * @return the move list
	 */
	MoveList getMoveList();

	/**
	 * Returns the simple moves.
	 * @return the simple moves
	 */
	List<Move> getSimpleMoves();

	/**
	 * Returns the king.
	 * @return the king
	 */
	Piece getKing();

	/**
	 * Sets the king.
	 * @param king the king
	 */
	void setKing(Piece king);

	/**
	 * Performs the resign or stale mate operation.
	 * @param chessGame the chess game
	 */
	void resignOrStaleMate(Game chessGame);

	/**
	 * Returns the chess clock.
	 * @return the chess clock
	 */
	ChessClock getChessClock();

	/**
	 * Sets the chess clock.
	 * @param stopWatch the stop watch
	 */
	void setChessClock(ChessClock stopWatch);

	/**
	 * Sets the up clock.
	 * @param timeForEachPlayer the time for each player
	 * @param incrementForWhite the increment for white
	 * @param runnable the runnable
	 */
	void setupClock(int timeForEachPlayer, int incrementForWhite, Runnable runnable);

	/**
	 * Returns the move in simulation.
	 * @param simulation the simulation
	 * @param m the m
	 * @return the move in simulation
	 */
	Move getMoveInSimulation(Game simulation, Move m);

	/**
	 * Sets the additional time.
	 * @param additionalTime the additional time
	 */
	void setAdditionalTime(int additionalTime);

	/**
	 * Returns the additional time.
	 * @return the additional time
	 */
	int getAdditionalTime();
}
