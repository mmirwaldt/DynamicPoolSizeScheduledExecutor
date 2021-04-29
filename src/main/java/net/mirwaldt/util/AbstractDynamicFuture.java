package net.mirwaldt.util;

import net.mirwaldt.util.job.JobWithResult;
import net.mirwaldt.util.job.JobWithoutResult;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractDynamicFuture<T> implements ScheduledFuture<T> {
    protected final ExecutorService executorService;

    protected final CountDownLatch scheduledFutureLatch = new CountDownLatch(1);
    protected final CountDownLatch futureLatch = new CountDownLatch(1);

    protected final ReentrantLock reentrantLock = new ReentrantLock();

    // guarded by reentrantLock
    protected ScheduledFuture<?> scheduledFuture;
    // guarded by reentrantLock
    protected Future<?> future;
    // guarded by reentrantLock
    protected InterruptedException interruptedException;

    public AbstractDynamicFuture(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return scheduledFuture.getDelay(unit);
    }

    @Override
    public int compareTo(Delayed o) {
        return scheduledFuture.compareTo(o);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return runLockedWithResult(() -> {
            if (future == null) {
                return scheduledFuture.cancel(mayInterruptIfRunning);
            } else {
                scheduledFuture.cancel(mayInterruptIfRunning);
                return future.cancel(mayInterruptIfRunning);
            }
        });
    }

    @Override
    public boolean isCancelled() {
        return runLockedWithResult(() -> {
            if (future == null) {
                return scheduledFuture.isCancelled();
            } else {
                return scheduledFuture.isCancelled() && future.isCancelled();
            }
        });
    }

    @Override
    public boolean isDone() {
        return runLockedWithResult(() -> {
            if (future == null) {
                return scheduledFuture.isDone();
            } else {
                return future.isDone();
            }
        });
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        throwInterruptedExceptionIfItHasBeenThrown();
        scheduledFutureLatch.await();
        scheduledFuture.get();
        futureLatch.await();
        return (T) future.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throwInterruptedExceptionIfItHasBeenThrown();

        final WaitTimer waitTimer = new WaitTimer(timeout, unit);
        final TimeUnit selectedUnit = waitTimer.getSelectedUnit();
        long remainingTimeout = waitTimer.start();

        if (scheduledFutureLatch.await(remainingTimeout, selectedUnit)) {
            throw createTimeoutException(timeout, unit);
        }

        scheduledFuture.get(waitTimer.nextRemainingTimeout(), selectedUnit);

        if (futureLatch.await(waitTimer.nextRemainingTimeout(), selectedUnit)) {
            throw createTimeoutException(timeout, unit);
        }

        return (T) future.get(waitTimer.nextRemainingTimeout(), selectedUnit);
    }

    public void setScheduledFuture(ScheduledFuture<?> scheduledFuture) {
        runLockedWithoutResult(() -> {
            this.scheduledFuture = scheduledFuture;
            scheduledFutureLatch.countDown();
        });
    }

    protected void waitForScheduledFuture() {
        try {
            scheduledFutureLatch.await();
        } catch (InterruptedException e) {
            runLockedWithoutResult(() -> {
                interruptedException = e;
            });
        }
    }

    public void runLockedWithoutResult(JobWithoutResult job) {
        reentrantLock.lock();
        try {
            job.run();
        } finally {
            reentrantLock.unlock();
        }
    }

    public <ResultType> ResultType runLockedWithResult(JobWithResult<ResultType> jobWithResult) {
        reentrantLock.lock();
        try {
            return jobWithResult.run();
        } finally {
            reentrantLock.unlock();
        }
    }

    public void throwInterruptedExceptionIfItHasBeenThrown() throws InterruptedException {
        reentrantLock.lock();
        try {
            if (interruptedException != null) {
                throw interruptedException;
            }
        } finally {
            reentrantLock.unlock();
        }
    }

    private TimeoutException createTimeoutException(long timeout, TimeUnit unit) {
        return new TimeoutException("Timeout of " + timeout + " " + unit.toChronoUnit().toString() + " is over!");
    }
}