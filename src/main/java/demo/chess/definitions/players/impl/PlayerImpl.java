package demo.chess.definitions.players.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import demo.chess.definitions.Color;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.clocks.impl.ChessClock;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Castling;
import demo.chess.definitions.moves.EnPassant;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.moves.Promotion;
import demo.chess.definitions.moves.impl.CastlingImpl;
import demo.chess.definitions.moves.impl.ChessMove;
import demo.chess.definitions.moves.impl.EnPassantImpl;
import demo.chess.definitions.moves.impl.PromotionImpl;
import demo.chess.definitions.pieces.Piece;
import demo.chess.definitions.pieces.impl.Bishop;
import demo.chess.definitions.pieces.impl.Knight;
import demo.chess.definitions.pieces.impl.Pawn;
import demo.chess.definitions.pieces.impl.Queen;
import demo.chess.definitions.pieces.impl.Rook;
import demo.chess.definitions.players.Player;
import demo.chess.definitions.states.State;
import demo.chess.game.Game;
import demo.chess.game.impl.Simulation;

/**
 * Abstract class representing a chess player with common functionalities.
 */
public abstract class PlayerImpl implements Player {

	private static final long SECOND_IN_MILLIS = 1000l;

	private final List<Piece> pieces;
	private final Color color;
	private Piece king;
	private MoveList moveList;
	private ChessClock chessClock;
	private final String name;
	private int additionalTime;

	/**
	 * Creates a new PlayerImpl instance.
	 * @param color the color
	 * @param moveList the move list
	 * @param string the string
	 */
	public PlayerImpl(Color color, MoveList moveList, String string) {
		this.pieces = new ArrayList<>();
		this.color = color;
		this.moveList = moveList;
		name = string;
		this.chessClock = new ChessClock();
	}

	/**
	 * Sets the up clock.
	 * @param timeForEachPlayer the time for each player
	 * @param incrementForWhite the increment for white
	 * @param runnable the runnable
	 */
	@Override
	public void setupClock(int timeForEachPlayer, int incrementForWhite, Runnable runnable) {
		this.chessClock.setIncrementMillis(incrementForWhite * SECOND_IN_MILLIS);
		this.chessClock.setTargetTimeMillis(timeForEachPlayer * SECOND_IN_MILLIS);
		this.chessClock.setTimeUpAction(runnable);
	}

	/**
	 * Returns the simple moves.
	 * @return the simple moves
	 */
	@Override
	public List<Move> getSimpleMoves() {
		List<Move> possibleValidMoves = new ArrayList<>();
		for (Piece piece : getPieces()) {
			possibleValidMoves.addAll(piece.getSimpleUnvalidatedMoves());
		}
		return possibleValidMoves;
	}

	/**
	 * Returns the valid moves.
	 * @param chessGame the chess game
	 * @return the valid moves
	 */
	@Override
	public List<Move> getValidMoves(Game chessGame) throws NoMoveFoundException, IOException {
		List<Move> possibleUnvalidetMoves = new ArrayList<>();
		List<Move> possibleValidMoves = new ArrayList<>();
		for (Piece piece : getPieces()) {
			possibleUnvalidetMoves.addAll(piece.getPossibleUnvalidatedMoves());
		}
		for (Move move : possibleUnvalidetMoves) {
			{
				if (simulate(chessGame, move)) {
					possibleValidMoves.add(move);
				}
			}
		}
		return possibleValidMoves;
	}

	/**
	 * Performs the resign or stale mate operation.
	 * @param chessGame the chess game
	 */
	@Override
	public void resignOrStaleMate(Game chessGame) {
		Player opponent = getColor().equals(Color.WHITE) ? chessGame.getBlackPlayer() : chessGame.getWhitePlayer();
		List<Field> listOfAttackedFields = opponent.getSimpleMoves().stream().map(Move::getTarget).distinct()
				.collect(Collectors.toList());
		if (!listOfAttackedFields.contains(king.getField())) {
			chessGame.setState(State.STALEMATE);
		} else {
			if (opponent.getColor().equals(Color.WHITE)) {
				chessGame.setState(State.BLACK_MATED);
			} else {
				chessGame.setState(State.WHITE_MATED);
			}
		}
		if (chessGame.getWhitePlayer().getChessClock().isStarted()) {
			chessGame.getWhitePlayer().getChessClock().stop();
		}
		if (chessGame.getBlackPlayer().getChessClock().isStarted()) {
			chessGame.getBlackPlayer().getChessClock().stop();
		}
	}

	/**
	 * Performs the replace by valid move operation.
	 * @param chessGame the chess game
	 * @param move the move
	 * @return the result of the operation
	 */
	@Override
	public Move replaceByValidMove(Game chessGame, Move move) throws NoMoveFoundException, IOException {
		List<Move> validMoves = getValidMoves(chessGame);
		Move answer = null;
		for (Move realMove : validMoves) {
			if (answer == null && realMove.equals(move)) {
				answer = realMove;
			}
		}
		return answer;
	}

	/**
	 * Validates the castling.
	 * @param chessGame the chess game
	 * @param move the move
	 * @return the result of the operation
	 */
	private boolean validateCastling(Game chessGame, Move move) {
		if (!(move instanceof Castling)) {
			return false;
		}
		Castling castling = (Castling) move;
		Rook rook = castling.getRook();

		List<Piece> listOfMovedPieces = chessGame.getMoveList().stream().map(Move::getPiece).distinct()
				.collect(Collectors.toList());

		List<Field> listOfAttackedFields = new ArrayList<>();
		Player opponent = getColor().equals(Color.WHITE) ? chessGame.getBlackPlayer() : chessGame.getWhitePlayer();
		listOfAttackedFields.addAll(
				opponent.getSimpleMoves().stream().map(Move::getTarget).distinct().collect(Collectors.toList()));

		if (listOfAttackedFields.contains(king.getField()) || listOfMovedPieces.contains(king)
				|| listOfMovedPieces.contains(rook)) {
			return false;
		}

		int rank = move.getPiece().getField().getRank();
		int file = move.getPiece().getField().getFile();
		boolean fieldsAreFree = true;
		int rookFile = move.getTarget().getFile() == 1 ? 1 : 8;
		if (rookFile == 1) {
			for (int i = 2; i < file; i++) {
				if (chessGame.getChessBoard().getField(i, rank).getPiece() != null) {
					fieldsAreFree = false;
				}
			}
		} else {
			for (int i = file + 1; i < 7; i++) {
				if (chessGame.getChessBoard().getField(i, rank).getPiece() != null) {
					fieldsAreFree = false;
				}
			}
		}
		if (!fieldsAreFree) {
			return false;
		}
		if (getColor().equals(Color.WHITE)) {
			if (castling.getRook().equals(chessGame.getChessBoard().getField(1, 1).getPiece())) {
				if (listOfAttackedFields.contains(chessGame.getChessBoard().getField(3, 1))
						|| listOfAttackedFields.contains(chessGame.getChessBoard().getField(4, 1))) {
					return false;
				}
			} else if (castling.getRook().equals(chessGame.getChessBoard().getField(8, 1).getPiece())) {
				if (listOfAttackedFields.contains(chessGame.getChessBoard().getField(6, 1))
						|| listOfAttackedFields.contains(chessGame.getChessBoard().getField(7, 1))) {
					return false;
				}
			}
		} else {
			if (castling.getRook().equals(chessGame.getChessBoard().getField(1, 8).getPiece())) {
				if (listOfAttackedFields.contains(chessGame.getChessBoard().getField(3, 8))
						|| listOfAttackedFields.contains(chessGame.getChessBoard().getField(4, 8))) {
					return false;
				}
			} else if (castling.getRook().equals(chessGame.getChessBoard().getField(8, 8).getPiece())) {
				if (listOfAttackedFields.contains(chessGame.getChessBoard().getField(6, 8))
						|| listOfAttackedFields.contains(chessGame.getChessBoard().getField(7, 8))) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Performs the simulate operation.
	 * @param chessGame the chess game
	 * @param move the move
	 * @return the result of the operation
	 */
	protected boolean simulate(Game chessGame, Move move) throws NoMoveFoundException, IOException {
		if (move instanceof Castling && !validateCastling(chessGame, move)) {
			return false;
		}
		Game simulation = Simulation.createSimulation();
		for (Move m : getMoveList()) {
			Move newMove = getMoveInSimulation(simulation, m);
			simulation.apply(newMove);
		}
		Player originalPlayer = simulation.getPlayer();
		simulation.apply(getMoveInSimulation(simulation, move));
		Player otherPlayer = simulation.getPlayer();
		List<Move> simpleMovesOfOtherPlayer = otherPlayer.getSimpleMoves();
		List<Field> fieldsOfiecesPossibleToTakeBySimpleMoves = simpleMovesOfOtherPlayer.stream().map(Move::getTarget)
				.distinct().collect(Collectors.toList());
		return !fieldsOfiecesPossibleToTakeBySimpleMoves.contains(originalPlayer.getKing().getField());
	}

	/**
	 * Returns the move in simulation.
	 * @param simulation the simulation
	 * @param m the m
	 * @return the move in simulation
	 */
	@Override
	public Move getMoveInSimulation(Game simulation, Move m) {
		Board chessBoard = simulation.getChessBoard();
		Field source = chessBoard.getField(m.getSource().getFile(), m.getSource().getRank());
		Field target = chessBoard.getField(m.getTarget().getFile(), m.getTarget().getRank());
		Piece piece = source.getPiece();
		if (m instanceof Promotion) {
			Piece promotedPiece = ((Promotion) m).getPromotedPiece();
			Piece simulatedPromotedPiece = null;
			switch (promotedPiece.getType()) {
			case QUEEN:
				simulatedPromotedPiece = new Queen(promotedPiece.getColor(), target, chessBoard, false);
				break;
			case ROOK:
				simulatedPromotedPiece = new Rook(promotedPiece.getColor(), target, chessBoard, false);
				break;
			case KNIGHT:
				simulatedPromotedPiece = new Knight(promotedPiece.getColor(), target, chessBoard, false);
				break;
			case BISHOP:
				simulatedPromotedPiece = new Bishop(promotedPiece.getColor(), target, chessBoard, false);
				break;
			default:
				break;
			}
			return new PromotionImpl(piece, source, target, simulatedPromotedPiece);
		}
		if (m instanceof EnPassant) {
			EnPassant ep = (EnPassant) m;
			Field fieldOfSlayedPawn = chessBoard.getField(ep.getSlayedPiece().getField().getFile(),
					ep.getSlayedPiece().getField().getRank());
			Pawn newPawn = (Pawn) fieldOfSlayedPawn.getPiece();
			return new EnPassantImpl(piece, source, target, newPawn);
		}
		if (m instanceof Castling) {
			return new CastlingImpl(piece, (Rook) target.getPiece());
		}
		return new ChessMove(piece, source, target);
	}

	/**
	 * Performs the reset operation.
	 */
	@Override
	public void reset() {
		pieces.clear();
	}

	/**
	 * Returns the king.
	 * @return the king
	 */
	@Override
	public Piece getKing() {
		return king;
	}

	/**
	 * Sets the king.
	 * @param king the king
	 */
	@Override
	public void setKing(Piece king) {
		this.king = king;
	}

	/**
	 * Returns the move list.
	 * @return the move list
	 */
	@Override
	public MoveList getMoveList() {
		return moveList;
	}

	/**
	 * Sets the move list.
	 * @param moveList the move list
	 */
	public void setMoveList(MoveList moveList) {
		this.moveList = moveList;
	}

	/**
	 * Returns the chess clock.
	 * @return the chess clock
	 */
	@Override
	public ChessClock getChessClock() {
		return chessClock;
	}

	/**
	 * Sets the chess clock.
	 * @param chessClock the chess clock
	 */
	@Override
	public void setChessClock(ChessClock chessClock) {
		this.chessClock = chessClock;
	}

	/**
	 * Returns the color.
	 * @return the color
	 */
	@Override
	public Color getColor() {
		return color;
	}

	/**
	 * Returns the name.
	 * @return the name
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Returns the pieces.
	 * @return the pieces
	 */
	@Override
	public List<Piece> getPieces() {
		return pieces;
	}

	/**
	 * Returns the additional time.
	 * @return the additional time
	 */
	@Override
	public int getAdditionalTime() {
		return additionalTime;
	}

	/**
	 * Sets the additional time.
	 * @param additionalTime the additional time
	 */
	@Override
	public void setAdditionalTime(int additionalTime) {
		this.additionalTime = additionalTime;
	}

	/**
	 * Returns a string representation of this object.
	 * @return the result of the operation
	 */
	@Override
	public String toString() {
		return " PLAYER: created for " + name;
	}

}
