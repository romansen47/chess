package demo.chess.analysis.annotation;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.Test;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.impl.Simulation;

public class MaterialTrajectoryAnalyzerTest {
    private final MaterialTrajectoryAnalyzer analyzer = new MaterialTrajectoryAnalyzer();

    @Test
    public void delayedRecoveryBeyondOldHorizonRemovesActiveSacrifice() throws Exception {
        Game root = position("g1f3", "g8f6", "c2c4", "g7g6");
        EngineLine pv = line("b1c3 d7d5 c4d5 f6d5 h2h4 d5c3 h4h5 g6g5 b2c3");
        var trajectory = analyzer.analyze(root, pv, true);
        assertNotNull(trajectory);
        assertEquals(List.of(0d, 0d, 0d, 1d, 0d, 0d, -3d, -3d, -3d, 0d), trajectory.balances());
        assertEquals(Integer.valueOf(6), trajectory.movedPieceCapturePly());
        assertEquals(3d, trajectory.maximumDeficit(), 0.001);
        assertEquals(0d, trajectory.persistentDeficit(), 0.001);
        assertEquals(3d, trajectory.recoveredMaterial(), 0.001);
        assertEquals(Integer.valueOf(9), trajectory.recoveryPly(6));
        assertEquals(3d, new MaterialInvestmentDetector().calculate(root, pv, true, 6), 0.001);
        assertNull(new MaterialSacrificeDetector().find(root, pv, "b1c3", true));
        assertEquals(4, root.getMoveList().size());
    }

    @Test
    public void blackPerspectiveAndInitialMaterialAreRespected() throws Exception {
        Game root = position("e2e4", "g8f6", "b1c3");
        var trajectory = analyzer.analyze(root, line("f6e4 c3e4"), false);
        assertNotNull(trajectory);
        assertArrayEquals(new double[] {0d, 1d, -2d},
                trajectory.balances().stream().mapToDouble(Double::doubleValue).toArray(), 0.001);
        assertEquals(2d, trajectory.persistentDeficit(), 0.001);
        assertNull(trajectory.recoveryPly(2));
        // Two points remain below the established three-point annotation threshold.
        assertNull(new MaterialSacrificeDetector().find(root, line("f6e4 c3e4"), "f6e4", false));

        Game unequal = position("e2e4", "d7d5", "e4d5");
        var unchanged = analyzer.analyze(unequal, line("g8f6"), false);
        assertEquals(List.of(-1d, -1d), unchanged.balances());
        assertEquals(0d, unchanged.maximumDeficit(), 0.001);
    }

    @Test
    public void missingOrIllegalPvDoesNotInventRecovery() {
        Game root = Simulation.createSimulation();
        assertNull(analyzer.analyze(root, null, true));
        assertNull(analyzer.analyze(root, line(""), true));
        assertNull(analyzer.analyze(root, line("e2e4 e7e5 invalid"), true));
        assertEquals(0, root.getMoveList().size());
    }

    @Test
    public void laterUnrelatedLossDoesNotReviveRecoveredInvestment() {
        var trajectory = new MaterialTrajectoryAnalyzer.Trajectory(
                List.of(0d, 0d, -3d, 0d, -5d), List.of("a", "b", "c", "d"), 2);
        assertEquals(0d, trajectory.remainingInvestment(2, 3d), 0.001);
        assertEquals(Integer.valueOf(3), trajectory.recoveryPly(2));
    }

    private EngineLine line(String moves) { return new EngineLine(0, 20, null, moves); }
    private Game position(String... moves) throws Exception {
        Game game = Simulation.createSimulation();
        for (String move : moves) game.apply(LegalMoveResolver.resolveUci(game, move));
        return game;
    }
}
