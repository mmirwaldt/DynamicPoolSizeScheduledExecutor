package net.mirwaldt.util.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class DynamicPoolSizeScheduledExecutor implements ScheduledExecutorService {
    private final ScheduledExecutorService scheduledExecutorService;
    private final ExecutorService executorService;

    public DynamicPoolSizeScheduledExecutor(
            ScheduledExecutorService scheduledExecutorService, ExecutorService executorService) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.executorService = executorService;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        final RunnableDynamicFuture<?> runnableDynamicFuture = new RunnableDynamicFuture<Void>(executorService, command);
        final ScheduledFuture<?> scheduledFuture = scheduledExecutorService.schedule(runnableDynamicFuture, delay, unit);
        runnableDynamicFuture.setScheduledFuture(scheduledFuture);
        return runnableDynamicFuture;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        final CallableDynamicFuture<V> callableDynamicFuture = new CallableDynamicFuture<V>(executorService, callable);
        final ScheduledFuture<V> scheduledFuture = scheduledExecutorService.schedule(callableDynamicFuture, delay, unit);
        callableDynamicFuture.setScheduledFuture(scheduledFuture);
        return callableDynamicFuture;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        final RunnableDynamicFuture<?> runnableDynamicFuture = new RunnableDynamicFuture<Void>(executorService, command);
        final ScheduledFuture<?> scheduledFuture =
                scheduledExecutorService.scheduleAtFixedRate(command, initialDelay, period, unit);
        runnableDynamicFuture.setScheduledFuture(scheduledFuture);
        return runnableDynamicFuture;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        final RunnableDynamicFuture<?> runnableDynamicFuture = new RunnableDynamicFuture<Void>(executorService, command);
        final ScheduledFuture<?> scheduledFuture =
                scheduledExecutorService.scheduleWithFixedDelay(command, initialDelay, delay, unit);
        runnableDynamicFuture.setScheduledFuture(scheduledFuture);
        return runnableDynamicFuture;
    }

    @Override
    public void shutdown() {
        scheduledExecutorService.shutdown();
        executorService.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        final List<Runnable> result = new ArrayList<>();
        result.addAll(scheduledExecutorService.shutdownNow());
        result.addAll(executorService.shutdownNow());
        return result;
    }

    @Override
    public boolean isShutdown() {
        return scheduledExecutorService.isShutdown() && executorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return scheduledExecutorService.isTerminated() && executorService.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        final WaitTimer waitTimer = new WaitTimer(timeout, unit);
        final TimeUnit selectedUnit = waitTimer.getSelectedUnit();
        long remainingTimeout = waitTimer.start();

        final boolean result = scheduledExecutorService.awaitTermination(remainingTimeout, selectedUnit);

        return result && executorService.awaitTermination(waitTimer.nextRemainingTimeout(), selectedUnit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return executorService.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return executorService.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return executorService.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return executorService.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return executorService.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return executorService.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return executorService.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        executorService.execute(command);
    }
}
