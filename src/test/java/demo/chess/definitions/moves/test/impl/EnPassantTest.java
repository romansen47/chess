package demo.chess.definitions.moves.test.impl;

import org.junit.Assert;
import org.junit.Test;

import demo.chess.definitions.moves.EnPassant;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.moves.test.TestClassAbstract;

public class EnPassantTest extends TestClassAbstract {

	@Override
	public String getPath() {
		return "src/test/resources/testfiles/enpassant.demo";
	}

	@Test
	public void testEnPassant() {
		MoveList moveList = getChessGame().getMoveList();
		Assert.assertTrue(moveList.get(moveList.size() - 1) instanceof EnPassant);
	}

}
