package demo.chess.analysis.annotation;

final class ObjectiveMoveQualityEvaluator {

    ObjectiveMoveQuality evaluate(double loss) {
        if (loss >= MoveAnnotationPolicy.BLUNDER_LOSS) {
            return ObjectiveMoveQuality.BLUNDER;
        }
        if (loss >= MoveAnnotationPolicy.MISTAKE_LOSS) {
            return ObjectiveMoveQuality.MISTAKE;
        }
        return ObjectiveMoveQuality.ACCEPTABLE;
    }
}
