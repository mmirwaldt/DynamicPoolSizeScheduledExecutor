package net.mirwaldt.util.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import static java.lang.Math.max;
import static java.lang.Math.subtractExact;
import static java.util.concurrent.TimeUnit.*;

final class WaitTimer {
    private final TimeUnit selectedUnit;
    private final LongSupplier millisSupplier;
    private final LongSupplier nanosSupplier;
    private long remainingTimeout;
    private long startTime;

    WaitTimer(long timeout, TimeUnit timeUnit) {
        this(timeout, timeUnit, System::currentTimeMillis, System::nanoTime);
    }

    WaitTimer(long timeout, TimeUnit timeUnit, LongSupplier millisSupplier, LongSupplier nanosSupplier) {
        this.selectedUnit = selectTimeUnit(timeUnit);
        this.remainingTimeout = max(0, selectedUnit.convert(timeout, timeUnit));
        this.millisSupplier = millisSupplier;
        this.nanosSupplier = nanosSupplier;
    }

    public TimeUnit getSelectedUnit() {
        return selectedUnit;
    }

    public long start() {
        startTime = getTime(selectedUnit);
        return remainingTimeout;
    }

    public long nextRemainingTimeout() {
        if(0 < remainingTimeout) {
            final long endTime = getTime(selectedUnit);
            final long elapsedTime = subtractExact(endTime, startTime);
            remainingTimeout = max(0, subtractExact(remainingTimeout, elapsedTime));
            startTime = endTime;
        }
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
            return nanosSupplier.getAsLong();
        } else {
            return millisSupplier.getAsLong();
        }
    }
}
