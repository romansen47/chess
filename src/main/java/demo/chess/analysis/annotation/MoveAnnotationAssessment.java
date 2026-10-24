package demo.chess.analysis.annotation;

/**
 * Full result of move assessment: the user-facing annotation plus optional
 * diagnostic evidence explaining a rejected extraordinary-move candidate.
 */
public record MoveAnnotationAssessment(
        MoveAnnotation annotation,
        MoveAnnotationDiagnostics diagnostics) {
}
