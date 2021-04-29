package net.mirwaldt.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class CallableDynamicFuture<V> extends AbstractDynamicFuture<V> implements Callable<V> {
    private final Callable<V> callable;

    public CallableDynamicFuture(ExecutorService executorService, Callable<V> callable) {
        super(executorService);
        this.callable = callable;
    }

    @Override
    public V call() {
        waitForScheduledFuture();
        runLockedWithoutResult(() -> {
            if (!scheduledFuture.isCancelled()) {
                future = executorService.submit(callable);
                futureLatch.countDown();
            }
        });
        return null;
    }
}
