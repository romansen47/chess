package demo.chess.definitions.moves.test.impl;

import demo.chess.definitions.moves.test.TestClassAbstract;

public class CastlingNotPossibleTest extends TestClassAbstract{

	@Override
	public String getPath() {
		return "src/test/resources/testfiles/castlingNotPossible.demo";
	}

//	@Test
//	public void testCastling() throws Exception {
//		String mesage = "queens side castling not possible";
//		getLogger().info(mesage);
//		throw new Exception(mesage);
//	}

}