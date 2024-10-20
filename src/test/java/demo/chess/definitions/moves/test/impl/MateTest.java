package demo.chess.definitions.moves.test.impl;

import org.junit.Assert;
import org.junit.Test;

import demo.chess.definitions.moves.test.TestClassAbstract;
import demo.chess.definitions.states.State;

public class MateTest extends TestClassAbstract {

	@Override
	public String getPath() {
		return "src/test/resources/testfiles/mate1.demo";
	}

	@Test
	public void testMate() {
		getLogger().debug("Sequence {} completed", getChessGame().getMoveList().toString());
		State gameState = this.getChessGame().getState();
		Assert.assertTrue(gameState != null && gameState.equals(State.BLACK_MATED));
	}

}
