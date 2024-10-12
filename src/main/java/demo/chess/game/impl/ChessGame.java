package demo.chess.game.impl;

import java.io.IOException;

import demo.chess.admin.Admin;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.players.BlackPlayer;
import demo.chess.definitions.players.WhitePlayer;

/**
 * The ChessGame class implements the core functionality for applying different
 * types of chess moves.
 */
public class ChessGame extends ChessGameTemplate {

	private Admin admin;

	final int timeForEachPlayer;
	
	int incrementForWhite;	
	int incrementForBlack;


	/**
	 * Constructs a ChessGame instance with the given chessboard, white player,
	 * black player, and move list.
	 *
	 * @param chessBoard  the chess board
	 * @param whitePlayer the white player
	 * @param blackPlayer the black player
	 * @param moveList    the list of moves
	 * @param chessAdmin
	 * @throws Exception
	 */
	public ChessGame(Board chessBoard, WhitePlayer whitePlayer, BlackPlayer blackPlayer, MoveList moveList, Admin chessAdmin, int timeForEachPlayer) throws Exception {
		super(chessBoard, whitePlayer, blackPlayer, moveList);
		this.setAdmin(chessAdmin);
		this.timeForEachPlayer = timeForEachPlayer;
	}

	public Admin getAdmin() {
		return admin;
	}

	public void setAdmin(Admin admin) {
		this.admin = admin;
	}

	/**
	 * Switches the current player to the other player.
	 */
	@Override
	public void switchPlayer() {
		if (!getPlayer().getChessClock().isStarted()) {
			getPlayer().getChessClock().start();
		}
		getPlayer().getChessClock().addIncrement();
		getPlayer().getChessClock().suspend();
		super.switchPlayer();
		if (!getPlayer().getChessClock().isStarted()) {
			getPlayer().getChessClock().start();
		} else {
			getPlayer().getChessClock().resume();
		}
	}

	@Override
	public int getTimeForEachPlayer() {
		return timeForEachPlayer;
	}

	@Override
	public int getIncrementForWhite() {
		return incrementForWhite;
	}

	@Override
	public void setIncrementForWhite(int incrementForWhite) {
		this.incrementForWhite = incrementForWhite;
	}

	@Override
	public int getIncrementForBlack() {
		return incrementForBlack;
	}

	@Override
	public void setIncrementForBlack(int incrementForBlack) {
		this.incrementForBlack = incrementForBlack;
	}

	/**
	 * Applies the given move to the chess game and computes actual stockfish evaluation.
	 *
	 * @param move the move to apply
	 * @throws IOException
	 * @throws NoMoveFoundException
	 */
	@Override
	public void apply(Move move) throws NoMoveFoundException, IOException {
		super.apply(move);
	}
}
