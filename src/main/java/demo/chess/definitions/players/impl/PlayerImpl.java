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
import demo.chess.game.impl.ChessGame;

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

	/**
	 * Constructs a PlayerImpl instance with the specified color and move list.
	 *
	 * @param color    the color of the player
	 * @param moveList the move list associated with the player
	 * @param string
	 */
	public PlayerImpl(Color color, MoveList moveList, String string) {
		this.pieces = new ArrayList<>();
		this.color = color;
		this.moveList = moveList;
		name = string;
		this.chessClock = new ChessClock();
	}

	@Override
	public void setupClock(int timeForEachPlayer, int incrementForWhite, Runnable runnable) {
		this.chessClock.setIncrementMillis(incrementForWhite * SECOND_IN_MILLIS);
		this.chessClock.setTargetTimeMillis(timeForEachPlayer * SECOND_IN_MILLIS);
		this.chessClock.setTimeUpAction(runnable);
	}

	@Override
	public List<Move> getSimpleMoves() {
		List<Move> possibleValidMoves = new ArrayList<>();
		for (Piece piece : getPieces()) {
			possibleValidMoves.addAll(piece.getSimpleUnvalidatedMoves());
		}
		return possibleValidMoves;
	}

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
	 * Validates a castling move.
	 *
	 * @param chessGame the game context
	 * @param move      the move to validate
	 * @return true if the castling move is valid, false otherwise
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

		List<Field> listOfFieldsBetweenKingAndRook = new ArrayList<>();
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
	 * Simulates the chess game to check for validity of the moves. In detail, this
	 * creates a new chess game where the move is applied and looks if king would be
	 * in check then.
	 *
	 * @param chessGame the game context
	 * @param move      the move to simulate
	 * @return true if the move is valid, false otherwise
	 * @throws IOException
	 * @throws NoMoveFoundException
	 */
	private boolean simulate(Game chessGame, Move move) throws NoMoveFoundException, IOException {
		if (move instanceof Castling && !validateCastling(chessGame, move)) {
			return false;
		}
		Game simulation = ((ChessGame) chessGame).getAdmin().simulation();
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
	 * Gets the corresponding move in the simulation.
	 *
	 * @param simulation the game simulation
	 * @param m          the move to process in the simulation
	 * @return the corresponding move in the simulation
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

	@Override
	public void reset() {
		pieces.clear();
	}

	@Override
	public Piece getKing() {
		return king;
	}

	@Override
	public void setKing(Piece king) {
		this.king = king;
	}

	@Override
	public MoveList getMoveList() {
		return moveList;
	}

	/**
	 * Sets the move list for the player.
	 *
	 * @param moveList the move list to set
	 */
	public void setMoveList(MoveList moveList) {
		this.moveList = moveList;
	}

	/**
	 * @return the stopWatch
	 */
	@Override
	public ChessClock getChessClock() {
		return chessClock;
	}

	/**
	 * @param stopWatch the stopWatch to set
	 */
	@Override
	public void setChessClock(ChessClock chessClock) {
		this.chessClock = chessClock;
	}

	@Override
	public Color getColor() {
		return color;
	}

	@Override
	public List<Piece> getPieces() {
		return pieces;
	}

	@Override
	public String toString() {
		return " PLAYER: created for " + name;
	}

}
