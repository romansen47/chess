package demo.chess.load;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.moves.impl.MoveListImpl;
import demo.chess.save.GameSaver;

public class Chess960PgnTest {

    @Test
    public void readsStartingPositionFromFen() throws Exception {
        String pgn = "[Variant \"Chess960\"]\n"
                + "[SetUp \"1\"]\n"
                + "[FEN \"bbqnnrkr/pppppppp/8/8/8/8/PPPPPPPP/BBQNNRKR w HFhf - 0 1\"]\n\n*\n";
        assertEquals(0, new GameLoader().parsePgnStartingPosition(pgn).getId());
    }

    @Test
    public void writesChess960SetupTags() throws Exception {
        MoveListImpl moves = new MoveListImpl();
        moves.setStartingPosition(ChessStartingPosition.of(0));
        String pgn = new GameSaver().toPgn(moves, Map.of("Result", "*"));
        assertTrue(pgn.contains("[Variant \"Chess960\"]"));
        assertTrue(pgn.contains("[SetUp \"1\"]"));
        assertTrue(pgn.contains("[FEN \"bbqnnrkr/pppppppp/8/8/8/8/PPPPPPPP/BBQNNRKR w HFhf - 0 1\"]"));
    }
}
