package demo.chess.game.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import demo.chess.admin.Admin;
import demo.chess.definitions.ChessStartingPosition;
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
    protected final List<Long> moveHashes = new ArrayList<>();

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
        moveHashes.add(0L);
    }

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    @Override
    public Color getTimedOutColor() {
        return timedOutColor;
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
        if (getState() == null) {
            if (getMoveList().size() > 180) gameEnd = checkFor50MovesRule();
            gameEnd = checkForThreefoldRepetition(0);
        }
        return gameEnd;
    }

    protected boolean checkFor50MovesRule() {
        boolean gameEnd = false;
        List<Move> reducedMoveList = getMoveList().subList(getMoveList().size() - 100, getMoveList().size());
        List<PieceType> piecesMoved = new ArrayList<>();
        reducedMoveList.forEach(move -> piecesMoved.add(move.getPiece().getType()));
        if (!piecesMoved.contains(PieceType.PAWN)) {
            setState(State.DRAW_BY_50_MOVES_RULE);
            gameEnd = true;
        }
        return gameEnd;
    }

    protected boolean checkForThreefoldRepetition(int movesBeforeRule) {
        boolean gameEnd = false;
        List<Long> reducedMoveList = moveHashes.subList(movesBeforeRule, getMoveList().size());
        for (Long hash : reducedMoveList) {
            int count = 0;
            for (Long otherHash : moveHashes) {
                if (otherHash.equals(hash)) count++;
            }
            if (count > 2) {
                setState(State.DRAW_BY_THREEFOLD_REPETITION);
                gameEnd = true;
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
        sanMoveList.add(getShortAlgebraicNotatedMove(move));
        super.apply(move);
        moveHashes.add(positionHash());
        checkForGameEnd();
    }

    protected long hashOf(Piece piece) {
        final long primeBiggerThanProductOfAll = 11;
        final long color = piece.getColor().equals(Color.WHITE) ? 1 : 2;
        return (long) (color + primeBiggerThanProductOfAll * piece.getType().hash()
                + Math.pow(primeBiggerThanProductOfAll, 2) * piece.getField().getFile()
                + Math.pow(primeBiggerThanProductOfAll, 3) * piece.getField().getRank());
    }

    protected Long positionHash() {
        long hash = 1;
        for (Piece piece : getWhitePlayer().getPieces()) hash *= hashOf(piece);
        for (Piece piece : getBlackPlayer().getPieces()) hash *= hashOf(piece);
        return hash * getWhitePlayer().getPieces().size() * getBlackPlayer().getPieces().size();
    }

    public String getShortAlgebraicNotatedMove(Move moveToExecute)
            throws NoMoveFoundException, IOException {
        return PgnNotation.toDisplayNotation(this, moveToExecute);
    }
}
