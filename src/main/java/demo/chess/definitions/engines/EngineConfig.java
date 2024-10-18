package demo.chess.definitions.engines;

public interface EngineConfig {

	int getDepth();

	void setDepth(int depth);

	int getThreads();

	void setThreads(Integer threads);

	int getHashSize();

	void setHashSize(Integer hashSize);

	int getMultiPV();

	void setMultiPV(Integer multiPV);

	int getMoveOverhead();

	void setMoveOverhead(Integer moveOverhead);

	int getContempt();

	void setContempt(Integer contempt);

	int getUciElo();

	void setUciElo(Integer uciElo);
	
}
