package demo.chess.analysis.annotation;

final class ObjectiveMoveQualityEvaluator {

    ObjectiveMoveQuality evaluate(double winChanceLoss) {
        if (winChanceLoss >= MoveAnnotationPolicy.BLUNDER_WIN_PERCENT_LOSS) {
            return ObjectiveMoveQuality.BLUNDER;
        }
        if (winChanceLoss >= MoveAnnotationPolicy.MISTAKE_WIN_PERCENT_LOSS) {
            return ObjectiveMoveQuality.MISTAKE;
        }
        return ObjectiveMoveQuality.ACCEPTABLE;
    }
}
