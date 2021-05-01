package net.mirwaldt.util.concurrent;

public class RecordingRunnable extends Recording implements Runnable {
    public RecordingRunnable(int capacity) {
        super(capacity);
    }

    public RecordingRunnable(int capacity, Runnable afterExecutionJob) {
        super(capacity, afterExecutionJob);
    }

    @Override
    public void run() {
        executed();
    }
}
