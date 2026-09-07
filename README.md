# Chess Core

`chess` is the domain and engine-integration core of the Chess Analysis Tool. It contains the chess model and rules that are shared by the API and database modules; it is not the user-facing application by itself.

## Responsibilities

The module provides the core game model, legal move generation and application, chess notation support, PGN loading and saving, simulations, clock/game-state foundations, and UCI engine integration. UCI executables are inspected through a real UCI handshake so their identity and supported options can be discovered at runtime.

PGN support includes parsing tags and movetext as well as splitting multi-game PGN sources for higher-level import workflows. The normal current-game workflow is deliberately separate from bulk database import.

## Engines

The engine abstraction is based on the UCI protocol and is intended to support engines such as Stockfish and Leela Chess Zero (Lc0), rather than hard-coding the application to one engine implementation. Engine executables are external software and are not bundled with this module.

Some engines need additional files next to the executable. Lc0, for example, normally needs a compatible neural-network weights file and, depending on the distribution and platform, may also require runtime libraries such as DLL files. Keep the files belonging to an engine distribution together unless that distribution explicitly documents another layout.

## Build

This module is a Maven JAR project. Its compiler target is Java 17. In the complete application it is normally built from the parent `chess-project` Maven reactor rather than in isolation.

## Role in the project

The primary product is an analysis tool. Playing a normal game, including playing against an engine, is a supporting capability that provides positions and games for analysis rather than the sole purpose of the project.
