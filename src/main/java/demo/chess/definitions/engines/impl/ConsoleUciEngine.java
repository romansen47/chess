package demo.chess.definitions.engines.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import demo.chess.definitions.engines.ChessEngine;

public abstract class ConsoleUciEngine implements ChessEngine {

    protected static final Logger logger = LogManager.getLogger(ConsoleUciEngine.class);

    protected Process stockfishProcess;
    protected PrintWriter writer;
    protected BufferedReader reader;

    protected int depth;
    protected int threads;
    protected int hashSize;
    protected int multiPV;
    protected int contempt;
    protected int moveOverhead;
    protected int uciElo;

    public ConsoleUciEngine(String path) throws Exception {
        stockfishProcess = new ProcessBuilder(path).start();
        writer = new PrintWriter(new OutputStreamWriter(stockfishProcess.getOutputStream()), true);
        reader = new BufferedReader(new InputStreamReader(stockfishProcess.getInputStream()));
    }

    @Override
    public void close() {
        writer.println("quit");
        writer.flush();
        writer.close();
        try {
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        stockfishProcess.destroy();
    }

    protected abstract StringBuilder getCommandLineOptions(StringBuilder command);

    @Override
    public int getThreads() {
        return threads;
    }

    @Override
    public void setThreads(Integer threads) {
        this.threads = threads;
    }

    @Override
    public int getHashSize() {
        return hashSize;
    }

    @Override
    public void setHashSize(Integer hashSize) {
        this.hashSize = hashSize;
    }

    @Override
    public int getMultiPV() {
        return multiPV;
    }

    @Override
    public void setMultiPV(Integer multiPV) {
        this.multiPV = multiPV;
    }

    @Override
    public int getMoveOverhead() {
        return moveOverhead;
    }

    @Override
    public void setMoveOverhead(Integer moveOverhead) {
        this.moveOverhead = moveOverhead;
    }

    @Override
    public int getContempt() {
        return contempt;
    }

    @Override
    public void setContempt(Integer contempt) {
        this.contempt = contempt;
    }

    @Override
    public int getUciElo() {
        return uciElo;
    }

    @Override
    public void setUciElo(Integer uciElo) {
        this.uciElo = uciElo;
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public void setDepth(int depth) {
        this.depth = depth;
    }
}
