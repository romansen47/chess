package demo.chess.definitions.moves.test.impl;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.test.TestClassAbstract;
import demo.chess.definitions.states.State;

/**
 * Abzugschachmatt
 */
public class MateNotRecognizedTest extends TestClassAbstract{

	@Override
	public String getPath() {
		return "src/test/resources/testfiles/mate3.demo";
	}

	String finalMove = "e7e4";
	Move checkMate;
	@Override
	@Before
	public void prepareForEach() throws Exception {
		setChessGame(getAdmin().chessGame(10));
		loader.loadGame(getPath(), getChessGame());
		for(Move move: getChessGame().getPlayer().getValidMoves(getChessGame())){
			if (move.toString().equals(finalMove)) {
				checkMate = move;
			}
		}
	}

	@Test
	public void testMate() throws NoMoveFoundException, IOException {
		getLogger().debug("Sequence {} completed", getChessGame().getMoveList().toString());
		getChessGame().apply(checkMate);
		State gameState = this.getChessGame().getState();
		// we are mated by retrieving
		Assert.assertTrue(gameState != null && gameState.equals(State.BLACK_MATED));
	}

}
