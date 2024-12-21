package demo.chess.definitions.engines.impl;

import demo.chess.definitions.engines.DeepAnalysisEngine;

public class DeepAnalysisUciEngine extends EvaluationUciEngine implements DeepAnalysisEngine{

    public DeepAnalysisUciEngine(String path) throws Exception {
        super(path);
    }
}
