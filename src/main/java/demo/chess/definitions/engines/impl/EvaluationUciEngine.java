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
import demo.chess.definitions.engines.EvaluationEngine;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;
import demo.chess.game.Game;

public class EvaluationUciEngine extends ConsoleUciEngine implements EvaluationEngine {

	String bestMove;
	final String name;
    private Map<String, List<Pair<Double, String>>> cachedBestLines = new HashMap<>();
    private String lastPositionHash = "";
    private Thread evaluationThread;
    String path;

    public EvaluationUciEngine(String path, String name) throws Exception {
        super(path);
        this.path = path;
        this.name = name;
    }

    @Override
    public List<Pair<Double, String>> getBestLines(Game chessGame) throws IOException, InterruptedException, ExecutionException {
    	if (chessGame.getState() != null) {
    		return new ArrayList<>();
    	}
        String movelist = chessGame.getMoveList().toString();
        List<Pair<Double, String>> cachedLines = cachedBestLines.get(movelist);
    	if (cachedLines != null) {
    		return cachedBestLines.get(movelist);
        }

    	cachedBestLines.put(chessGame.getMoveList().toString(), new ArrayList<>());
    	logger.info("starting new infinite calculation for move list " + movelist);
        startEvaluationEngine(chessGame, movelist);
        return cachedBestLines.get(movelist);
    }

    @Override
    protected StringBuilder getCommandLineOptions(StringBuilder command) {
        StringBuilder positionCommand = new StringBuilder();
        positionCommand.append("position startpos moves ").append(command.toString());
        positionCommand.append("\ngo infinite ");
        return positionCommand;
    }


    // Methode zum Prüfen, ob ein neuer Zug gemacht wurde
    protected boolean isPositionNew(Game chessGame) {
        String currentPositionHash = chessGame.getMoveList().toString();
        if (!currentPositionHash.equals(lastPositionHash)) {
            lastPositionHash = currentPositionHash;
            return true;
        }
        return false;
    } 

    protected List<Pair<Double, String>> parseBestLines(Color color, List<String> bestLines) {
        List<Pair<Double, String>> moves = new ArrayList<>();
        int requiredDepth = getDepth(); // Der Wert der Mindesttiefe wird von der Methode getDepth() geholt

        for (String chessLine : bestLines) {
            if (chessLine.startsWith("info depth")) {
                int currentDepth = Integer.parseInt(chessLine.split("depth ")[1].split(" ")[0]);

                if (currentDepth >= requiredDepth) {
                    double parsedValue = 0;

                    if (chessLine.contains("mate")) {
                        parsedValue = Integer.signum(Integer.parseInt(chessLine.split("mate")[1].split(" ")[1])) * 99d;
                    } else if (chessLine.contains("cp")) {
                        parsedValue = Double.parseDouble(chessLine.split("cp")[1].split(" ")[1]) / 100.0;
                    }

                    if (chessLine.contains("pv")) {
                        String uciEngineLine = chessLine.split(" pv ")[1];
                        double factor = color.equals(Color.BLACK) ? -1 : 1;
                        moves.add(Pair.of(factor * parsedValue, uciEngineLine));
                    }
                }
            }
        }

        return sortLinesByColor(color, moves);
    }

    public void startEvaluationEngine(Game chessGame, String moveListAsString) throws IOException {
        if (evaluationThread != null) {
            stopInfiniteEvaluation();
        }
        if (chessGame.getState() != null) {
        	return;
        }
        
        List<Move> moveList = chessGame.getMoveList();
        
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

                // Unendliche Analyse starten
                StringBuilder evaluationCommand = new StringBuilder("stop\n" + getCommandLineOptions(command).toString());
                writer.flush();
                writer.println(evaluationCommand.toString());
                writer.flush();
                String line;
                List<String> bestLines = new ArrayList<>();
                while ((line = reader.readLine()) != null) {
                	if (chessGame.getState() != null) {
                    	return;
                    }
                    if (line.startsWith("bestmove")) {
                    	bestMove = line;
                        break;
                    }
                    if (line.startsWith("info depth")) {
                        bestLines.add(line);
                    	Color color = moveList.size() % 2 == 0 ? Color.WHITE : Color.BLACK;
                    	List<Pair<Double, String>> newLines = parseBestLines(color, bestLines);
                    	List<Pair<Double, String>> cached = cachedBestLines.get(moveListAsString);
                    	if (cached != null && !cached.isEmpty()) {
                    		for (Pair<Double,String> pair:cached) {
                    			boolean contained = false;
                    			for (Pair<Double,String> newPair:newLines) {
                    				if (newPair.getRight().startsWith(pair.getRight())) {
                    					contained = true;
                    				}
                    			}
                    			if (!contained) {
                    				newLines.add(pair);
                    			}
                    		}
                        	newLines.addAll(cached);
                    	}
                        cachedBestLines.put(moveListAsString, newLines);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        evaluationThread.start();
    }
    
    @Override
    public void stopInfiniteEvaluation() {
    	if (evaluationThread.isAlive()) {
    		evaluationThread.interrupt();
    	}
    }

    // Sortiert die Zuglinien basierend auf der Spielerfarbe
    protected List<Pair<Double, String>> sortLinesByColor(Color color, List<Pair<Double, String>> lines) {
        List<Pair<Double, String>> tmpLines = new ArrayList<>(lines);
        if (color.equals(Color.WHITE)) {
            // Sortiere für Weiß: positive Bewertungen höher
            tmpLines.sort((pair1, pair2) -> Double.compare(pair2.getLeft(), pair1.getLeft()));
        } else {
            // Sortiere für Schwarz: negative Bewertungen höher
            tmpLines.sort((pair1, pair2) -> Double.compare(pair1.getLeft(), pair2.getLeft()));
        }
        return tmpLines;
    }
}
