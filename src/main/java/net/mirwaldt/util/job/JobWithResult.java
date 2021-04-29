package net.mirwaldt.util.job;

@FunctionalInterface
public interface JobWithResult<T> {
    T run();
}
