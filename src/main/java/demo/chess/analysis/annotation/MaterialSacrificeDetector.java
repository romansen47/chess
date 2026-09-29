package demo.chess.analysis.annotation;

import demo.chess.definitions.engines.EngineLine;
import demo.chess.game.Game;

/**
 * Combines the two legitimate sacrifice shapes used by brilliant-move
 * detection: investing the moved piece itself and deliberately offering a
 * different piece.
 */
final class MaterialSacrificeDetector {

    private final MaterialInvestmentDetector investmentDetector =
            new MaterialInvestmentDetector();
    private final MaterialOfferDetector offerDetector =
            new MaterialOfferDetector();

    private final MaterialTrajectoryAnalyzer trajectoryAnalyzer = new MaterialTrajectoryAnalyzer();
    private final MaterialCompensationAnalyzer compensationAnalyzer = new MaterialCompensationAnalyzer();

    MaterialSacrificeEvidence find(
            Game rootPosition,
            EngineLine finalPlayed,
            String playedMoveUci,
            boolean whiteMover) {
        double investment = investmentDetector.calculate(
                rootPosition,
                finalPlayed,
                whiteMover,
                MoveAnnotationPolicy.EXTRAORDINARY_MATERIAL_HORIZON_PLIES);

        MaterialSacrificeEvidence offer = offerDetector.find(
                rootPosition,
                playedMoveUci,
                whiteMover);

        MaterialTrajectoryAnalyzer.Trajectory trajectory =
                trajectoryAnalyzer.analyze(rootPosition, finalPlayed, whiteMover);
        if (trajectory != null && !trajectory.moves().get(0).equalsIgnoreCase(playedMoveUci)) {
            trajectory = null;
            investment = 0;
        }
        if (trajectory != null && trajectory.movedPieceCapturePly() != null) {
            investment = trajectory.remainingInvestment(
                    trajectory.movedPieceCapturePly(), investment);
        }
        if (offer != null) {
            double remaining = offer.getValue();
            // Only the PV that actually accepts this offer can refute its
            // material cost. A declined offer has no deficit in that PV.
            if (trajectory != null && trajectory.moves().size() > 1
                    && trajectory.moves().get(1).equalsIgnoreCase(offer.getAcceptanceMoveUci())) {
                remaining = trajectory.remainingInvestment(2, remaining);
            }
            if (remaining < MoveAnnotationPolicy.EXTRAORDINARY_MATERIAL_INVESTMENT
                    || compensationAnalyzer.find(rootPosition, playedMoveUci, offer, whiteMover) != null) {
                offer = null;
            } else {
                offer = new MaterialSacrificeEvidence(
                        offer.getType(), remaining, offer.getAcceptanceMoveUci(), trajectory);
            }
        }

        boolean activeQualifies =
                investment >= MoveAnnotationPolicy.EXTRAORDINARY_MATERIAL_INVESTMENT;
        boolean offerQualifies = offer != null
                && offer.getValue()
                        >= MoveAnnotationPolicy.EXTRAORDINARY_MATERIAL_INVESTMENT;

        if (activeQualifies
                && (!offerQualifies || investment >= offer.getValue())) {
            return new MaterialSacrificeEvidence(
                    MaterialSacrificeType.ACTIVE_INVESTMENT,
                    investment, null, trajectory);
        }

        return offerQualifies ? offer : null;
    }
}
