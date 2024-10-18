package demo.chess.definitions.engines;

public class UciEngineConfig implements EngineConfig{

    protected int depth;
    protected int threads;
    protected int hashSize;
    protected int multiPV;
    protected int contempt;
    protected int moveOverhead;
    protected int uciElo;
    
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
