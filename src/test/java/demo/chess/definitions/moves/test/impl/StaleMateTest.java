package demo.chess.definitions.moves.test.impl;

import org.junit.Assert;
import org.junit.Test;

import demo.chess.definitions.moves.test.TestClassAbstract;
import demo.chess.definitions.states.State;

public class StaleMateTest extends TestClassAbstract {

	@Override
	public String getPath() {
		return "src/test/resources/testfiles/stalemate.demo";
	}

	@Test
	public void testStaleMate() {
		Assert.assertTrue(
				this.getChessGame().getState() != null && this.getChessGame().getState().equals(State.STALEMATE));
	}

}
