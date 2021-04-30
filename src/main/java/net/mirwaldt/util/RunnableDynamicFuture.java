package net.mirwaldt.util;

import java.util.concurrent.ExecutorService;

final class RunnableDynamicFuture<T> extends AbstractDynamicFuture<T> implements Runnable {
    private final Runnable runnable;

    public RunnableDynamicFuture(ExecutorService executorService, Runnable runnable) {
        super(executorService);
        this.runnable = runnable;
    }

    @Override
    public void run() {
        waitForScheduledFuture();
        runLockedWithoutResult(() -> {
            if (!scheduledFuture.isCancelled()) {
                future = executorService.submit(runnable);
                futureLatch.countDown();
            }
        });
    }
}
