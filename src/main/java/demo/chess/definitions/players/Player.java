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
	 * Returns the color of the player.
	 *
	 * @return the color of the player
	 */
	Color getColor();

	/**
	 * Returns the list of pieces owned by the player.
	 *
	 * @return the list of pieces
	 */
	List<Piece> getPieces();

	/**
	 * Returns the list of valid moves the player can make.
	 *
	 * @return the list of valid moves
	 * @throws IOException
	 * @throws NoMoveFoundException
	 */
	List<Move> getValidMoves(Game chessGame) throws NoMoveFoundException, IOException;

	/**
	 * Validates the given move in the context of the specified game.
	 *
	 * @param game the game context
	 * @param move the move to validate
	 * @return the validated move, or null if the move is invalid
	 * @throws IOException
	 * @throws NoMoveFoundException
	 */
	Move replaceByValidMove(Game game, Move move) throws NoMoveFoundException, IOException;

	/**
	 * Resets the player's state to the initial configuration.
	 */
	void reset();

	/**
	 * Returns the move list associated with the player.
	 *
	 * @return the move list
	 */
	MoveList getMoveList();

	/**
	 * Returns a simple list of moves the player can make.
	 *
	 * @return the list of simple moves
	 */
	List<Move> getSimpleMoves();

	/**
	 * Returns the king piece owned by the player.
	 *
	 * @return the king piece
	 */
	Piece getKing();

	/**
	 * Sets the king piece for the player.
	 *
	 * @param king the king piece to set
	 */
	void setKing(Piece king);

	void resignOrStaleMate(Game chessGame);

	/**
	 * @return the stopWatch
	 */
	ChessClock getChessClock();

	/**
	 * @param stopWatch the stopWatch to set
	 */
	void setChessClock(ChessClock stopWatch);

	void setupClock(int timeForEachPlayer, int incrementForWhite, Runnable runnable);

	/**
	 * Gets the corresponding move in the simulation.
	 *
	 * @param simulation the game simulation
	 * @param m          the move to process in the simulation
	 * @return the corresponding move in the simulation
	 */
	Move getMoveInSimulation(Game simulation, Move m);
}
