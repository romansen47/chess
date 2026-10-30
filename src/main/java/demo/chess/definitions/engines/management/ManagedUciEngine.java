package demo.chess.definitions.engines.management;

/**
 * Identifies a native UCI adapter registered with the shared process manager.
 *
 * <p>Engine consumers can use this narrow capability without depending on a
 * concrete UCI implementation. Non-native and test engines simply do not
 * implement it.</p>
 */
public interface ManagedUciEngine {

    /** Returns the process-manager identity of this engine instance. */
    String getManagementId();
}
