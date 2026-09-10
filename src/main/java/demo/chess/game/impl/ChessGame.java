package demo.chess.game.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import demo.chess.admin.Admin;
import demo.chess.definitions.Color;
import demo.chess.definitions.PieceType;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.pieces.Piece;
import demo.chess.definitions.players.BlackPlayer;
import demo.chess.definitions.players.Player;
import demo.chess.definitions.players.WhitePlayer;
import demo.chess.definitions.states.State;
import demo.chess.notation.PgnNotation;

/**
 * The ChessGame class implements the core functionality for applying different
 * types of chess moves.
 */
public class ChessGame extends ChessGameTemplate {

	private Admin admin;

	final int timeForEachPlayer;

	int incrementForWhite;

	int incrementForBlack;

	protected List<String> sanMoveList = new ArrayList<>();

	protected final List<Long> moveHashes = new ArrayList<>();

	/**
	 * Creates a new ChessGame instance.
	 * @param chessBoard the chess board
	 * @param whitePlayer the white player
	 * @param blackPlayer the black player
	 * @param moveList the move list
	 * @param chessAdmin the chess admin
	 * @param timeForEachPlayer the time for each player
	 */
	public ChessGame(Board chessBoard, WhitePlayer whitePlayer, BlackPlayer blackPlayer, MoveList moveList,
			Admin chessAdmin, int timeForEachPlayer){
		super(chessBoard, whitePlayer, blackPlayer, moveList);
		this.setAdmin(chessAdmin);
		this.timeForEachPlayer = timeForEachPlayer;
		moveHashes.add(0l);
	}

	/**
	 * Returns the admin.
	 * @return the admin
	 */
	public Admin getAdmin() {
		return admin;
	}

	/**
	 * Sets the admin.
	 * @param admin the admin
	 */
	public void setAdmin(Admin admin) {
		this.admin = admin;
	}

	/**
	 * Performs the switch player operation.
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

	/**
	 * Checks the for game end.
	 * @return the result of the operation
	 */
	protected boolean checkForGameEnd() throws NoMoveFoundException, IOException {
		boolean gameEnd = false;
		if (getPlayer().getValidMoves(this).isEmpty()) {
			getPlayer().resignOrStaleMate(this);
			return true;
		}
		if (getState() == null) {
			if (getMoveList().size() > 180) {
				gameEnd = checkFor50MovesRule();
			}
			gameEnd = checkForThreefoldRepetition(0);
		}
		return gameEnd;
	}

	/**
	 * Checks the for50 moves rule.
	 * @return the result of the operation
	 */
	protected boolean checkFor50MovesRule() {
		boolean gameEnd = false;
		List<Move> reducedMoveList = getMoveList().subList(getMoveList().size() - 100, getMoveList().size());
		List<PieceType> piecesMoved = new ArrayList<>();
		reducedMoveList.forEach(move -> piecesMoved.add(move.getPiece().getType()));
		if (!piecesMoved.contains(PieceType.PAWN)) {
			this.setState(State.DRAW_BY_50_MOVES_RULE);
			gameEnd = true;
		}
		return gameEnd;
	}

	/**
	 * Checks the for threefold repetition.
	 * @param movesBeforeRule the moves before rule
	 * @return the result of the operation
	 */
	protected boolean checkForThreefoldRepetition(int movesBeforeRule) {
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

	/**
	 * Returns the time for each player.
	 * @return the time for each player
	 */
	@Override
	public int getTimeForEachPlayer() {
		return timeForEachPlayer;
	}

	/**
	 * Returns the increment for white.
	 * @return the increment for white
	 */
	@Override
	public int getIncrementForWhite() {
		return incrementForWhite;
	}

	/**
	 * Sets the increment for white.
	 * @param incrementForWhite the increment for white
	 */
	@Override
	public void setIncrementForWhite(int incrementForWhite) {
		this.incrementForWhite = incrementForWhite;
	}

	/**
	 * Returns the increment for black.
	 * @return the increment for black
	 */
	@Override
	public int getIncrementForBlack() {
		return incrementForBlack;
	}

	/**
	 * Sets the increment for black.
	 * @param incrementForBlack the increment for black
	 */
	@Override
	public void setIncrementForBlack(int incrementForBlack) {
		this.incrementForBlack = incrementForBlack;
	}

	/**
	 * Returns the san move list.
	 * @return the san move list
	 */
	@Override
	public List<String> getSanMoveList() {
		return sanMoveList;
	}

	/**
	 * Sets the san move list.
	 * @param sanMoveList the san move list
	 */
	@Override
	public void setSanMoveList(List<String> sanMoveList) {
		this.sanMoveList = sanMoveList;
	}

	/**
	 * Performs the apply operation.
	 * @param move the move
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

	/**
	 * Returns whether this object has the h of.
	 * @param piece the piece
	 * @return true when the condition is satisfied; otherwise false
	 */
	protected long hashOf(Piece piece) {
		final long primeBiggerThanProductOfAll = 11;
		final long color = piece.getColor().equals(Color.WHITE) ? 1 : 2;
		return (long) (color + primeBiggerThanProductOfAll * piece.getType().hash()
				+ Math.pow(primeBiggerThanProductOfAll, 2) * (piece.getField().getFile())
				+ Math.pow(primeBiggerThanProductOfAll, 3) * (piece.getField().getRank()));
	}

	/**
	 * Performs the position hash operation.
	 * @return the result of the operation
	 */
	protected Long positionHash() {
		long hash = 1;
		for (Piece piece : getWhitePlayer().getPieces()) {
			hash *= hashOf(piece);
		}
		for (Piece piece : getBlackPlayer().getPieces()) {
			hash *= hashOf(piece);
		}
		return hash * getWhitePlayer().getPieces().size() * getBlackPlayer().getPieces().size();
	}

	/**
	 * Returns the short algebraic notation used by the live move list.
	 *
	 * <p>All notation rules are owned by {@link PgnNotation}; ChessGame only
	 * delegates to the canonical formatter.</p>
	 *
	 * @param moveToExecute the move to execute
	 * @return display SAN for the move
	 */
	public String getShortAlgebraicNotatedMove(Move moveToExecute) throws NoMoveFoundException, IOException {
		return PgnNotation.toDisplayNotation(this, moveToExecute);
	}
}
