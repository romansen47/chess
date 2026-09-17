package demo.chess.game.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.board.impl.ChessBoard;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.moves.impl.MoveListImpl;
import demo.chess.definitions.players.BlackPlayer;
import demo.chess.definitions.players.WhitePlayer;
import demo.chess.definitions.players.impl.BlackPlayerImpl;
import demo.chess.definitions.players.impl.DummyBlackPlayer;
import demo.chess.definitions.players.impl.DummyWhitePlayer;
import demo.chess.definitions.players.impl.WhitePlayerImpl;

public class Simulation extends ChessGameTemplate {

    public Simulation(Board chessBoard, WhitePlayer whitePlayer, BlackPlayer blackPlayer, MoveList moveList) {
        super(chessBoard, whitePlayer, blackPlayer, moveList);
    }

    public Simulation(
            Board chessBoard,
            WhitePlayer whitePlayer,
            BlackPlayer blackPlayer,
            MoveList moveList,
            ChessStartingPosition startingPosition) {
        super(chessBoard, whitePlayer, blackPlayer, moveList, startingPosition);
    }

    public static Simulation createSimulation() {
        return createSimulation(ChessStartingPosition.STANDARD);
    }

    public static Simulation createSimulation(ChessStartingPosition startingPosition) {
        MoveList moveList = new MoveListImpl();
        moveList.setStartingPosition(startingPosition);
        return new Simulation(
                new ChessBoard(),
                new WhitePlayerImpl(moveList, "Simulation"),
                new BlackPlayerImpl(moveList, "Simulation"),
                moveList,
                startingPosition);
    }

    public static DummyChessGame createDummySimulation() {
        return createDummySimulation(ChessStartingPosition.STANDARD);
    }

    public static DummyChessGame createDummySimulation(ChessStartingPosition startingPosition) {
        MoveList moveList = new MoveListImpl();
        moveList.setStartingPosition(startingPosition);
        return new DummyChessGame(
                new ChessBoard(),
                new DummyWhitePlayer(moveList),
                new DummyBlackPlayer(moveList),
                moveList,
                startingPosition);
    }

    public static Simulation forkSimulationFrom(MoveList moveList) throws NoMoveFoundException, IOException {
        ChessStartingPosition startingPosition = moveList != null
                ? moveList.getStartingPosition()
                : ChessStartingPosition.STANDARD;
        Simulation simulation = createSimulation(startingPosition);
        if (moveList != null) {
            for (Move move : moveList) {
                simulation.apply(simulation.getPlayer().getMoveInSimulation(simulation, move));
            }
        }
        return simulation;
    }

    public static Simulation forkSimulationFrom(demo.chess.game.Game game)
            throws NoMoveFoundException, IOException {
        return game == null
                ? createSimulation()
                : forkSimulationFrom(game.getMoveList());
    }

    public static DummyChessGame forkDummyFrom(MoveList moveList) throws NoMoveFoundException, IOException {
        ChessStartingPosition startingPosition = moveList != null
                ? moveList.getStartingPosition()
                : ChessStartingPosition.STANDARD;
        DummyChessGame fork = createDummySimulation(startingPosition);
        if (moveList != null) {
            for (Move move : moveList) {
                fork.apply(fork.getPlayer().getMoveInSimulation(fork, move));
            }
        }
        return fork;
    }

    @Override
    public int getTimeForEachPlayer() {
        return 10000;
    }

    @Override
    public int getIncrementForWhite() {
        return 0;
    }

    @Override
    public void setIncrementForWhite(int incrementForWhite) {
    }

    @Override
    public int getIncrementForBlack() {
        return 0;
    }

    @Override
    public void setIncrementForBlack(int incrementForBlack) {
    }

    @Override
    public List<String> getSanMoveList() {
        return Collections.emptyList();
    }

    @Override
    public void setSanMoveList(List<String> sanMoveList) {
    }
}
