package demo.chess.save;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;

public class GameSaver {

    public void saveGame(MoveList moveList, String location) throws IOException {
        Files.writeString(Path.of(location), toUci(moveList), StandardCharsets.UTF_8);
    }

    /**
     * Serializes a game as a plain UCI move list, one move per line.
     */
    public String toUci(Iterable<Move> moveList) {
        if (moveList == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (Move move : moveList) {
            if (move == null) {
                continue;
            }
            result.append(move.toString()).append('\n');
        }
        return result.toString();
    }
}
