package net.mirwaldt.util.concurrent;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

abstract class AbstractDynamicFuture<T> implements ScheduledFuture<T> {
    protected final ExecutorService executorService;

    protected final CountDownLatch scheduledFutureLatch = new CountDownLatch(1);
    protected final CountDownLatch futureLatch = new CountDownLatch(1);

    protected final ReentrantLock reentrantLock = new ReentrantLock();

    // guarded by reentrantLock
    protected ScheduledFuture<?> scheduledFuture;
    // guarded by reentrantLock
    protected Future<?> future;

    protected AbstractDynamicFuture(ExecutorService executorService) {
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
        waitForScheduledFuture();
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
        waitForScheduledFuture();
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
        waitForScheduledFuture();
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
        scheduledFutureLatch.await();
        scheduledFuture.get();
        futureLatch.await();
        return (T) future.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        final WaitTimer waitTimer = new WaitTimer(timeout, unit);
        final TimeUnit selectedUnit = waitTimer.getSelectedUnit();
        long remainingTimeout = waitTimer.start();

        if (!scheduledFutureLatch.await(remainingTimeout, selectedUnit)) {
            throw createTimeoutException(timeout, unit);
        }

        scheduledFuture.get(waitTimer.nextRemainingTimeout(), selectedUnit);

        if (!futureLatch.await(waitTimer.nextRemainingTimeout(), selectedUnit)) {
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

    protected void runLockedWithoutResult(Runnable job) {
        reentrantLock.lock();
        try {
            job.run();
        } finally {
            reentrantLock.unlock();
        }
    }

    protected <ResultType> ResultType runLockedWithResult(Callable<ResultType> jobWithResult) {
        reentrantLock.lock();
        try {
            return jobWithResult.call();
        } catch (Exception e) {
            throw new AssertionError("The callable parameter is not supposed to throw an exception!", e);
        } finally {
            reentrantLock.unlock();
        }
    }

    protected void waitForScheduledFuture() {
        try {
            scheduledFutureLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting for the setting of the scheduledFuture.", e);
        }
    }

    private TimeoutException createTimeoutException(long timeout, TimeUnit unit) {
        return new TimeoutException("Timeout of " + timeout + " " + unit.toChronoUnit().toString() + " is over!");
    }
}