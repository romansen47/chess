package demo.chess.game.impl;

import demo.chess.definitions.board.Board;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.players.BlackPlayer;
import demo.chess.definitions.players.WhitePlayer;

public class Simulation extends ChessGameTemplate {

	/**
	 * Constructs a ChessGame instance with the given chessboard, white player,
	 * black player, and move list.
	 *
	 * @param chessBoard  the chess board
	 * @param whitePlayer the white player
	 * @param blackPlayer the black player
	 * @param moveList    the list of moves
	 * @param chessAdmin
	 */
	public Simulation(Board chessBoard, WhitePlayer whitePlayer, BlackPlayer blackPlayer, MoveList moveList) {
		super(chessBoard, whitePlayer, blackPlayer, moveList);
	}

	@Override
	public int getTimeForEachPlayer() {
		return 10000;
	}

	@Override
	public int getIncrementForWhite() {
		return 0;
	}

	@Override
	public void setIncrementForWhite(int incrementForWhite) {
	}

	@Override
	public int getIncrementForBlack() {
		return 0;
	}

	@Override
	public void setIncrementForBlack(int incrementForBlack) {

	}

}
