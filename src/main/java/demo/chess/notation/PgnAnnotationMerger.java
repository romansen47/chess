package demo.chess.notation;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.game.impl.Simulation;
import demo.chess.load.GameLoader;
import demo.chess.save.GameSaver;

/** Enriches an already identified identical game; existing annotation fields win. */
public final class PgnAnnotationMerger {
    private final PgnAnnotationParser parser = new PgnAnnotationParser();
    private final GameLoader loader = new GameLoader();
    private final GameSaver saver = new GameSaver();

    public String fillMissing(String existingPgn, String incomingPgn) throws IOException {
        if (existingPgn == null) return incomingPgn;
        if (existingPgn.equals(incomingPgn)) return existingPgn;
        try {
            Map<Integer, PgnMoveAnnotation> existing = parser.parse(existingPgn);
            Map<Integer, PgnMoveAnnotation> merged = new LinkedHashMap<>(existing);
            parser.parse(incomingPgn).forEach((ply, incoming) -> merged.merge(ply, incoming, this::fillMissing));
            if (merged.equals(existing)) return existingPgn;
            Simulation game = Simulation.createSimulation(loader.parsePgnStartingPosition(existingPgn));
            loader.loadGame(loader.parsePgnMoveList(existingPgn), game);
            return saver.toPgn(game.getMoveList(), loader.parsePgnTags(existingPgn), merged);
        } catch (NoMoveFoundException e) {
            throw new IOException("Could not merge PGN annotations", e);
        }
    }

    private PgnMoveAnnotation fillMissing(PgnMoveAnnotation existing, PgnMoveAnnotation incoming) {
        return new PgnMoveAnnotation(
                existing.nag() != null ? existing.nag() : incoming.nag(),
                existing.comment() != null ? existing.comment() : incoming.comment(),
                existing.evaluation() != null ? existing.evaluation() : incoming.evaluation(),
                !existing.variations().isEmpty() ? existing.variations() : incoming.variations(),
                existing.clockMillis() != null ? existing.clockMillis() : incoming.clockMillis(),
                existing.elapsedMoveMillis() != null ? existing.elapsedMoveMillis() : incoming.elapsedMoveMillis());
    }
}
