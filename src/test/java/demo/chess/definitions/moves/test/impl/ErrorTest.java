package demo.chess.definitions.moves.test.impl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import demo.chess.definitions.moves.test.TestClassAbstract;

public class ErrorTest extends TestClassAbstract {

	Exception e;

	/**
	 * Returns the path.
	 * @return the path
	 */
	@Override
	public String getPath() {
		return "src/test/resources/testfiles/error.demo";
	}

	/**
	 * Performs the prepare for each operation.
	 */
	@Override
	@Before
	public void prepareForEach() throws Exception {
		setChessGame(getAdmin().chessGame(10));
		try {
			loader.loadGame(getPath(), getChessGame());
		} catch (Exception ex) {
			this.e = ex;
		}
	}

	/**
	 * Performs the test error operation.
	 */
	@Test
	public void testError() {
		boolean correctExceptionThrown = e != null;
		Assert.assertTrue(correctExceptionThrown);
	}

}
