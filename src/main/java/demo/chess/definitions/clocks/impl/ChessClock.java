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
	private volatile long targetTimeMillis;
	private volatile Runnable timeUpAction;
	private volatile long incrementMillis;
	private volatile long incrementTotal = 0;

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
	 * Returns the cumulative wall-clock time for which this clock has actively run.
	 *
	 * <p>Unlike {@link #getTime(TimeUnit)}, this value is not adjusted by
	 * increments and can therefore be differenced between two completed moves
	 * to obtain the thinking time spent on the latest move.</p>
	 *
	 * @return cumulative active clock time in milliseconds
	 */
	public long getElapsedThinkingTimeMillis() {
		return super.getTime(TimeUnit.MILLISECONDS);
	}

	/**
	 * Returns the remaining clock time in milliseconds.
	 *
	 * @return remaining milliseconds, never negative
	 */
	public long getRemainingTimeMillis() {
		return Math.max(0L, targetTimeMillis - getTime(TimeUnit.MILLISECONDS));
	}

	/**
	 * Returns whether the configured clock time has expired.
	 *
	 * @return true when no clock time remains
	 */
	public boolean isTimeUp() {
		return getTime(TimeUnit.MILLISECONDS) >= targetTimeMillis;
	}

	/**
	 * Checks the time periodically.
	 */
	private void checkTimePeriodically() {
		scheduler.scheduleAtFixedRate(() -> {
			if (isTimeUp()) {
				logger.debug("targetTimeMillis: {}, incrementTotal: {}, this.getTime(): {}, super.getTime(): {}",
						targetTimeMillis, incrementTotal, this.getTime(TimeUnit.MILLISECONDS),
						super.getTime(TimeUnit.MILLISECONDS));

				Runnable action = timeUpAction;
				if (action != null) {
					action.run();
				}

				if (this.isStarted() && !this.isStopped()) {
					stop();
				}
			}
		}, 0, 100, TimeUnit.MILLISECONDS);
	}

	/**
	 * Performs the stop operation.
	 */
	@Override
	public void stop() {
		if (this.isStarted() && !this.isStopped()) {
			super.stop();
		}
		scheduler.shutdown();
	}

	/**
	 * Returns whether the running.
	 * @return true when the condition is satisfied; otherwise false
	 */
	public boolean isRunning() {
		return this.isStarted() && !this.isStopped() && !this.isSuspended();
	}

}
