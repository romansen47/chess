package demo.chess.definitions.clocks.impl;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService; 
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChessClock extends StopWatch{

	private static final Logger logger = LogManager.getLogger(ChessClock.class);
	
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private long targetTimeMillis; 
    private Runnable timeUpAction; 
    private long incrementMillis;
    private long incrementTotal = 0;

    public void setTargetTimeMillis(long targetTimeMillis) {
        this.targetTimeMillis = targetTimeMillis;
    }

    public void setTimeUpAction(Runnable timeUpAction) {
        this.timeUpAction = timeUpAction;
    }

    public void setIncrementMillis(long incrementMillis) {
        this.incrementMillis = incrementMillis;
    }

    @Override
    public void start() {
        super.start();
        checkTimePeriodically();
    }

    @Override
    public void suspend() {
        super.suspend();
    }

    @Override
    public void resume() {
        super.resume();
    }

    public void addIncrement() {
        if (this.isStarted()) {
            incrementTotal += incrementMillis;
        }
    }

    @Override
    public long getTime(TimeUnit timeUnit) {
        return super.getTime(timeUnit) - timeUnit.convert(incrementTotal, TimeUnit.MILLISECONDS);
    }

    private void checkTimePeriodically() {
        scheduler.scheduleAtFixedRate(() -> {
        	if (this.getTime(TimeUnit.MILLISECONDS) >= targetTimeMillis) {
        		logger.info("targetTimeMillis: {}, incrementTotal: {}, this.getTime(): {}, super.getTime(): {}", targetTimeMillis, incrementTotal, this.getTime(TimeUnit.MILLISECONDS), super.getTime(TimeUnit.MILLISECONDS));
                timeUpAction.run();
                stop();
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        super.stop();
        scheduler.shutdown();
    }
}
