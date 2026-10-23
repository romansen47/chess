package demo.chess.definitions.engines.uci;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Builds complete, same-depth MultiPV snapshots from a stream of UCI info
 * lines.
 *
 * <p>Live engines are free to interleave depths, repeat one MultiPV rank or
 * finish a newer iteration before every line of the previous one has been
 * observed by the client. A UI update should nevertheless represent one
 * coherent engine snapshot: exactly ranks {@code 1..N} from one depth.</p>
 */
public final class UciMultiPvSnapshotCollector {

    private final int expectedVariants;
    private final Map<Integer, UciInfoLine> linesByVariant = new TreeMap<>();

    private int currentDepth = -1;
    private boolean emittedCurrentDepth;

    public UciMultiPvSnapshotCollector(int expectedVariants) {
        if (expectedVariants < 1) {
            throw new IllegalArgumentException("expectedVariants must be at least 1");
        }
        this.expectedVariants = expectedVariants;
    }

    /**
     * Accept one exact scored PV line.
     *
     * @return a complete snapshot ordered by MultiPV rank, or empty until all
     *         expected ranks for the current depth are available
     */
    public Optional<List<UciInfoLine>> accept(UciInfoLine info) {
        if (info == null) return Optional.empty();
        if (info.multiPv() > expectedVariants) return Optional.empty();

        if (info.depth() < currentDepth) {
            return Optional.empty();
        }

        if (info.depth() > currentDepth) {
            currentDepth = info.depth();
            linesByVariant.clear();
            emittedCurrentDepth = false;
        }

        if (emittedCurrentDepth) {
            return Optional.empty();
        }

        linesByVariant.put(info.multiPv(), info);
        if (!isComplete()) {
            return Optional.empty();
        }

        List<UciInfoLine> snapshot = new ArrayList<>(expectedVariants);
        for (int multiPv = 1; multiPv <= expectedVariants; multiPv++) {
            snapshot.add(linesByVariant.get(multiPv));
        }
        emittedCurrentDepth = true;
        return Optional.of(List.copyOf(snapshot));
    }

    private boolean isComplete() {
        if (linesByVariant.size() < expectedVariants) return false;
        for (int multiPv = 1; multiPv <= expectedVariants; multiPv++) {
            if (!linesByVariant.containsKey(multiPv)) return false;
        }
        return true;
    }
}
