package demo.chess.definitions.players.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import demo.chess.definitions.Color;
import demo.chess.definitions.clocks.impl.ChessClock;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Castling;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.pieces.Piece;
import demo.chess.definitions.players.Player;
import demo.chess.definitions.states.State;
import demo.chess.game.AttackDetector;
import demo.chess.game.CastlingValidator;
import demo.chess.game.Game;
import demo.chess.game.MoveSimulationMapper;
import demo.chess.game.TerminalPositionEvaluator;
import demo.chess.game.impl.Simulation;

/**
 * Base implementation of player state and player-level move orchestration.
 *
 * <p>Rule details that are not inherently player state are delegated to focused
 * collaborators: {@link CastlingValidator} validates castling geometry and attack
 * constraints, {@link AttackDetector} provides attack-map checks, and
 * {@link MoveSimulationMapper} rebuilds moves for simulation boards.</p>
 */
public abstract class PlayerImpl extends PlayerSimulationBase implements Player {

    private static final long SECOND_IN_MILLIS = 1000L;

    private final List<Piece> pieces;
    private final Color color;
    private Piece king;
    private MoveList moveList;
    private ChessClock chessClock;
    private final String name;
    private int additionalTime;

    public PlayerImpl(Color color, MoveList moveList, String string) {
        this.pieces = new ArrayList<>();
        this.color = color;
        this.moveList = moveList;
        this.name = string;
        this.chessClock = new ChessClock();
    }

    @Override
    public void setupClock(int timeForEachPlayer, int incrementForWhite, Runnable runnable) {
        chessClock.setIncrementMillis(incrementForWhite * SECOND_IN_MILLIS);
        chessClock.setTargetTimeMillis(timeForEachPlayer * SECOND_IN_MILLIS);
        chessClock.setTimeUpAction(runnable);
    }

    @Override
    public List<Move> getSimpleMoves() {
        List<Move> moves = new ArrayList<>();
        for (Piece piece : pieces) moves.addAll(piece.getSimpleUnvalidatedMoves());
        return moves;
    }

    @Override
    public List<Move> getValidMoves(Game chessGame) throws NoMoveFoundException, IOException {
        List<Move> candidates = new ArrayList<>();
        for (Piece piece : pieces) candidates.addAll(piece.getPossibleUnvalidatedMoves());

        List<Move> legalMoves = new ArrayList<>();
        for (Move move : candidates) {
            if (simulate(chessGame, move)) legalMoves.add(move);
        }
        return legalMoves;
    }

    @Override
    public void resignOrStaleMate(Game chessGame) {
        State terminalState = TerminalPositionEvaluator.determineStateWhenNoLegalMoves(chessGame);
        if (terminalState != null) chessGame.setState(terminalState);
        if (chessGame.getWhitePlayer().getChessClock().isStarted()) chessGame.getWhitePlayer().getChessClock().stop();
        if (chessGame.getBlackPlayer().getChessClock().isStarted()) chessGame.getBlackPlayer().getChessClock().stop();
    }

    @Override
    public Move replaceByValidMove(Game chessGame, Move move) throws NoMoveFoundException, IOException {
        for (Move realMove : getValidMoves(chessGame)) {
            if (realMove.equals(move)) return realMove;
        }
        return null;
    }

    /**
     * Replays the current history in an isolated game, applies the candidate and
     * verifies that the moving side's king is not left under attack.
     */
    @Override
    protected boolean simulate(Game chessGame, Move move) throws NoMoveFoundException, IOException {
        if (move instanceof Castling castling && !CastlingValidator.isLegal(chessGame, this, castling)) {
            return false;
        }

        Game simulation = Simulation.createSimulation(chessGame.getStartingPosition());
        for (Move previous : moveList) {
            Move replayMove = MoveSimulationMapper.map(simulation, previous);
            if (replayMove == null) return false;
            simulation.apply(replayMove);
        }

        Player movingPlayer = simulation.getPlayer();
        Move simulatedMove = MoveSimulationMapper.map(simulation, move);
        if (simulatedMove == null) return false;
        simulation.apply(simulatedMove);

        Player opponent = simulation.getPlayer();
        Field kingField = movingPlayer.getKing().getField();
        return !AttackDetector.isAttacked(kingField, opponent);
    }

    /**
     * Compatibility method required by {@link Player}. The actual translation is
     * centralized in {@link MoveSimulationMapper}.
     */
    @Override
    public Move getMoveInSimulation(Game simulation, Move move) {
        return MoveSimulationMapper.map(simulation, move);
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

    public void setMoveList(MoveList moveList) {
        this.moveList = moveList;
    }

    @Override
    public ChessClock getChessClock() {
        return chessClock;
    }

    @Override
    public void setChessClock(ChessClock chessClock) {
        this.chessClock = chessClock;
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Piece> getPieces() {
        return pieces;
    }

    @Override
    public int getAdditionalTime() {
        return additionalTime;
    }

    @Override
    public void setAdditionalTime(int additionalTime) {
        this.additionalTime = additionalTime;
    }

    @Override
    public String toString() {
        return " PLAYER: created for " + name;
    }
}
