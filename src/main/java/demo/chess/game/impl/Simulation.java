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
import demo.chess.definitions.players.impl.WhitePlayerImpl;
import demo.chess.game.Game;

public class Simulation extends ChessGameTemplate {

	/**
	 * Constructs a ChessGame instance with the given chessboard, white player,
	 * black player, and move list.
	 *
	 * @param chessBoard  the chess board
	 * @param whitePlayer the white player
	 * @param blackPlayer the black player
	 * @param moveList    the list of moves
	 * @param chessAdmin
	 */
	public Simulation(Board chessBoard, WhitePlayer whitePlayer, BlackPlayer blackPlayer, MoveList moveList) {
		super(chessBoard, whitePlayer, blackPlayer, moveList);
	}

	public static Simulation createSimulation() {
		MoveList moveList = new MoveListImpl();
		return new Simulation(new ChessBoard(), new WhitePlayerImpl(moveList, "Simulation"),
				new BlackPlayerImpl(moveList, "Simulation"), moveList);
	}
	
	public static Simulation forkFromn(MoveList ml) throws NoMoveFoundException, IOException {
		MoveList moveList = new MoveListImpl();
		Simulation simulation = new Simulation(new ChessBoard(), new WhitePlayerImpl(moveList, "Simulation"),
				new BlackPlayerImpl(moveList, "Simulation"), moveList);
		for (Move move : ml) {
			simulation.apply(simulation.getPlayer().getMoveInSimulation(simulation, move));
		}
		return simulation;
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
