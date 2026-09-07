package demo.chess.load;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class GameLoaderPgnGamesTest {

    @Test
    public void splitsSingleCompleteGame() throws Exception {
        GameLoader loader = new GameLoader();
        String pgn = """
                [Event "One"]
                [White "White"]
                [Black "Black"]
                [Result "1-0"]

                1. e4 e5 2. Nf3 Nc6 1-0
                """;

        List<String> games = loader.splitPgnGames(pgn);

        assertEquals(1, games.size());
        assertEquals(List.of("e2e4", "e7e5", "g1f3", "b8c6"), loader.parsePgnMoveList(games.get(0)));
    }

    @Test
    public void splitsTwoTaggedGames() throws Exception {
        GameLoader loader = new GameLoader();
        String pgn = """
                [Event "One"]
                [White "A"]
                [Black "B"]
                [Result "1-0"]

                1. e4 e5 1-0

                [Event "Two"]
                [White "C"]
                [Black "D"]
                [Result "0-1"]

                1. d4 d5 0-1
                """;

        List<String> games = loader.splitPgnGames(pgn);

        assertEquals(2, games.size());
        assertEquals(List.of("e2e4", "e7e5"), loader.parsePgnMoveList(games.get(0)));
        assertEquals(List.of("d2d4", "d7d5"), loader.parsePgnMoveList(games.get(1)));
    }

    @Test
    public void ignoresResultsInTagsCommentsAndVariations() {
        GameLoader loader = new GameLoader();
        String pgn = """
                [Event "One"]
                [Result "1-0"]

                1. e4 {comment containing 0-1} e5 (1... c5 1/2-1/2) 2. Nf3 *
                """;

        assertEquals(1, loader.splitPgnGames(pgn).size());
    }

    @Test
    public void acceptsIncompleteGameWithoutResult() throws Exception {
        GameLoader loader = new GameLoader();
        String pgn = """
                [Event "Incomplete"]
                [White "A"]
                [Black "B"]

                1. e4 e5 2. Nf3 Nc6
                """;

        List<String> games = loader.splitPgnGames(pgn);

        assertEquals(1, games.size());
        assertEquals(List.of("e2e4", "e7e5", "g1f3", "b8c6"), loader.parsePgnMoveList(games.get(0)));
    }

    @Test
    public void returnsNoGameForBlankOrTagOnlyContent() {
        GameLoader loader = new GameLoader();

        assertTrue(loader.splitPgnGames("   \n").isEmpty());
        assertTrue(loader.splitPgnGames("[Event \"Only Tags\"]\n[Result \"*\"]\n").isEmpty());
    }

    @Test
    public void splitsTaglessGamesAfterResultOnFollowingLine() throws Exception {
        GameLoader loader = new GameLoader();
        String pgn = """
                1. e4 e5 1-0
                1. d4 d5 0-1
                """;

        List<String> games = loader.splitPgnGames(pgn);

        assertEquals(2, games.size());
        assertEquals(List.of("e2e4", "e7e5"), loader.parsePgnMoveList(games.get(0)));
        assertEquals(List.of("d2d4", "d7d5"), loader.parsePgnMoveList(games.get(1)));
    }
}
