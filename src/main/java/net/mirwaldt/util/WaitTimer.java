package net.mirwaldt.util;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.*;

public class WaitTimer {
    private final TimeUnit selectedUnit;
    private long remainingTimeout;
    private long startTime;

    public WaitTimer(long timeout, TimeUnit timeUnit) {
        this.selectedUnit = selectTimeUnit(timeUnit);
        this.remainingTimeout = selectedUnit.convert(timeout, selectedUnit);
    }

    public TimeUnit getSelectedUnit() {
        return selectedUnit;
    }

    public long start() {
        startTime = getTime(selectedUnit);
        return remainingTimeout;
    }

    public long nextRemainingTimeout() {
        final long endTime = getTime(selectedUnit);
        final long elapsedTime = Math.subtractExact(startTime, endTime);
        remainingTimeout = Math.subtractExact(remainingTimeout, elapsedTime);
        startTime = endTime;
        return remainingTimeout;
    }

    private TimeUnit selectTimeUnit(TimeUnit unit) {
        if(MICROSECONDS.equals(unit) || NANOSECONDS.equals(unit)) {
            return NANOSECONDS;
        } else { // we can use milli seconds
            return MILLISECONDS;
        }
    }

    private long getTime(TimeUnit selectedUnit) {
        if(NANOSECONDS.equals(selectedUnit)) {
            return System.nanoTime();
        } else {
            return System.currentTimeMillis();
        }
    }
}
