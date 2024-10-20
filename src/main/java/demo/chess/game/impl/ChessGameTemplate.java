package demo.chess.game.impl;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import demo.chess.definitions.Color;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Castling;
import demo.chess.definitions.moves.EnPassant;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.moves.Promotion;
import demo.chess.definitions.pieces.Piece;
import demo.chess.definitions.pieces.impl.Bishop;
import demo.chess.definitions.pieces.impl.King;
import demo.chess.definitions.pieces.impl.Knight;
import demo.chess.definitions.pieces.impl.Pawn;
import demo.chess.definitions.pieces.impl.Queen;
import demo.chess.definitions.pieces.impl.Rook;
import demo.chess.definitions.players.BlackPlayer;
import demo.chess.definitions.players.Player;
import demo.chess.definitions.players.WhitePlayer;
import demo.chess.definitions.states.State;
import demo.chess.game.Game;

/**
 * Abstract class providing the core functionalities and setup for a chess game.
 */
public abstract class ChessGameTemplate implements Game {

	private Player player;
	private final WhitePlayer whitePlayer;
	private final BlackPlayer blackPlayer;
	private final Board chessBoard;
	private final MoveList moveList;
	private State state = null;

	/**
	 * Constructs a ChessGameTemplate instance with the given chessboard, white
	 * player, black player, and move list.
	 *
	 * @param chessBoard  the chess board
	 * @param whitePlayer the white player
	 * @param blackPlayer the black player
	 * @param moveList    the list of moves
	 * @param chessAdmin
	 */
	public ChessGameTemplate(Board chessBoard, WhitePlayer whitePlayer, BlackPlayer blackPlayer, MoveList moveList) {
		this.player = whitePlayer;
		this.whitePlayer = whitePlayer;
		this.blackPlayer = blackPlayer;
		this.chessBoard = chessBoard;
		this.moveList = moveList;
		createPieces();
	}

	/**
	 * Creates and initializes the chess pieces for both players.
	 */
	@Override
	public void createPieces() {
		List<Piece> whitePieces = whitePlayer.getPieces();
		List<Piece> blackPieces = blackPlayer.getPieces();

		for (int i = 1; i < 9; i++) {
			whitePieces.add(new Pawn(Color.WHITE, chessBoard.getField(i, 2), chessBoard, true));
			blackPieces.add(new Pawn(Color.BLACK, chessBoard.getField(i, 7), chessBoard, true));
		}

		whitePieces.add(new Rook(Color.WHITE, chessBoard.getField(1, 1), chessBoard, true));
		whitePieces.add(new Rook(Color.WHITE, chessBoard.getField(8, 1), chessBoard, true));

		blackPieces.add(new Rook(Color.BLACK, chessBoard.getField(1, 8), chessBoard, true));
		blackPieces.add(new Rook(Color.BLACK, chessBoard.getField(8, 8), chessBoard, true));

		whitePieces.add(new Knight(Color.WHITE, chessBoard.getField(2, 1), chessBoard, true));
		whitePieces.add(new Knight(Color.WHITE, chessBoard.getField(7, 1), chessBoard, true));

		blackPieces.add(new Knight(Color.BLACK, chessBoard.getField(2, 8), chessBoard, true));
		blackPieces.add(new Knight(Color.BLACK, chessBoard.getField(7, 8), chessBoard, true));

		whitePieces.add(new Bishop(Color.WHITE, chessBoard.getField(3, 1), chessBoard, true));
		whitePieces.add(new Bishop(Color.WHITE, chessBoard.getField(6, 1), chessBoard, true));

		blackPieces.add(new Bishop(Color.BLACK, chessBoard.getField(3, 8), chessBoard, true));
		blackPieces.add(new Bishop(Color.BLACK, chessBoard.getField(6, 8), chessBoard, true));

		whitePieces.add(new Queen(Color.WHITE, chessBoard.getField(4, 1), chessBoard, true));

		King whiteKing = new King(Color.WHITE, chessBoard.getField(5, 1), chessBoard, true);
		whitePieces.add(whiteKing);
		this.getWhitePlayer().setKing(whiteKing);

		blackPieces.add(new Queen(Color.BLACK, chessBoard.getField(4, 8), chessBoard, true));

		King blackKing = new King(Color.BLACK, chessBoard.getField(5, 8), chessBoard, true);
		blackPieces.add(blackKing);
		this.getBlackPlayer().setKing(blackKing);

		whitePieces.forEach(piece -> piece.setMoveList(moveList));
		blackPieces.forEach(piece -> piece.setMoveList(moveList));
	}

	/**
	 * Returns the chessboard.
	 *
	 * @return the chessboard
	 */
	@Override
	public Board getChessBoard() {
		return chessBoard;
	}

	/**
	 * Returns the white player.
	 *
	 * @return the white player
	 */
	@Override
	public Player getWhitePlayer() {
		return whitePlayer;
	}

	/**
	 * Returns the black player.
	 *
	 * @return the black player
	 */
	@Override
	public Player getBlackPlayer() {
		return blackPlayer;
	}

	/**
	 * Returns the current player.
	 *
	 * @return the current player
	 */
	@Override
	public Player getPlayer() {
		return player;
	}

	/**
	 * Sets the current player.
	 *
	 * @param player the player to set as the current player
	 */
	@Override
	public void setPlayer(Player player) {
		this.player = player;
	}

	/**
	 * Returns the move list.
	 *
	 * @return the move list
	 */
	@Override
	public MoveList getMoveList() {
		return moveList;
	}

	@Override
	public State getState() {
		return state;
	}

	@Override
	public void setState(State state) {
		if (this.getWhitePlayer().getChessClock().isStarted()) {
			this.getWhitePlayer().getChessClock().stop();
		}
		if (this.getBlackPlayer().getChessClock().isStarted()) {
			this.getBlackPlayer().getChessClock().stop();
		}
		this.state = state;
	}

	/**
	 * Switches the current player to the other player.
	 */
	@Override
	public void switchPlayer() {
		if (getPlayer() == getWhitePlayer()) {
			setPlayer(getBlackPlayer());
		} else {
			setPlayer(getWhitePlayer());
		}
	}

	/**
	 * Applies the given move to the chess game.
	 *
	 * @param move the move to apply
	 * @throws IOException
	 * @throws NoMoveFoundException
	 */
	@Override
	public void apply(Move move) throws NoMoveFoundException, IOException {
		boolean applied = false;
		boolean isRealGame = this instanceof ChessGame;
		Player opponent = getPlayer().getColor().equals(Color.WHITE) ? this.getBlackPlayer() : this.getWhitePlayer();
		if (isRealGame) {
			if ((getPlayer().getChessClock().getTime(TimeUnit.MILLISECONDS) / 1000 > getTimeForEachPlayer())
					|| (opponent.getChessClock().getTime(TimeUnit.MILLISECONDS) / 1000 > getTimeForEachPlayer())) {
				setState(State.LOST_ON_TIME);
				return;
			}
		}
		if (move instanceof EnPassant) {
			applyEnPassant(move);
			applied = true;
		}
		if (move instanceof Promotion) {
			applyPromotion((Promotion) move);
			applied = true;
		}
		if (move instanceof Castling) {
			applyCastling((Castling) move);
			applied = true;
		}
		if (!applied) {
			applyRegularMove(move);
		}

		if (isRealGame) {
			if (getPlayer().getValidMoves(this).isEmpty()) {
				getPlayer().resignOrStaleMate(this);
			}
		}
	}

	/**
	 * Applies a castling move.
	 *
	 * @param move the castling move
	 */
	private void applyCastling(Castling move) {
		King king = (King) move.getPiece();
		Rook rook = move.getRook();

		king.getField().setPiece(null);
		rook.getField().setPiece(null);

		Field fieldRook;
		Field fieldKing;

		if (king.getColor().equals(Color.WHITE)) {
			if (rook.getField().equals(getChessBoard().getField(1, 1))) {
				fieldRook = getChessBoard().getField(4, 1);
				fieldKing = getChessBoard().getField(3, 1);
			} else {
				fieldRook = getChessBoard().getField(6, 1);
				fieldKing = getChessBoard().getField(7, 1);
			}
			king.setField(fieldKing);
			fieldKing.setPiece(king);
			rook.setField(fieldRook);
			fieldRook.setPiece(rook);
		} else {
			if (rook.getField().equals(getChessBoard().getField(1, 8))) {
				fieldRook = getChessBoard().getField(4, 8);
				fieldKing = getChessBoard().getField(3, 8);
			} else {
				fieldRook = getChessBoard().getField(6, 8);
				fieldKing = getChessBoard().getField(7, 8);
			}
			king.setField(fieldKing);
			fieldKing.setPiece(king);
			rook.setField(fieldRook);
			fieldRook.setPiece(rook);
		}
		getMoveList().add(move);
		switchPlayer();
	}

	/**
	 * Applies a promotion move.
	 *
	 * @param move the promotion move
	 */
	private void applyPromotion(Promotion move) {
		Field source = move.getSource();
		Field target = move.getTarget();
		Piece sourcePiece = move.getPiece();
		Piece promotedPiece = move.getPromotedPiece();
		Piece targetPiece = move.getTarget().getPiece();

		sourcePiece.setField(null);
		promotedPiece.setField(target);
		promotedPiece.setMoveList(getMoveList());
		target.setPiece(promotedPiece);
		getPlayer().getPieces().add(promotedPiece);
		getPlayer().getPieces().remove(sourcePiece);
		source.setPiece(null);
		getMoveList().add(move);
		switchPlayer();

		if (targetPiece != null) {
			targetPiece.setField(null);
			getPlayer().getPieces().remove(targetPiece);
		}
	}

	/**
	 * Applies a regular move.
	 *
	 * @param move the regular move
	 */
	private void applyRegularMove(Move move) {
		Field source = move.getSource();
		Field target = move.getTarget();
		Piece sourcePiece = move.getPiece();
		Piece targetPiece = move.getTarget().getPiece();
		sourcePiece.setField(target);
		target.setPiece(sourcePiece);
		source.setPiece(null);
		getMoveList().add(move);
		switchPlayer();

		if (targetPiece != null) {
			targetPiece.setField(null);
			getPlayer().getPieces().remove(targetPiece);
		}
	}

	/**
	 * Applies an en passant move.
	 *
	 * @param move the en passant move
	 */
	private void applyEnPassant(Move move) {
		EnPassant enPassant = (EnPassant) move;
		Field source = enPassant.getSource();
		Field target = enPassant.getTarget();
		Pawn sourcePawn = (Pawn) enPassant.getPiece();
		sourcePawn.setField(target);
		target.setPiece(sourcePawn);
		source.setPiece(null);

		Pawn slayedPawn = enPassant.getSlayedPiece();
		Field fieldOfSlayedPawn = slayedPawn.getField();

		fieldOfSlayedPawn.setPiece(null);
		getMoveList().add(move);
		switchPlayer();
		getPlayer().getPieces().remove(slayedPawn);
	}

}
