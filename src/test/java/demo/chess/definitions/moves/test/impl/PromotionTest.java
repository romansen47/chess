package demo.chess.definitions.moves.test.impl;

import org.junit.Assert;
import org.junit.Test;

import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.moves.Promotion;
import demo.chess.definitions.moves.test.TestClassAbstract;

public class PromotionTest extends TestClassAbstract {

	/**
	 * Returns the path.
	 * @return the path
	 */
	@Override
	public String getPath() {
		return "src/test/resources/testfiles/promotion_to_rook.demo";
	}

	/**
	 * Performs the test promotion operation.
	 */
	@Test
	public void testPromotion() {
		MoveList moveList = getChessGame().getMoveList();
		Assert.assertTrue(moveList.get(moveList.size() - 1) instanceof Promotion);
	}
}
