package demo.chess.definitions.pieces.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import demo.chess.definitions.Color;
import demo.chess.definitions.PieceType;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.impl.ChessMove;
import demo.chess.definitions.moves.impl.EnPassantImpl;
import demo.chess.definitions.moves.impl.PromotionImpl;
import demo.chess.definitions.pieces.Piece;

/**
 * Implementation of the pawn piece in a chess game.
 */
public class Pawn extends PieceImpl {

	private static final Logger logger = LogManager.getLogger(Pawn.class);

	/**
	 * Creates a new Pawn instance.
	 * @param color the color
	 * @param field the field
	 * @param chessBoard the chess board
	 * @param setField the set field
	 */
	public Pawn(Color color, Field field, Board chessBoard, boolean setField) {
		super(color, field, chessBoard, setField);
	}

	/**
	 * Returns a string representation of this object.
	 * @return the result of the operation
	 */
	@Override
	public String toString() {
		return getColor().label + "p" + getField().getName();
	}

	/**
	 * Returns the possible unvalidated moves.
	 * @return the possible unvalidated moves
	 */
	@Override
	public List<Move> getPossibleUnvalidatedMoves() {
		List<Move> moveList = new ArrayList<>();
		int originFile = this.getField().getFile();
		int originRank = this.getField().getRank();
		moveList.addAll(addSimpleMoves(originFile, originRank));
		moveList.addAll(addSpecialMoves(originFile, originRank));
		return new ArrayList<>(new HashSet<>(moveList));
	}

	/**
	 * Returns the simple unvalidated moves.
	 * @return the simple unvalidated moves
	 */
	@Override
	public List<Move> getSimpleUnvalidatedMoves() {
		List<Move> moveList = new ArrayList<>();
		int originFile = this.getField().getFile();
		int originRank = this.getField().getRank();
		if (getColor().equals(Color.WHITE)) {
			if (originFile > 1) {
				Field targetLeft = getChessBoard().getField(originFile - 1, originRank + 1);
				if (targetLeft.getPiece() != null && !targetLeft.getPiece().getColor().equals(this.getColor())) {
					Move move = new ChessMove(this, getField(), targetLeft);
					moveList.add(move);
				}
			}
			if (originFile < 8) {
				Field targetRight = getChessBoard().getField(originFile + 1, originRank + 1);
				if (targetRight.getPiece() != null && !targetRight.getPiece().getColor().equals(this.getColor())) {
					Move move = new ChessMove(this, getField(), targetRight);
					moveList.add(move);
				}
			}
			Field target = getChessBoard().getField(originFile, originRank + 1);
			if (target.getPiece() == null) {
				Move move = new ChessMove(this, getField(), target);
				moveList.add(move);
			}
		} else {
			if (originFile > 1) {
				Field targetLeft = getChessBoard().getField(originFile - 1, originRank - 1);
				if (targetLeft.getPiece() != null && !targetLeft.getPiece().getColor().equals(this.getColor())) {
					Move move = new ChessMove(this, getField(), targetLeft);
					moveList.add(move);
				}
			}
			if (originFile < 8) {
				Field targetRight = getChessBoard().getField(originFile + 1, originRank - 1);
				if (targetRight.getPiece() != null && !targetRight.getPiece().getColor().equals(this.getColor())) {
					Move move = new ChessMove(this, getField(), targetRight);
					moveList.add(move);
				}
			}
			Field target = getChessBoard().getField(originFile, originRank - 1);
			if (target.getPiece() == null) {
				Move move = new ChessMove(this, getField(), target);
				moveList.add(move);
			}
		}
		return new ArrayList<>(new HashSet<>(moveList));
	}

	/**
	 * Returns the logger.
	 * @return the logger
	 */
	@Override
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Adds the simple moves.
	 * @param originFile the origin file
	 * @param originRank the origin rank
	 * @return the result of the operation
	 */
	private List<Move> addSimpleMoves(int originFile, int originRank) {
		List<Move> moveList = new ArrayList<>();
		if (getColor().equals(Color.WHITE)) {
			moveList.addAll(addSimpleMovesForWhitePawn(originFile, originRank));
		} else {
			moveList.addAll(addSimpleMovesForBlackPawn(originFile, originRank));
		}
		return moveList;
	}

	/**
	 * Adds the simple moves for white pawn.
	 * @param originFile the origin file
	 * @param originRank the origin rank
	 * @return the result of the operation
	 */
	private List<Move> addSimpleMovesForWhitePawn(int originFile, int originRank) {
		List<Move> moveList = new ArrayList<>();
		if (originRank == 7) {
			return moveList;
		}
		if (originRank == 2) {
			Field tmpField = getChessBoard().getField(originFile, originRank + 1);
			Move move = new ChessMove(this, getField(), tmpField);
			if (tmpField.getPiece() == null) {
				moveList.add(move);
				Field tmpField2 = getChessBoard().getField(originFile, originRank + 2);
				if (tmpField2.getPiece() == null) {
					Move move2 = new ChessMove(this, getField(), tmpField2);
					moveList.add(move2);
				}
			}
		}
		moveList.addAll(getSimpleUnvalidatedMoves());
		return moveList;
	}

	/**
	 * Adds the simple moves for black pawn.
	 * @param originFile the origin file
	 * @param originRank the origin rank
	 * @return the result of the operation
	 */
	private List<Move> addSimpleMovesForBlackPawn(int originFile, int originRank) {
		List<Move> moveList = new ArrayList<>();
		if (originRank == 2) {
			return moveList;
		}
		if (originRank == 7) {
			Field tmpField = getChessBoard().getField(originFile, originRank - 1);
			Move move = new ChessMove(this, getField(), tmpField);
			if (tmpField.getPiece() == null) {
				moveList.add(move);
				Field tmpField2 = getChessBoard().getField(originFile, originRank - 2);
				if (tmpField2.getPiece() == null) {
					Move move2 = new ChessMove(this, getField(), tmpField2);
					moveList.add(move2);
				}
			}
		}
		moveList.addAll(getSimpleUnvalidatedMoves());
		return moveList;
	}

	/**
	 * Adds the special moves.
	 * @param originFile the origin file
	 * @param originRank the origin rank
	 * @return the result of the operation
	 */
	private List<Move> addSpecialMoves(int originFile, int originRank) {
		List<Move> moveList = new ArrayList<>();
		if (getColor().equals(Color.WHITE)) {
			moveList.addAll(addSpecialMovesForWhite(originFile, originRank));
		} else {
			moveList.addAll(addSpecialMovesForBlack(originFile, originRank));
		}
		return moveList;
	}

	/**
	 * Adds the special moves for white.
	 * @param originFile the origin file
	 * @param originRank the origin rank
	 * @return the result of the operation
	 */
	private List<Move> addSpecialMovesForWhite(int originFile, int originRank) {
		List<Move> moveList = new ArrayList<>();
		moveList.addAll(addEnPassantMovesForWhite(originFile, originRank));
		moveList.addAll(addPromotionMovesForWhite(originFile, originRank));
		moveList.addAll(addCastlingMovesForWhite(originFile, originRank));
		return moveList;
	}

	/**
	 * Adds the promotion moves for white.
	 * @param originFile the origin file
	 * @param originRank the origin rank
	 * @return the result of the operation
	 */
	private List<Move> addPromotionMovesForWhite(int originFile, int originRank) {
		List<Move> moveList = new ArrayList<>();
		if (originRank == 7) {
			if (originFile > 1) {
				Field targetField = getChessBoard().getField(originFile - 1, originRank + 1);
				Piece targetPiece = targetField.getPiece();
				if (targetPiece != null && !targetPiece.getColor().equals(getColor())) {
					moveList.add(new PromotionImpl(this, getField(), targetField,
							new Queen(this.getColor(), targetField, this.getChessBoard(), false)));
					moveList.add(new PromotionImpl(this, getField(), targetField,
							new Rook(this.getColor(), targetField, this.getChessBoard(), false)));
					moveList.add(new PromotionImpl(this, getField(), targetField,
							new Knight(this.getColor(), targetField, this.getChessBoard(), false)));
					moveList.add(new PromotionImpl(this, getField(), targetField,
							new Bishop(this.getColor(), targetField, this.getChessBoard(), false)));
				}
			}
			if (originFile < 8) {
				Field targetField = getChessBoard().getField(originFile + 1, originRank + 1);
				Piece targetPiece = targetField.getPiece();
				if (targetPiece != null && !targetPiece.getColor().equals(getColor())) {
					moveList.add(new PromotionImpl(this, getField(), targetField,
							new Queen(this.getColor(), targetField, this.getChessBoard(), false)));
					moveList.add(new PromotionImpl(this, getField(), targetField,
							new Rook(this.getColor(), targetField, this.getChessBoard(), false)));
					moveList.add(new PromotionImpl(this, getField(), targetField,
							new Knight(this.getColor(), targetField, this.getChessBoard(), false)));
					moveList.add(new PromotionImpl(this, getField(), targetField,
							new Bishop(this.getColor(), targetField, this.getChessBoard(), false)));
				}
			}
			if (getChessBoard().getField(originFile, originRank + 1).getPiece() == null) {
				Field targetField = getChessBoard().getField(originFile, originRank + 1);
				moveList.add(new PromotionImpl(this, getField(), targetField,
						new Queen(this.getColor(), targetField, this.getChessBoard(), false)));
				moveList.add(new PromotionImpl(this, getField(), targetField,
						new Rook(this.getColor(), targetField, this.getChessBoard(), false)));
				moveList.add(new PromotionImpl(this, getField(), targetField,
						new Knight(this.getColor(), targetField, this.getChessBoard(), false)));
				moveList.add(new PromotionImpl(this, getField(), targetField,
						new Bishop(this.getColor(), targetField, this.getChessBoard(), false)));
			}
		}
		return moveList;
	}

	/**
	 * Adds the en passant moves for white.
	 * @param originFile the origin file
	 * @param originRank the origin rank
	 * @return the result of the operation
	 */
	private List<Move> addEnPassantMovesForWhite(int originFile, int originRank) {
		List<Move> moveList = new ArrayList<>();
		if (originRank == 5) {
			Move previousMove = this.getMoveList().get(getMoveList().size() - 1);
			Field originPrevMove = previousMove.getSource();
			Field targetPrevMove = previousMove.getTarget();
			Piece piecePrevMove = previousMove.getPiece();
			if (originFile > 1) {
				Field enPassantField = getChessBoard().getField(originFile - 1, originRank + 1);
				boolean condition = piecePrevMove.getType().equals(PieceType.PAWN)
						&& targetPrevMove.getRank() == originRank && targetPrevMove.getFile() == originFile - 1
						&& originPrevMove.getRank() == 7;
				if (condition) {
					Move move = new EnPassantImpl(this, getField(), enPassantField, (Pawn) piecePrevMove);
					moveList.add(move);
				}
			}
			if (originFile < 8) {
				Field enPassantField = getChessBoard().getField(originFile + 1, originRank + 1);
				boolean condition = piecePrevMove.getType().equals(PieceType.PAWN)
						&& targetPrevMove.getRank() == originRank && targetPrevMove.getFile() == originFile + 1
						&& originPrevMove.getRank() == 7;
				if (condition) {
					Move move = new EnPassantImpl(this, getField(), enPassantField, (Pawn) piecePrevMove);
					moveList.add(move);
				}
			}
		}
		return moveList;
	}

	/**
	 * Adds the castling moves for white.
	 * @param originFile the origin file
	 * @param originRank the origin rank
	 * @return the result of the operation
	 */
	private List<Move> addCastlingMovesForWhite(int originFile, int originRank) {
		// Pawns cannot perform castling moves; returning an empty list
		return new ArrayList<>();
	}

	/**
	 * Adds the special moves for black.
	 * @param originFile the origin file
	 * @param originRank the origin rank
	 * @return the result of the operation
	 */
	private List<Move> addSpecialMovesForBlack(int originFile, int originRank) {
		List<Move> moveList = new ArrayList<>();
		moveList.addAll(addEnPassantMovesForBlack(originFile, originRank));
		moveList.addAll(addPromotionMovesForBlack(originFile, originRank));
		moveList.addAll(addCastlingMovesForBlack(originFile, originRank));
		return moveList;
	}

	/**
	 * Adds the promotion moves for black.
	 * @param originFile the origin file
	 * @param originRank the origin rank
	 * @return the result of the operation
	 */
	private List<Move> addPromotionMovesForBlack(int originFile, int originRank) {
		List<Move> moveList = new ArrayList<>();
		if (originRank == 2) {
			if (originFile > 1) {
				Field targetField = getChessBoard().getField(originFile - 1, originRank - 1);
				Piece targetPiece = targetField.getPiece();
				if (targetPiece != null && !targetPiece.getColor().equals(getColor())) {
					moveList.add(new PromotionImpl(this, getField(), targetField,
							new Queen(this.getColor(), targetField, this.getChessBoard(), false)));
					moveList.add(new PromotionImpl(this, getField(), targetField,
							new Rook(this.getColor(), targetField, this.getChessBoard(), false)));
					moveList.add(new PromotionImpl(this, getField(), targetField,
							new Knight(this.getColor(), targetField, this.getChessBoard(), false)));
					moveList.add(new PromotionImpl(this, getField(), targetField,
							new Bishop(this.getColor(), targetField, this.getChessBoard(), false)));
				}
			}
			if (originFile < 8) {
				Field targetField = getChessBoard().getField(originFile + 1, originRank - 1);
				Piece targetPiece = targetField.getPiece();
				if (targetPiece != null && !targetPiece.getColor().equals(getColor())) {
					moveList.add(new PromotionImpl(this, getField(), targetField,
							new Queen(this.getColor(), targetField, this.getChessBoard(), false)));
					moveList.add(new PromotionImpl(this, getField(), targetField,
							new Rook(this.getColor(), targetField, this.getChessBoard(), false)));
					moveList.add(new PromotionImpl(this, getField(), targetField,
							new Knight(this.getColor(), targetField, this.getChessBoard(), false)));
					moveList.add(new PromotionImpl(this, getField(), targetField,
							new Bishop(this.getColor(), targetField, this.getChessBoard(), false)));
				}
			}
			if (getChessBoard().getField(originFile, originRank - 1).getPiece() == null) {
				Field targetField = getChessBoard().getField(originFile, originRank - 1);
				moveList.add(new PromotionImpl(this, getField(), targetField,
						new Queen(this.getColor(), targetField, this.getChessBoard(), false)));
				moveList.add(new PromotionImpl(this, getField(), targetField,
						new Rook(this.getColor(), targetField, this.getChessBoard(), false)));
				moveList.add(new PromotionImpl(this, getField(), targetField,
						new Knight(this.getColor(), targetField, this.getChessBoard(), false)));
				moveList.add(new PromotionImpl(this, getField(), targetField,
						new Bishop(this.getColor(), targetField, this.getChessBoard(), false)));
			}
		}
		return moveList;
	}

	/**
	 * Adds the castling moves for black.
	 * @param originFile the origin file
	 * @param originRank the origin rank
	 * @return the result of the operation
	 */
	private List<Move> addCastlingMovesForBlack(int originFile, int originRank) {
		// Pawns cannot perform castling moves; returning an empty list
		return new ArrayList<>();
	}

	/**
	 * Adds the en passant moves for black.
	 * @param originFile the origin file
	 * @param originRank the origin rank
	 * @return the result of the operation
	 */
	private List<Move> addEnPassantMovesForBlack(int originFile, int originRank) {
		List<Move> moveList = new ArrayList<>();
		if (originRank == 4) {
			Move previousMove = this.getMoveList().get(getMoveList().size() - 1);
			Field originPrevMove = previousMove.getSource();
			Field targetPrevMove = previousMove.getTarget();
			Piece piecePrevMove = previousMove.getPiece();
			if (originFile > 1) {
				Field enPassantField = getChessBoard().getField(originFile - 1, originRank - 1);
				boolean condition = piecePrevMove.getType().equals(PieceType.PAWN)
						&& targetPrevMove.getRank() == originRank && targetPrevMove.getFile() == originFile - 1
						&& originPrevMove.getRank() == 2;
				if (condition) {
					Move move = new EnPassantImpl(this, getField(), enPassantField, (Pawn) piecePrevMove);
					moveList.add(move);
				}
			}
			if (originFile < 8) {
				Field enPassantField = getChessBoard().getField(originFile + 1, originRank - 1);
				boolean condition = piecePrevMove.getType().equals(PieceType.PAWN)
						&& targetPrevMove.getRank() == originRank && targetPrevMove.getFile() == originFile + 1
						&& originPrevMove.getRank() == 2;
				if (condition) {
					Move move = new EnPassantImpl(this, getField(), enPassantField, (Pawn) piecePrevMove);
					moveList.add(move);
				}
			}
		}
		return moveList;
	}

	/**
	 * Returns the type.
	 * @return the type
	 */
	@Override
	public PieceType getType() {
		return PieceType.PAWN;
	}
}
