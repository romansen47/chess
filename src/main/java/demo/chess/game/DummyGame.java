package demo.chess.game;

import java.io.IOException;

import demo.chess.definitions.engines.impl.NoMoveFoundException;

public interface DummyGame extends Game {

	boolean checkForGameEnd() throws NoMoveFoundException, IOException;

	boolean checkFor50MovesRule();

	boolean checkForThreefoldRepetition(int movesBeforeRule);
}
