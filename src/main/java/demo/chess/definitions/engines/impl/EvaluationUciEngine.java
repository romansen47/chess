package demo.chess.definitions.engines.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import demo.chess.definitions.Color;
import demo.chess.definitions.engines.EngineConfig;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.engines.EvaluationEngine;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;

public class EvaluationUciEngine extends ConsoleUciEngine implements EvaluationEngine {

	String bestMove;
	private Map<String, List<EngineLine>> cachedBestLines = new HashMap<>();
	private String lastPositionHash = "";
	private Thread evaluationThread;

	public EvaluationUciEngine(String path) throws Exception {
		super(path);
		logger.info("Creating new evaluation engine: {}", path);
	}

	@Override
	public void clearChachedLines() {
		getCachedBestLines().clear();
	}

	@Override
	public synchronized List<EngineLine> getBestLines(Game chessGame, EngineConfig config)
			throws IOException, InterruptedException, ExecutionException {
		if (chessGame.getState() != null) {
			return new ArrayList<>();
		}
		String movelist = chessGame.getMoveList().toString();
		List<EngineLine> cachedLines = getCachedBestLines().get(movelist);
		if (cachedLines != null) {
			return getCachedBestLines().get(movelist);
		}

		getCachedBestLines().put(chessGame.getMoveList().toString(), new ArrayList<>());
		startEvaluationEngine(chessGame, movelist, config);
		return getCachedBestLines().get(movelist);
	}

	@Override
	protected StringBuilder getCommandLineOptions(StringBuilder command, EngineConfig config) {
		StringBuilder positionCommand = new StringBuilder();
		if (config.getMultiPV() > 0) {
			positionCommand.append("setoption name MultiPV value " + config.getMultiPV());
		}
		if (config.getThreads() > 0) {
			positionCommand.append("\nsetoption name Threads value " + config.getThreads());
		}
		positionCommand.append("\nposition startpos moves ").append(command.toString());
		positionCommand.append("\ngo infinite ");
		return positionCommand;
	}

	protected boolean isPositionNew(Game chessGame) {
		String currentPositionHash = chessGame.getMoveList().toString();
		if (!currentPositionHash.equals(lastPositionHash)) {
			lastPositionHash = currentPositionHash;
			return true;
		}
		return false;
	}

	protected List<EngineLine> parseBestLines(Color color, List<String> bestLines, EngineConfig config) {
		return parseBestLines(color, bestLines, config, Math.max(0, config.getDepth()));
	}

	/**
	 * Parses only the highest depth reached by a finite analysis.
	 *
	 * We deliberately do not fall back to an older, complete MultiPV depth.
	 * If one variant of the highest depth is unusable (for example a transient
	 * "score mate 0"), that variant is simply omitted from the result.
	 */
	protected List<EngineLine> parseBestLinesAtHighestDepth(
			Color color,
			List<String> bestLines,
			EngineConfig config) {
		int maxDepth = bestLines.stream()
				.filter(line -> line.contains("info") && line.contains("depth") && line.contains(" pv "))
				.mapToInt(line -> Integer.parseInt(line.split("depth ")[1].split(" ")[0]))
				.max()
				.orElse(0);

		if (maxDepth == 0) {
			return new ArrayList<>();
		}

		List<String> highestDepthLines = bestLines.stream()
				.filter(line -> line.contains("depth "))
				.filter(line -> Integer.parseInt(line.split("depth ")[1].split(" ")[0]) == maxDepth)
				.toList();

		return parseBestLines(color, highestDepthLines, config, 0);
	}

	private List<EngineLine> parseBestLines(
			Color color,
			List<String> bestLines,
			EngineConfig config,
			int minimumDepth) {
		int requestedVariants = Math.max(1, config.getMultiPV());
		TreeMap<Integer, Map<Integer, EngineLine>> linesByDepth = new TreeMap<>();

		for (String chessLine : bestLines) {
			if (!chessLine.contains("info") || !chessLine.contains("depth") || !chessLine.contains(" pv ")) {
				continue;
			}

			int currentDepth = Integer.parseInt(chessLine.split("depth ")[1].split(" ")[0]);
			if (currentDepth < minimumDepth) {
				continue;
			}

			int multipv = chessLine.contains("multipv ")
					? Integer.parseInt(chessLine.split("multipv ")[1].split(" ")[0])
					: 1;
			if (multipv < 1 || multipv > requestedVariants) {
				continue;
			}

			Map<Integer, EngineLine> depthLines = linesByDepth.computeIfAbsent(
					currentDepth,
					ignored -> new TreeMap<>());

			double parsedValue;
			Integer mateDistance = null;

			if (chessLine.contains(" score mate ")) {
				int mateScore = Integer.parseInt(chessLine.split(" score mate ")[1].split(" ")[0]);

				// Stockfish 8 can emit "mate 0" for an unfinished MultiPV root
				// score when a search is stopped between variants. Such a value must
				// not replace a real evaluation and must not make this depth look
				// complete.
				if (mateScore == 0) {
					depthLines.remove(multipv);
					logger.debug(
							"Ignoring transient UCI score mate 0 at depth {} multipv {}",
							currentDepth,
							multipv);
					continue;
				}

				parsedValue = Integer.signum(mateScore) * 99d;
				mateDistance = Math.abs(mateScore);
			} else if (chessLine.contains(" score cp ")) {
				parsedValue = Double.parseDouble(chessLine.split(" score cp ")[1].split(" ")[0]) / 100.0;
			} else {
				continue;
			}

			String uciEngineLine = chessLine.split(" pv ", 2)[1];
			double factor = color.equals(Color.BLACK) ? -1 : 1;
			depthLines.put(
					multipv,
					new EngineLine(
							factor * parsedValue,
							currentDepth,
							mateDistance,
							uciEngineLine));
		}

		List<EngineLine> completeLines = selectHighestCompleteDepth(
				linesByDepth,
				requestedVariants);
		if (!completeLines.isEmpty()) {
			return completeLines;
		}

		// A legal position can contain fewer moves than the configured MultiPV
		// value. If no depth ever contained all requested variants, return the
		// largest contiguous MultiPV prefix that was actually completed.
		int largestCompletedVariantCount = 0;
		for (Map<Integer, EngineLine> depthLines : linesByDepth.values()) {
			largestCompletedVariantCount = Math.max(
					largestCompletedVariantCount,
					countContiguousVariants(depthLines, requestedVariants));
		}

		if (largestCompletedVariantCount == 0) {
			return new ArrayList<>();
		}

		return selectHighestCompleteDepth(linesByDepth, largestCompletedVariantCount);
	}

	private List<EngineLine> selectHighestCompleteDepth(
			TreeMap<Integer, Map<Integer, EngineLine>> linesByDepth,
			int expectedVariants) {
		for (Map.Entry<Integer, Map<Integer, EngineLine>> depthEntry : linesByDepth.descendingMap().entrySet()) {
			Map<Integer, EngineLine> depthLines = depthEntry.getValue();
			if (countContiguousVariants(depthLines, expectedVariants) < expectedVariants) {
				continue;
			}

			List<EngineLine> result = new ArrayList<>();
			for (int multipv = 1; multipv <= expectedVariants; multipv++) {
				result.add(depthLines.get(multipv));
			}
			return result;
		}

		return new ArrayList<>();
	}

	private int countContiguousVariants(Map<Integer, EngineLine> depthLines, int maxVariants) {
		int count = 0;
		for (int multipv = 1; multipv <= maxVariants; multipv++) {
			if (!depthLines.containsKey(multipv)) {
				break;
			}
			count++;
		}
		return count;
	}

	public synchronized void startEvaluationEngine(Game chessGame, String moveListAsString, EngineConfig config) throws IOException {
	    if (evaluationThread != null) {
	        stopEvaluation();
	    }
	    if (chessGame.getState() != null) {
	        logger.info("Game is decided. Not starting new infinite analysis...");
	        return;
	    }

	    List<Move> moveList = new ArrayList<>(chessGame.getMoveList());
	    logger.info("{} is starting new infinite analysis for move list {}", this, moveList);

	    if (evaluationThread != null && !evaluationThread.isInterrupted()) {
	    	evaluationThread.interrupt();
	    }
	    
	    evaluationThread = new Thread(() -> {
	        try {
	            restartProcess();
	            final java.io.PrintWriter processWriter = writer;
	            final java.io.BufferedReader processReader = reader;

	            StringBuilder command = new StringBuilder();
	            for (Move move : moveList) {
	                command.append(move.toString()).append(" ");
	            }

	            StringBuilder evaluationCommand = new StringBuilder(
	                    "stop\n" + getCommandLineOptions(command, config).toString());
	            logger.info("Starting new infinite analysis with {} threads", config.getThreads());
	            processWriter.println(evaluationCommand.toString());
	            processWriter.flush();

	            List<String> bestLines = new ArrayList<>();
	            int currentMaxDepth = 10;
	            String line;
	            while ((line = processReader.readLine()) != null) {
	                if (chessGame.getState() != null) {
	                    return;
	                }

	                if (line.contains("info") && line.contains("depth") && !(line.split(" ").length == 3)) {
	                    int depth = Integer.parseInt(line.split("depth ")[1].split(" ")[0]);

	                    // Sammle nur Zeilen mit einer Tiefe >= currentMaxDepth
	                    if (depth >= currentMaxDepth) {
	                        currentMaxDepth = depth; // Aktualisiere die maximale Tiefe
	                        bestLines.add(line);

	                        // Verarbeite die Zeilen, wenn alle Varianten gesammelt wurden
	                        if (config.getMultiPV() == 1 || bestLines.stream().filter(l -> l.contains("multipv")).count() >= config.getMultiPV()) {
	                            Color color = moveList.size() % 2 == 0 ? Color.WHITE : Color.BLACK;
	                            List<EngineLine> newLines = parseBestLines(color, bestLines, config);

	                            synchronized (getCachedBestLines()) {
	                                getCachedBestLines().put(moveListAsString, newLines);
	                            }

	                            bestLines.clear(); // Leere die Liste nach Verarbeitung
	                        }
	                    }
	                }
	            }
	            processReader.close();
	        } catch (Exception e) {
	            logger.debug("Caught IOException since reader is not ready");
	        }
	    });

	    try {
	        evaluationThread.start();
	    } catch (NullPointerException np) {
	        logger.debug("Thread was cancelled...");
	    }
	}

	@Override
	public void stopEvaluation() {
		logger.info("{} stopping actual infinite analysis", this);
		if (evaluationThread != null && evaluationThread.isAlive()) {
			getWriter().println("stop");
			getWriter().flush();
			evaluationThread.interrupt();
		}
	}

	protected List<EngineLine> sortLinesByColor(Color color, List<EngineLine> moves) {
		List<EngineLine> tmpLines = new ArrayList<>(moves);
		if (color.equals(Color.WHITE)) {
			tmpLines.sort((line1, line2) -> Double.compare(line2.getEvaluation(), line1.getEvaluation()));
		} else {
			tmpLines.sort((line1, line2) -> Double.compare(line1.getEvaluation(), line2.getEvaluation()));
		}
		return tmpLines;
	}

	/**
	 * @return the cachedBestLines
	 */
	@Override
	public Map<String, List<EngineLine>> getCachedBestLines() {
		return cachedBestLines;
	}
}
