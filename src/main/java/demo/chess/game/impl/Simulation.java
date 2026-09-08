package demo.chess.game.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

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

	/**
	 * Creates a new Simulation instance.
	 * @param chessBoard the chess board
	 * @param whitePlayer the white player
	 * @param blackPlayer the black player
	 * @param moveList the move list
	 */
	public Simulation(Board chessBoard, WhitePlayer whitePlayer, BlackPlayer blackPlayer, MoveList moveList) {
		super(chessBoard, whitePlayer, blackPlayer, moveList);
	}

	/**
	 * Creates the simulation.
	 * @return the result of the operation
	 */
	public static Simulation createSimulation() {
		MoveList moveList = new MoveListImpl();
		return new Simulation(new ChessBoard(), new WhitePlayerImpl(moveList, "Simulation"),
				new BlackPlayerImpl(moveList, "Simulation"), moveList);
	}

	/**
	 * Creates the dummy simulation.
	 * @return the result of the operation
	 */
	public static DummyChessGame createDummySimulation() {
		MoveList moveList = new MoveListImpl();
		return new DummyChessGame(new ChessBoard(), new DummyWhitePlayer(moveList),
				new DummyBlackPlayer(moveList), moveList);
	}
	
	/**
	 * Performs the fork simulation from operation.
	 * @param ml the ml
	 * @return the result of the operation
	 */
	public static Simulation forkSimulationFrom(MoveList ml) throws NoMoveFoundException, IOException {
		Simulation simulation = createSimulation();
		for (Move move : ml) {
			simulation.apply(simulation.getPlayer().getMoveInSimulation(simulation, move));
		}
		return simulation;
	}

	/**
	 * Performs the fork dummy from operation.
	 * @param ml the ml
	 * @return the result of the operation
	 */
	public static DummyChessGame forkDummyFrom(MoveList ml) throws NoMoveFoundException, IOException {
		DummyChessGame fork = createDummySimulation();
		for (Move move : ml) {
			fork.apply(fork.getPlayer().getMoveInSimulation(fork, move));
		}
		return fork;
	}
	
	/**
	 * Returns the time for each player.
	 * @return the time for each player
	 */
	@Override
	public int getTimeForEachPlayer() {
		return 10000;
	}

	/**
	 * Returns the increment for white.
	 * @return the increment for white
	 */
	@Override
	public int getIncrementForWhite() {
		return 0;
	}

	/**
	 * Sets the increment for white.
	 * @param incrementForWhite the increment for white
	 */
	@Override
	public void setIncrementForWhite(int incrementForWhite) {
	}

	/**
	 * Returns the increment for black.
	 * @return the increment for black
	 */
	@Override
	public int getIncrementForBlack() {
		return 0;
	}

	/**
	 * Sets the increment for black.
	 * @param incrementForBlack the increment for black
	 */
	@Override
	public void setIncrementForBlack(int incrementForBlack) {

	}

	/**
	 * Returns the san move list.
	 * @return the san move list
	 */
	@Override
	public List<String> getSanMoveList() {
		return Collections.emptyList();
	}

	/**
	 * Sets the san move list.
	 * @param sanMoveList the san move list
	 */
	@Override
	public void setSanMoveList(List<String> sanMoveList) {
	}

}
