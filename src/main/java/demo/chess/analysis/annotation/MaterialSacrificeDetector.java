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

        boolean activeQualifies =
                investment >= MoveAnnotationPolicy.EXTRAORDINARY_MATERIAL_INVESTMENT;
        boolean offerQualifies = offer != null
                && offer.getValue()
                        >= MoveAnnotationPolicy.EXTRAORDINARY_MATERIAL_INVESTMENT;

        if (activeQualifies
                && (!offerQualifies || investment >= offer.getValue())) {
            return new MaterialSacrificeEvidence(
                    MaterialSacrificeType.ACTIVE_INVESTMENT,
                    investment);
        }

        return offerQualifies ? offer : null;
    }
}
