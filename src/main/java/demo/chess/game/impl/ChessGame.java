package demo.chess.game.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;

import demo.chess.admin.Admin;
import demo.chess.definitions.Color;
import demo.chess.definitions.PieceType;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Castling;
import demo.chess.definitions.moves.EnPassant;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.moves.Promotion;
import demo.chess.definitions.pieces.Piece;
import demo.chess.definitions.players.BlackPlayer;
import demo.chess.definitions.players.Player;
import demo.chess.definitions.players.WhitePlayer;
import demo.chess.definitions.states.State;
import demo.chess.game.Game;

/**
 * The ChessGame class implements the core functionality for applying different
 * types of chess moves.
 */
public class ChessGame extends ChessGameTemplate {

	private Admin admin;

	final int timeForEachPlayer;

	int incrementForWhite;

	int incrementForBlack;

	private List<String> sanMoveList = new ArrayList<>();

	private final List<Long> moveHashes = new ArrayList<>();

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
	public ChessGame(Board chessBoard, WhitePlayer whitePlayer, BlackPlayer blackPlayer, MoveList moveList,
			Admin chessAdmin, int timeForEachPlayer) throws Exception {
		super(chessBoard, whitePlayer, blackPlayer, moveList);
		this.setAdmin(chessAdmin);
		this.timeForEachPlayer = timeForEachPlayer;
		moveHashes.add(0l);
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
		if (!getPlayer().getChessClock().isRunning()) {
			getPlayer().getChessClock().start();
		}
		getPlayer().getChessClock().addIncrement();
		if (getMoveList().size() == 79 || getMoveList().size() == 80) {
			getPlayer().getChessClock().addAdditionalTime(getPlayer().getAdditionalTime());
		}
		if (getPlayer().getChessClock().isRunning()) {
			getPlayer().getChessClock().suspend();
		}
		super.switchPlayer();
		if (!getPlayer().getChessClock().isStarted()) {
			getPlayer().getChessClock().start();
		} else {
			getPlayer().getChessClock().resume();
		}
	}


	private boolean checkForGameEnd() throws NoMoveFoundException, IOException {
		boolean gameEnd = false;
		if (getPlayer().getValidMoves(this).isEmpty()) {
			getPlayer().resignOrStaleMate(this);
			return true;
		}
		if (getState() == null) {
			if (getMoveList().size() > 130) {
				gameEnd = checkFor50MovesRule();
			}
			gameEnd = checkForThreefoldRepetition(0);
		}
		return gameEnd;
	}

	private boolean checkFor50MovesRule() {
		boolean gameEnd = false;
		List<Move> reducedMoveList = getMoveList().subList(getMoveList().size() - 50, getMoveList().size());
		List<PieceType> piecesMoved = new ArrayList<>();
		reducedMoveList.forEach(move -> piecesMoved.add(move.getPiece().getType()));
		if (!piecesMoved.contains(PieceType.PAWN)) {
			this.setState(State.DRAW_BY_50_MOVES_RULE);
			gameEnd = true;
		}
		return gameEnd;
	}

	private boolean checkForThreefoldRepetition(int movesBeforeRule) {
		boolean gameEnd = false;
		List<Long> reducedMoveList = moveHashes.subList(movesBeforeRule, getMoveList().size());
		for (Long hash : reducedMoveList) {
			int count = 0;
			for (Long otherHash : moveHashes) {
				if (otherHash.equals(hash)) {
					count++;
				}
			}
			if (count > 2) {
				setState(State.DRAW_BY_THREEFOLD_REPETITION);
				gameEnd = true;
				continue;
			}
		}
		return gameEnd;
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

	@Override
	public List<String> getSanMoveList() {
		return sanMoveList;
	}

	@Override
	public void setSanMoveList(List<String> sanMoveList) {
		this.sanMoveList = sanMoveList;
	}

	/**
	 * Applies the given move to the chess game and computes actual uciEngine
	 * evaluation.
	 *
	 * @param move the move to apply
	 * @throws IOException
	 * @throws NoMoveFoundException
	 */
	@Override
	public void apply(Move move) throws NoMoveFoundException, IOException {
		sanMoveList.add(getShortAlgebraicNotatedMove(move));
		Player opponent = getPlayer().getColor().equals(Color.WHITE) ? this.getBlackPlayer() : this.getWhitePlayer();
		if ((getPlayer().getChessClock().getTime(TimeUnit.MILLISECONDS) / 1000 > getTimeForEachPlayer())
				|| (opponent.getChessClock().getTime(TimeUnit.MILLISECONDS) / 1000 > getTimeForEachPlayer())) {
			setState(State.LOST_ON_TIME);
			return;
		}
		super.apply(move);
		moveHashes.add(positionHash());
		checkForGameEnd();
	}

	private long hashOf(Piece piece) {
		final long primeBiggerThanProductOfAll = 11;
		final long color = piece.getColor().equals(Color.WHITE) ? 1 : 2;
		return (long) (color  	+ primeBiggerThanProductOfAll * piece.getType().hash()
						+ Math.pow(primeBiggerThanProductOfAll, 2) * (piece.getField().getFile())
						+ Math.pow(primeBiggerThanProductOfAll, 3) * (piece.getField().getRank()));
	}

	private Long positionHash() {
		long hash = 1;
		for (Piece piece : getWhitePlayer().getPieces()) {
			hash *= hashOf(piece);
		}
		for (Piece piece : getBlackPlayer().getPieces()) {
			hash *= hashOf(piece);
		}
		return hash * getWhitePlayer().getPieces().size() * getBlackPlayer().getPieces().size();
	}

	public String getUnicodeSymbol(String s, Color color) {
			switch (color) {
			case WHITE:
				switch (s.toLowerCase()) {
				case "k":
					return "♔";
				case "q":
					return "♕";
				case "r":
					return "♖";
				case "b":
					return "♗";
				case "n":
					return "♘";
				}
			case BLACK:
				switch (s.toLowerCase()) {
				case "k":
					return "♚";
				case "q":
					return "♛";
				case "r":
					return "♜";
				case "b":
					return "♝";
				case "n":
					return "♞";
			}
			default:
				return "";
			}
	}

	public String getShortAlgebraicNotatedMove(Move moveToExecute) throws NoMoveFoundException, IOException {
		Game simulation = getAdmin().simulation();
		for (Move move : getMoveList()) {
			simulation.apply(simulation.getPlayer().getMoveInSimulation(simulation, move));
		}
		String convertedMove = "";
		Field originalSource = moveToExecute.getSource();
		Field originalTarget = moveToExecute.getTarget();
		Field source = simulation.getChessBoard().getField(originalSource.getFile(), originalSource.getRank());
		Field target = simulation.getChessBoard().getField(originalTarget.getFile(), originalTarget.getRank());
		Move moveInSimulation = simulation.getPlayer().getMoveInSimulation(simulation, moveToExecute);
		String pieceToString = getUnicodeSymbol(getPiecePrefix(source.getPiece()),source.getPiece().getColor());
		String sourceFieldToString = "";
		if (moveInSimulation.getPiece().getType().equals(PieceType.PAWN) && target.getPiece() != null) {
			sourceFieldToString = moveInSimulation.getPiece().getField().toString().substring(0, 1);
		}
		String hits = target.getPiece() == null || moveToExecute instanceof Castling ? "" : "x";
		String targetFieldToString = target.toString();
		String postFix = moveInSimulation instanceof EnPassant ? " e.p." : "";

		if (moveInSimulation instanceof EnPassant) {
			sourceFieldToString = moveInSimulation.getPiece().getField().toString().substring(0, 1);
			hits = "x";
		}
		List<Move> validMoves = simulation.getPlayer().getValidMoves(this);

		if (moveInSimulation instanceof Castling) {
			pieceToString = "";
			sourceFieldToString = "";
			targetFieldToString = "";
			postFix = "0-0-0";
			if (source.getFile() > 4) {
				postFix = "0-0";
			}
		} else if (moveInSimulation instanceof Promotion) {
			targetFieldToString = target.toString();
			List<Promotion> promotions = new ArrayList<>();
			validMoves.forEach(move -> {
				if (move instanceof Promotion && move.getTarget().equals(target)) {
					promotions.add(((Promotion) move));
				}
			});
			if (promotions.size() != 4) {
				sourceFieldToString = source.getName();
			}
			postFix = "=" + getPiecePrefix(((Promotion) moveInSimulation).getPromotedPiece());

		} else {
			List<Piece> possiblePieces = new ArrayList<>();
			validMoves.forEach(move -> {
				if (move.getTarget().equals(target)) {
					possiblePieces.add(move.getPiece());
				}
			});
			if (possiblePieces.size() != 1) {
				List<String> s = new ArrayList<>();
				String relevantType = getPiecePrefix(source.getPiece());
				possiblePieces.forEach(piece -> {
					if (getPiecePrefix(piece).equals(relevantType) && piece != moveInSimulation.getPiece()) {
						s.add(relevantType);
					}
				});
				if (s.size() > 0) {
					if (s.size() > 1) {
						sourceFieldToString = source.toString();
					} else {
						if (s.size() == 1) {
							if (moveInSimulation.getSource().getFile() != possiblePieces.get(0).getField().getFile()) {
								sourceFieldToString = moveInSimulation.getSource().toString().substring(0,1);
							} else {
								sourceFieldToString = moveInSimulation.getSource().toString().substring(1,2);
							}
						}
					}
				}
			}
		}
		convertedMove = pieceToString + sourceFieldToString + hits + targetFieldToString + postFix;
		return convertedMove;
	}

	String getPiecePrefix(Piece piece) {
		if (piece.getType().equals(PieceType.PAWN)) {
			return StringUtils.EMPTY;
		}
		return piece.getType().equals(PieceType.KNIGHT) ? "N" : piece.getType().name().substring(0, 1);
	}
}
