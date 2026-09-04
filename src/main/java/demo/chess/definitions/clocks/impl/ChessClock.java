package demo.chess.definitions.clocks.impl;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChessClock extends StopWatch {

	private static final Logger logger = LogManager.getLogger(ChessClock.class);

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private long targetTimeMillis;
	private Runnable timeUpAction;
	private long incrementMillis;
	private long incrementTotal = 0;

	/**
	 * Sets the target time millis.
	 * @param targetTimeMillis the target time millis
	 */
	public void setTargetTimeMillis(long targetTimeMillis) {
		this.targetTimeMillis = targetTimeMillis;
	}

	/**
	 * Sets the time up action.
	 * @param timeUpAction the time up action
	 */
	public void setTimeUpAction(Runnable timeUpAction) {
		this.timeUpAction = timeUpAction;
	}

	/**
	 * Sets the increment millis.
	 * @param incrementMillis the increment millis
	 */
	public void setIncrementMillis(long incrementMillis) {
		this.incrementMillis = incrementMillis;
	}

	/**
	 * Performs the start operation.
	 */
	@Override
	public void start() {
		if (!this.isStarted()) {
			super.start();
			checkTimePeriodically();
		}
	}

	/**
	 * Performs the suspend operation.
	 */
	@Override
	public void suspend() {
		super.suspend();
	}

	/**
	 * Performs the resume operation.
	 */
	@Override
	public void resume() {
		super.resume();
	}

	/**
	 * Adds the increment.
	 */
	public void addIncrement() {
		if (this.isStarted()) {
			incrementTotal += incrementMillis;
		}
	}

	/**
	 * Adds the additional time.
	 * @param additionalMillis the additional millis
	 */
	public void addAdditionalTime(long additionalMillis) {
		if (this.isStarted()) {
			this.targetTimeMillis += additionalMillis;
		}
	}

	/**
	 * Returns the time.
	 * @param timeUnit the time unit
	 * @return the time
	 */
	@Override
	public long getTime(TimeUnit timeUnit) {
		return super.getTime(timeUnit) - timeUnit.convert(incrementTotal, TimeUnit.MILLISECONDS);
	}

	/**
	 * Checks the time periodically.
	 */
	private void checkTimePeriodically() {
		scheduler.scheduleAtFixedRate(() -> {
			if (this.getTime(TimeUnit.MILLISECONDS) >= targetTimeMillis) {
				logger.debug("targetTimeMillis: {}, incrementTotal: {}, this.getTime(): {}, super.getTime(): {}",
						targetTimeMillis, incrementTotal, this.getTime(TimeUnit.MILLISECONDS),
						super.getTime(TimeUnit.MILLISECONDS));
				timeUpAction.run();
				stop();
			}
		}, 0, 100, TimeUnit.MILLISECONDS);
	}

	/**
	 * Performs the stop operation.
	 */
	@Override
	public void stop() {
		super.stop();
		scheduler.shutdown();
	}

	/**
	 * Returns whether the running.
	 * @return true when the condition is satisfied; otherwise false
	 */
	public boolean isRunning() {
		return this.isStarted() && !this.isSuspended();
	}

}
