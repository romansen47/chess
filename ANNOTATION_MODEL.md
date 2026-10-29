# Move annotation model

This document describes the domain decisions in
`demo.chess.analysis.annotation`. Engine execution and persistence belong to
the API module; the core classifier is deterministic and does not start an
engine.

## Decision order

The order is intentional. A later positive rule may never hide an objective
mistake found by the deepest available analysis.

1. Build one `MoveAssessmentContext` from the position, played move and
   `DeepAnalysisResult`. MultiPV lines retain the engine's original order.
2. Measure objective move quality with the deepest final lines and the
   resulting-position evaluation.
3. Return `??` at 25 percentage points of winning-chance loss or `?` at 10.
4. Select a stable human decision point. Searches no deeper than 20 use their
   final result. Deeper searches require stability between depths 16 and 20.
5. Detect mechanically natural moves. The current routine reasons are the only
   legal move, a safe retreat after a newly created attack, and an equal-value
   immediate recapture.
6. Return `!!` for a sound material sacrifice or genuine deep discovery. A
   routine move suppresses deep-discovery brilliance, but not a real material
   sacrifice.
7. Otherwise return `!` when the played move is the stable best move, clearly
   exceeds the second choice and was not already trivial in the early search.
8. Return no annotation when none of these rules applies.

## Objective safety and human stability

The two concepts deliberately use different evidence:

- **Objective safety** uses the deepest available analysis. It may veto a
  positive annotation when a move is genuinely refuted.
- **Human stability** uses the depth-16-to-20 window. Once the move is stable
  there, a later rank fluctuation alone does not redefine how difficult the
  move was for a human to find.

This separation prevents both false praise for unsound moves and unstable
annotations caused only by engine depths beyond realistic human calculation.

## Shared domain components

| Component | Responsibility |
| --- | --- |
| `MoveAssessmentContext` | Validated facts shared by all rules |
| `EvaluationScoring` | Mover-oriented scores, winning chance, regret and gaps |
| `SearchTimeline` | Ordered and filtered depth snapshots |
| `HumanAnnotationHorizon` | Stable human decision point |
| `RoutineMoveDetector` | Natural-move reason, not annotation policy |
| `ExtraordinaryMoveDetector` | Combines deep discovery and real sacrifice |
| `OnlyMoveDetector` | Rejects choices that were already obvious early |
| `MoveAnnotationPolicy` | All classification thresholds |

## Regression safety

Changes to this package must preserve the focused classifier tests and the
golden historical-game tests. In particular, coverage must retain:

- authoritative Leela/MultiPV ordering;
- no positive mark for an attacked-piece retreat;
- no positive mark for an equal rook recapture;
- stability of a positive annotation after depth 20;
- a deeper objective refutation overriding that stability;
- historical classifications for Kramnik-Leko, Nezhmetdinov-Chernikov and
  Byrne-Fischer.

Threshold changes are product decisions and must not be mixed into structural
refactoring commits.
