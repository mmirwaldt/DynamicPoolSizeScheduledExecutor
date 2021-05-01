package net.mirwaldt.util.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

public class Recording {
    private final CountDownLatch countDownLatch;
    private final AtomicLongArray scheduleTimesInMillisReference;
    private final Runnable afterExecutionJob;

    private final AtomicInteger indexReference = new AtomicInteger();

    public Recording(int capacity) {
        this(capacity, () -> {});
    }

    public Recording(int capacity, Runnable afterExecutionJob) {
        this.afterExecutionJob = afterExecutionJob;
        this.countDownLatch = new CountDownLatch(capacity);
        this.scheduleTimesInMillisReference = new AtomicLongArray(capacity);
    }

    public void executed() {
        scheduleTimesInMillisReference.set(indexReference.getAndIncrement(), System.currentTimeMillis());
        countDownLatch.countDown();
        afterExecutionJob.run();
    }

    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }

    public AtomicLongArray getScheduleTimesInMillisReference() {
        return scheduleTimesInMillisReference;
    }
}
