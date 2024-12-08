package demo.chess.definitions.engines.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.tuple.Pair;

import demo.chess.definitions.Color;
import demo.chess.definitions.engines.EngineConfig;
import demo.chess.definitions.engines.EvaluationEngine;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;

public class EvaluationUciEngine extends ConsoleUciEngine implements EvaluationEngine {

	String bestMove;
	private Map<String, List<Pair<Pair<Double, Integer>, String>>> cachedBestLines = new HashMap<>();
	private String lastPositionHash = "";
	private Thread evaluationThread;
	String path;

	public EvaluationUciEngine(String path) throws Exception {
		super(path);
		logger.info("Creating new evaluation engine: {}", path);
		this.path = path;
	}

	@Override
	public void clearChachedLines() {
		getCachedBestLines().clear();
	}

	@Override
	public synchronized List<Pair<Pair<Double, Integer>, String>> getBestLines(Game chessGame, EngineConfig config)
			throws IOException, InterruptedException, ExecutionException {
		if (chessGame.getState() != null) {
			return new ArrayList<>();
		}
		String movelist = chessGame.getMoveList().toString();
		List<Pair<Pair<Double, Integer>, String>> cachedLines = getCachedBestLines().get(movelist);
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

	protected List<Pair<Pair<Double, Integer>, String>> parseBestLines(Color color, List<String> bestLines, EngineConfig config) {
	    Map<Integer, Pair<Pair<Double, Integer>, String>> multipvLines = new HashMap<>();
	    int requiredDepth = config.getDepth();
	    int maxVariants = config.getMultiPV();

	    // Erstelle eine Liste nur mit Zeilen der maximalen Tiefe
	    int maxDepth = bestLines.stream()
	            .mapToInt(line -> Integer.parseInt(line.split("depth ")[1].split(" ")[0]))
	            .max()
	            .orElse(0);

	    for (String chessLine : bestLines) {
	        if (chessLine.contains("info") && chessLine.contains("depth")) {
	            int currentDepth = Integer.parseInt(chessLine.split("depth ")[1].split(" ")[0]);

	            // Ignoriere Zeilen mit geringerer Tiefe
	            if (currentDepth >= maxDepth && currentDepth >= requiredDepth) {
	                double parsedValue = 0;

	                if (chessLine.contains("mate")) {
	                    parsedValue = Integer.signum(Integer.parseInt(chessLine.split("mate")[1].split(" ")[1])) * 99d;
	                } else if (chessLine.contains("cp")) {
	                    parsedValue = Double.parseDouble(chessLine.split("cp")[1].split(" ")[1]) / 100.0;
	                }

	                if (chessLine.contains("multipv")) {
	                    int multipv = Integer.parseInt(chessLine.split("multipv ")[1].split(" ")[0]);

	                    if (!multipvLines.containsKey(multipv)) {
	                        if (chessLine.contains("pv")) {
	                            String uciEngineLine = chessLine.split(" pv ")[1];
	                            double factor = color.equals(Color.BLACK) ? -1 : 1;
	                            multipvLines.put(multipv, Pair.of(Pair.of(factor * parsedValue, currentDepth), uciEngineLine));
	                        }
	                    }
	                }
	            }
	        }
	    }

	    List<Pair<Pair<Double, Integer>, String>> sortedLines = new ArrayList<>(multipvLines.values());
	    sortLinesByColor(color, sortedLines);
//	    sortedLines.sort((pair1, pair2) -> Double.compare(pair2.getLeft().getLeft(), pair1.getLeft().getLeft()));

	    if (sortedLines.size() > maxVariants) {
	        sortedLines = sortedLines.subList(0, maxVariants);
	    }

	    return sortedLines;
	}

	public synchronized void startEvaluationEngine(Game chessGame, String moveListAsString, EngineConfig config) throws IOException {
	    if (evaluationThread != null) {
	        stopEvaluation();
	    }
	    if (chessGame.getState() != null) {
	        logger.info("Game is decided. Not starting new infinite analysis...");
	        return;
	    }

//	    List<Move> movelist = new ArrayList<>(chessGame.getMoveList());
//	    Color color = movelist.size() % 2 == 0 ? Color.WHITE : Color.BLACK;
	    List<Move> moveList = chessGame.getMoveList();
	    logger.info("{} is starting new infinite analysis for move list {}", this, moveList);

	    if (evaluationThread != null && !evaluationThread.isInterrupted()) {
	    	evaluationThread.interrupt();
	    }
	    
	    writer.close();
	    reader.close();
	    
	    evaluationThread = new Thread(() -> {
	        try {
	            this.uciEngineProcess.destroy();
	            this.uciEngineProcess = new ProcessBuilder(path).start();
	            writer = new PrintWriter(new OutputStreamWriter(uciEngineProcess.getOutputStream()), true);
	            reader = new BufferedReader(new InputStreamReader(uciEngineProcess.getInputStream()));

	            StringBuilder command = new StringBuilder();
	            for (Move move : moveList) {
	                command.append(move.toString()).append(" ");
	            }

	            StringBuilder evaluationCommand = new StringBuilder(
	                    "stop\n" + getCommandLineOptions(command, config).toString());
	            getWriter().println(evaluationCommand.toString());
	            getWriter().flush();

	            List<String> bestLines = new ArrayList<>();
	            int currentMaxDepth = 10;
	            String line;
	            while ((line = reader.readLine()) != null) {
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
	                            List<Pair<Pair<Double, Integer>, String>> newLines = parseBestLines(color, bestLines, config);

	                            synchronized (getCachedBestLines()) {
	                                getCachedBestLines().put(moveListAsString, newLines);
	                            }

	                            bestLines.clear(); // Leere die Liste nach Verarbeitung
	                        }
	                    }
	                }
	            }
	            reader.close();
	        } catch (IOException e) {
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

	protected List<Pair<Pair<Double, Integer> , String>> sortLinesByColor(Color color, List<Pair<Pair<Double, Integer>, String>> moves) {
		List<Pair<Pair<Double, Integer>, String>> tmpLines = new ArrayList<>(moves);
		if (color.equals(Color.WHITE)) {
			tmpLines.sort((pair1, pair2) -> Double.compare(pair2.getLeft().getLeft(), pair1.getLeft().getLeft()));
		} else {
			tmpLines.sort((pair1, pair2) -> Double.compare(pair1.getLeft().getLeft(), pair2.getLeft().getLeft()));
		}
		return tmpLines;
	}

	/**
	 * @return the cachedBestLines
	 */
	@Override
	public Map<String, List<Pair<Pair<Double, Integer>, String>>> getCachedBestLines() {
		return cachedBestLines;
	}
}
