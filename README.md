# Chess Core

`chess` is the domain and engine-integration core of the Chess Analysis Tool. It contains the chess model and rules shared by the API and database modules; it is not the user-facing application by itself.

## Responsibilities

The module provides:

- the board, pieces, players, clocks and game state;
- legal move generation and move application;
- classical chess and Chess960 starting positions;
- simulation and replay support;
- SAN/PGN notation and PGN loading/saving;
- UCI move encoding and UCI engine process integration;
- common analysis primitives used by higher-level modules.

PGN support includes tags, movetext, annotations and splitting multi-game PGN sources for higher-level import workflows. The normal current-game workflow is deliberately separate from bulk database import.

## Chess960 domain model

Chess960 is modeled as a generalization of chess rather than as a parallel rule engine. Classical chess is Scharnagl position `518` and is available as `ChessStartingPosition.STANDARD`.

`ChessStartingPosition` is an immutable value object for the 960 legal Scharnagl positions. It owns the initial back-rank layout and the original king and rook files. Those files are required for castling rights, FEN and UCI handling. The game stores this starting-position context and propagates it into simulations and move history so a replay can reconstruct the same variant.

The central rule is therefore:

```text
ChessStartingPosition
        |
        +--> Game
        +--> CastlingRights
        +--> MoveList / replay context
        +--> Simulation
        +--> PGN/FEN
        +--> UCI
```

Do not reintroduce fixed assumptions such as `king starts on e1/e8` or `castling rooks start on a/h`. Code that needs initial geometry should ask `ChessStartingPosition` or `CastlingRights`.

## Castling

Chess960 castling has the same final squares as classical castling:

- king-side: king on `g`, rook on `f`;
- queen-side: king on `c`, rook on `d`.

The initial king and rook files vary. Either piece can already occupy its final square, so castling cannot be recognized by a two-square king move.

The model uses:

- `CastlingSide` for the semantic side;
- `CastlingRights` for the original rook files that still retain castling rights;
- `Castling` for the compound move geometry (`king source/target`, `rook source/target`);
- `CastlingValidator` for path and attack constraints;
- `AttackDetector` for attack-map checks used by castling and ordinary king-safety validation.

For compatibility with the historical `Move` contract, a `Castling` move still exposes the original rook square through `Move.getTarget()`. New castling code should use the dedicated castling accessors instead of depending on that compatibility detail.

## Simulations and move history

A move object is tied to the board and piece instances on which it was created. `MoveSimulationMapper` rebuilds moves against a simulation board before replaying them. This keeps promotion, en-passant and Chess960 castling reconstruction out of player-state code.

`MoveList` retains the initial `ChessStartingPosition` as replay metadata. Although the historical type name says "list", it functions as the lightweight move-history object used throughout the existing API. Changing that public contract would affect the API and database modules, so the starting-position metadata is intentionally retained there for compatibility.

## Notation and protocol boundaries

Domain moves and external notation are deliberately separated.

`PgnNotation` handles SAN/PGN semantics. `UciMoveCodec` is the single boundary for UCI move text. Code must not assume that `Move.toString()` is always a UCI move.

This matters for Chess960 castling. A domain castling move can have a king destination such as `g1`, while Chess960 UCI represents the same move as the king source followed by the original rook source. For example, a king already on `g1` castling with a rook on `h1` has:

```text
domain king movement: g1 -> g1
Chess960 UCI:          g1h1
```

`GameSaver.toUci(...)`, legal-move resolution and engine position commands all use the UCI codec so this distinction is preserved.

PGN is the self-describing persistence format. Non-standard Chess960 games are written with `Variant`, `SetUp` and initial `FEN` tags. The lightweight UCI export contains only moves; its consumer must already know the starting position.

## UCI engines

The engine abstraction is based on the UCI protocol and is intended to support engines such as Stockfish and Leela Chess Zero (Lc0), rather than hard-coding the application to one engine implementation. Engine executables are external software and are not bundled with this module.

Before a game is sent to an engine, `ConsoleUciEngine` configures `UCI_Chess960` from the game's starting-position context. Classical position 518 continues to use `position startpos`; non-standard positions use `position fen <initial-fen>` followed by the encoded move sequence.

### Engine capabilities and system-managed options

A successful UCI handshake only proves that an executable speaks UCI. Chess960 support is a separate advertised capability. The core recognizes an engine as Chess960-capable when its handshake contains a `UCI_Chess960` option of type `check`.

`UCI_Chess960` is **system-managed**. It remains part of the engine option schema because it describes a capability, but it is not an ordinary profile value. `UciEngineConfig.toUciSetOptionCommands()` deliberately omits it, and `ConsoleUciEngine` derives its value from the current `Game` immediately before a search. This prevents a persisted default such as `UCI_Chess960=false` from silently switching a running Chess960 engine back to classical mode.

The runtime rules are:

```text
classical game + engine without UCI_Chess960
    -> allowed; no unknown option is sent

Chess960 game + engine advertising UCI_Chess960
    -> UCI_Chess960=true, then FEN + canonical Chess960 UCI moves

Chess960 game + engine without UCI_Chess960
    -> rejected before search
```

The advertised option is the protocol-level capability signal. Higher application layers may additionally perform behavioral smoke tests for specific engine/version/network combinations.

Some engines need additional files next to the executable. Lc0, for example, normally needs a compatible neural-network weights file and, depending on the distribution and platform, may also require runtime libraries such as DLL files. Keep the files belonging to an engine distribution together unless that distribution explicitly documents another layout.

## Design rules

When extending this module:

1. Keep chess rules in the core domain rather than in the API or frontend.
2. Treat position 518 as one `ChessStartingPosition`, not as a separate implementation path unless an external protocol requires it.
3. Keep external notation/protocol details in codecs or adapters; do not overload `toString()` with protocol meaning.
4. Preserve starting-position context whenever moves are replayed, analyzed, saved or sent to an engine.
5. Keep system-managed UCI state out of reusable profile values; game context is authoritative for protocol mode.
6. Do not assume that every UCI engine supports Chess960 merely because its handshake succeeds.
7. Prefer focused rule helpers over growing `PlayerImpl` or `ChessGameTemplate` into general-purpose utility classes.
8. Add Javadoc for public domain types and non-obvious compatibility constraints, and update this README when an architectural boundary changes.

## Build and tests

This module is a Maven JAR project compiled with Java 21. In the complete application it is normally built from the parent `chess-project` Maven reactor rather than in isolation.

The test suite contains regression tests for standard chess as well as dedicated Chess960 coverage for Scharnagl decoding, FEN/PGN, castling geometry, UCI position commands and system-managed engine protocol state. Changes to shared move or simulation code should be verified through the parent reactor because the API, database and frontend consume this module together.

## Role in the project

The primary product is an analysis tool. Playing a normal game, including playing against an engine, is a supporting capability that provides positions and games for analysis rather than the sole purpose of the project.
