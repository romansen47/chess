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

	private volatile Color timedOutColor;

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
		configureClock(getWhitePlayer(), Color.WHITE);
		configureClock(getBlackPlayer(), Color.BLACK);
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
	 * Returns the player that lost on time.
	 *
	 * @return timed-out color, or {@code null} when the game did not end on time
	 */
	@Override
	public Color getTimedOutColor() {
		return timedOutColor;
	}

	/**
	 * Configures one core-owned chess clock and its timeout action.
	 *
	 * @param player player whose clock is configured
	 * @param color player color used when the clock expires
	 */
	private void configureClock(Player player, Color color) {
		player.setupClock(timeForEachPlayer, 0, () -> loseOnTime(color));
	}

	/**
	 * Ends the game because one player's clock expired.
	 *
	 * @param color player that lost on time
	 */
	private synchronized void loseOnTime(Color color) {
		if (color == null || getState() != null) {
			return;
		}
		timedOutColor = color;
		setState(State.LOST_ON_TIME);
	}

	/**
	 * Returns the color of an expired clock before a move is applied.
	 *
	 * @return expired color or {@code null}
	 */
	private Color expiredClockColor() {
		Player playerToMove = getPlayer();
		if (playerToMove != null
				&& playerToMove.getChessClock() != null
				&& playerToMove.getChessClock().isTimeUp()) {
			return playerToMove.getColor();
		}

		Player opponent = playerToMove != null && playerToMove.getColor() == Color.WHITE
				? getBlackPlayer()
				: getWhitePlayer();
		if (opponent != null
				&& opponent.getChessClock() != null
				&& opponent.getChessClock().isTimeUp()) {
			return opponent.getColor();
		}

		return null;
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
			getPlayer().getChessClock().addAdditionalTime(
					TimeUnit.SECONDS.toMillis(getPlayer().getAdditionalTime()));
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
		this.incrementForWhite = Math.max(0, incrementForWhite);
		getWhitePlayer().getChessClock().setIncrementMillis(
				TimeUnit.SECONDS.toMillis(this.incrementForWhite));
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
		this.incrementForBlack = Math.max(0, incrementForBlack);
		getBlackPlayer().getChessClock().setIncrementMillis(
				TimeUnit.SECONDS.toMillis(this.incrementForBlack));
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
	public synchronized void apply(Move move) throws NoMoveFoundException, IOException {
		Color expiredColor = expiredClockColor();
		if (expiredColor != null) {
			loseOnTime(expiredColor);
			return;
		}

		sanMoveList.add(getShortAlgebraicNotatedMove(move));
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
