package demo.chess.definitions.moves.test.impl;

import org.junit.Assert;
import org.junit.Test;

import demo.chess.definitions.moves.Castling;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.moves.test.TestClassAbstract;

public class CastlingTest extends TestClassAbstract {

	@Override
	public String getPath() {
		return "src/test/resources/testfiles/castling.demo";
	}

	@Test
	public void testCastling() {
		MoveList moveList = getChessGame().getMoveList();
		Assert.assertTrue(moveList.get(moveList.size() - 1) instanceof Castling);
	}

}
