package net.mirwaldt.util.concurrent;

import java.util.concurrent.Callable;

public class RecordingCallable<V> extends Recording implements Callable<V> {
    public RecordingCallable(int capacity) {
        super(capacity);
    }

    public RecordingCallable(int capacity, Runnable afterExecutionJob) {
        super(capacity, afterExecutionJob);
    }

    @Override
    public V call() {
        executed();
        return null;
    }
}
