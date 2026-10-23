package demo.chess.definitions.engines.uci;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

public class UciMultiPvSnapshotCollectorTest {

    @Test
    public void emitsOnlyAfterAllRanksOfOneDepthArePresent() {
        UciMultiPvSnapshotCollector collector = new UciMultiPvSnapshotCollector(3);

        assertTrue(collector.accept(cp(20, 2, 15)).isEmpty());
        assertTrue(collector.accept(cp(20, 1, 21)).isEmpty());

        Optional<List<UciInfoLine>> snapshot = collector.accept(cp(20, 3, 9));

        assertTrue(snapshot.isPresent());
        assertEquals(List.of(1, 2, 3),
                snapshot.get().stream().map(UciInfoLine::multiPv).toList());
        assertEquals(List.of(21, 15, 9),
                snapshot.get().stream().map(UciInfoLine::centipawnScore).toList());
    }

    @Test
    public void newerDepthDiscardsIncompleteOlderDepth() {
        UciMultiPvSnapshotCollector collector = new UciMultiPvSnapshotCollector(2);

        assertTrue(collector.accept(cp(12, 1, 18)).isEmpty());
        assertTrue(collector.accept(cp(13, 2, 11)).isEmpty());

        Optional<List<UciInfoLine>> snapshot = collector.accept(cp(13, 1, 16));

        assertTrue(snapshot.isPresent());
        assertEquals(13, snapshot.get().get(0).depth());
        assertEquals(13, snapshot.get().get(1).depth());
    }

    @Test
    public void duplicateRankReplacesEarlierValueBeforeSnapshotCompletes() {
        UciMultiPvSnapshotCollector collector = new UciMultiPvSnapshotCollector(2);

        assertTrue(collector.accept(cp(18, 1, 10)).isEmpty());
        assertTrue(collector.accept(cp(18, 1, 14)).isEmpty());

        Optional<List<UciInfoLine>> snapshot = collector.accept(cp(18, 2, 8));

        assertTrue(snapshot.isPresent());
        assertEquals(Integer.valueOf(14), snapshot.get().get(0).centipawnScore());
    }

    @Test
    public void ignoresLateOlderDepthAndDoesNotReemitSameDepth() {
        UciMultiPvSnapshotCollector collector = new UciMultiPvSnapshotCollector(1);

        assertTrue(collector.accept(cp(9, 1, 5)).isPresent());
        assertTrue(collector.accept(cp(8, 1, 99)).isEmpty());
        assertTrue(collector.accept(cp(9, 1, 7)).isEmpty());
        assertTrue(collector.accept(cp(10, 1, 8)).isPresent());
    }

    private static UciInfoLine cp(int depth, int multiPv, int score) {
        return new UciInfoLine(depth, multiPv, score, null, "e2e4 e7e5");
    }
}
