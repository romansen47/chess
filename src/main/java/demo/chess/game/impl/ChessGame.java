package demo.chess.game.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import demo.chess.admin.Admin;
import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.Color;
import demo.chess.definitions.clocks.impl.ChessClock;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.players.BlackPlayer;
import demo.chess.definitions.players.Player;
import demo.chess.definitions.players.WhitePlayer;
import demo.chess.definitions.states.State;
import demo.chess.game.HalfmoveClock;
import demo.chess.game.MoveTiming;
import demo.chess.game.PositionIdentity;
import demo.chess.game.TerminalPositionEvaluator;
import demo.chess.notation.PgnNotation;

/** Live chess game implementation. */
public class ChessGame extends ChessGameTemplate {

    private Admin admin;
    final int timeForEachPlayer;
    int incrementForWhite;
    int incrementForBlack;
    private volatile Color timedOutColor;
    protected List<String> sanMoveList = new ArrayList<>();
    private final HalfmoveClock halfmoveClock = new HalfmoveClock();
    private final List<PositionIdentity> positionHistory = new ArrayList<>();
    private final Map<Integer, MoveTiming> moveTimings = new LinkedHashMap<>();
    private long whiteElapsedAtLastMoveMillis;
    private long blackElapsedAtLastMoveMillis;

    public ChessGame(
            Board chessBoard,
            WhitePlayer whitePlayer,
            BlackPlayer blackPlayer,
            MoveList moveList,
            Admin chessAdmin,
            int timeForEachPlayer) {
        this(chessBoard, whitePlayer, blackPlayer, moveList, chessAdmin,
                timeForEachPlayer, ChessStartingPosition.STANDARD);
    }

    public ChessGame(
            Board chessBoard,
            WhitePlayer whitePlayer,
            BlackPlayer blackPlayer,
            MoveList moveList,
            Admin chessAdmin,
            int timeForEachPlayer,
            ChessStartingPosition startingPosition) {
        super(chessBoard, whitePlayer, blackPlayer, moveList, startingPosition);
        this.setAdmin(chessAdmin);
        this.timeForEachPlayer = timeForEachPlayer;
        configureClock(getWhitePlayer(), Color.WHITE);
        configureClock(getBlackPlayer(), Color.BLACK);
        positionHistory.add(PositionIdentity.from(this));
    }

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    @Override
    public synchronized void resetClocks() {
        stopClock(getWhitePlayer());
        stopClock(getBlackPlayer());

        getWhitePlayer().setChessClock(new ChessClock());
        getBlackPlayer().setChessClock(new ChessClock());
        configureClock(getWhitePlayer(), Color.WHITE);
        configureClock(getBlackPlayer(), Color.BLACK);
        setIncrementForWhite(incrementForWhite);
        setIncrementForBlack(incrementForBlack);

        moveTimings.clear();
        whiteElapsedAtLastMoveMillis = 0L;
        blackElapsedAtLastMoveMillis = 0L;
        timedOutColor = null;
    }

    private void stopClock(Player player) {
        if (player != null && player.getChessClock() != null) {
            player.getChessClock().stop();
        }
    }

    @Override
    public Color getTimedOutColor() {
        return timedOutColor;
    }

    @Override
    public Map<Integer, MoveTiming> getMoveTimings() {
        return Map.copyOf(moveTimings);
    }

    private void configureClock(Player player, Color color) {
        player.setupClock(timeForEachPlayer, 0, () -> loseOnTime(color));
    }

    private synchronized void loseOnTime(Color color) {
        if (color == null || getState() != null) return;
        timedOutColor = color;
        setState(State.LOST_ON_TIME);
    }

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

    @Override
    public void switchPlayer() {
        if (!getPlayer().getChessClock().isRunning()) getPlayer().getChessClock().start();
        getPlayer().getChessClock().addIncrement();
        if (getMoveList().size() == 79 || getMoveList().size() == 80) {
            getPlayer().getChessClock().addAdditionalTime(
                    TimeUnit.SECONDS.toMillis(getPlayer().getAdditionalTime()));
        }
        if (getPlayer().getChessClock().isRunning()) getPlayer().getChessClock().suspend();
        super.switchPlayer();
        if (!getPlayer().getChessClock().isStarted()) getPlayer().getChessClock().start();
        else getPlayer().getChessClock().resume();
    }

    protected boolean checkForGameEnd() throws NoMoveFoundException, IOException {
        boolean gameEnd = false;
        if (TerminalPositionEvaluator.hasInsufficientMatingMaterial(this)) {
            setState(State.DRAW_BY_INSUFFICIENT_MATERIAL);
            return true;
        }
        if (getPlayer().getValidMoves(this).isEmpty()) {
            getPlayer().resignOrStaleMate(this);
            return true;
        }
        if (getState() == null && checkFor50MovesRule()) {
            return true;
        }
        if (getState() == null && checkForThreefoldRepetition(0)) {
            return true;
        }
        return gameEnd;
    }

    protected boolean checkFor50MovesRule() {
        if (!halfmoveClock.isFiftyMoveRuleReached()) {
            return false;
        }
        setState(State.DRAW_BY_50_MOVES_RULE);
        return true;
    }

    protected boolean checkForThreefoldRepetition(int movesBeforeRule) {
        if (positionHistory.isEmpty()) {
            return false;
        }
        PositionIdentity current = positionHistory.get(positionHistory.size() - 1);
        int startIndex = Math.max(0, Math.min(movesBeforeRule, positionHistory.size() - 1));
        long occurrences = positionHistory.subList(startIndex, positionHistory.size()).stream()
                .filter(current::equals)
                .count();
        if (occurrences < 3) {
            return false;
        }
        setState(State.DRAW_BY_THREEFOLD_REPETITION);
        return true;
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
        this.incrementForWhite = Math.max(0, incrementForWhite);
        getWhitePlayer().getChessClock().setIncrementMillis(
                TimeUnit.SECONDS.toMillis(this.incrementForWhite));
    }

    @Override
    public int getIncrementForBlack() {
        return incrementForBlack;
    }

    @Override
    public void setIncrementForBlack(int incrementForBlack) {
        this.incrementForBlack = Math.max(0, incrementForBlack);
        getBlackPlayer().getChessClock().setIncrementMillis(
                TimeUnit.SECONDS.toMillis(this.incrementForBlack));
    }

    @Override
    public List<String> getSanMoveList() {
        return sanMoveList;
    }

    @Override
    public void setSanMoveList(List<String> sanMoveList) {
        this.sanMoveList = sanMoveList;
    }

    @Override
    public synchronized void apply(Move move) throws NoMoveFoundException, IOException {
        Color expiredColor = expiredClockColor();
        if (expiredColor != null) {
            loseOnTime(expiredColor);
            return;
        }
        Player movingPlayer = getPlayer();
        sanMoveList.add(getShortAlgebraicNotatedMove(move));
        halfmoveClock.record(move);
        super.apply(move);
        recordMoveTiming(movingPlayer);
        positionHistory.add(PositionIdentity.from(this));
        checkForGameEnd();
    }

    private void recordMoveTiming(Player movingPlayer) {
        if (movingPlayer == null || movingPlayer.getChessClock() == null) return;

        long cumulativeElapsedMillis =
                movingPlayer.getChessClock().getElapsedThinkingTimeMillis();
        long previousElapsedMillis = movingPlayer.getColor() == Color.WHITE
                ? whiteElapsedAtLastMoveMillis
                : blackElapsedAtLastMoveMillis;
        long elapsedMoveMillis = Math.max(
                0L,
                cumulativeElapsedMillis - previousElapsedMillis);

        if (movingPlayer.getColor() == Color.WHITE) {
            whiteElapsedAtLastMoveMillis = cumulativeElapsedMillis;
        } else {
            blackElapsedAtLastMoveMillis = cumulativeElapsedMillis;
        }

        moveTimings.put(
                getMoveList().size(),
                new MoveTiming(
                        movingPlayer.getChessClock().getRemainingTimeMillis(),
                        elapsedMoveMillis));
    }

    public String getShortAlgebraicNotatedMove(Move moveToExecute)
            throws NoMoveFoundException, IOException {
        return PgnNotation.toDisplayNotation(this, moveToExecute);
    }
}
