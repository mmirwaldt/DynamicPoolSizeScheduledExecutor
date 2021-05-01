package net.mirwaldt.util.concurrent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DynamicPoolSizeScheduledExecutorTest {
    public static final long TOLERANCE_FOR_WAIT_TIME_IN_MILLIS = 100;
    public static final long EXPECTED_WAIT_TIME_IN_MILLIS = 1000L + TOLERANCE_FOR_WAIT_TIME_IN_MILLIS;

    @SuppressWarnings("unused")
    private static Stream<Arguments> argumentsForOneExecution() {
        return Stream.of(
                Arguments.of(new RecordingRunnable(1)),
                Arguments.of(new RecordingCallable<>(1))
        );
    }

    @ParameterizedTest
    @MethodSource("argumentsForOneExecution")
    void givenRunnableOrCallable_whenScheduledWithDelay_thenExecutedAfterDelay(Recording recording)
            throws InterruptedException, ExecutionException, TimeoutException {
        final DynamicPoolSizeScheduledExecutor executor = createSingleThreadedDynamicPoolSizeScheduledExecutor();
        final long scheduleTimeInMillis = System.currentTimeMillis();

        actScheduleWithDelay(recording, executor);

        assertExecutedAfterDelayInTime(recording, scheduleTimeInMillis);

        executor.shutdown();
    }

    private DynamicPoolSizeScheduledExecutor createSingleThreadedDynamicPoolSizeScheduledExecutor() {
        return new DynamicPoolSizeScheduledExecutor(
                Executors.newSingleThreadScheduledExecutor(),
                Executors.newSingleThreadExecutor());
    }

    private void actScheduleWithDelay(Recording recording, DynamicPoolSizeScheduledExecutor executor)
            throws InterruptedException, ExecutionException, TimeoutException {
        final ScheduledFuture<?> scheduledFuture;
        if(recording instanceof Runnable) {
            scheduledFuture = executor.schedule((Runnable) recording, 1000, TimeUnit.MILLISECONDS);
        } else {
            scheduledFuture = executor.schedule((Callable<?>) recording, 1000, TimeUnit.MILLISECONDS);
        }
        scheduledFuture.get(2, SECONDS);
    }

    private void assertExecutedAfterDelayInTime(Recording recording, long scheduleTimeInMillis) {
        final AtomicLongArray scheduleTimesInMillisReference = recording.getScheduleTimesInMillisReference();
        final long startTimeInMillis = scheduleTimesInMillisReference.get(0);
        assertTrue(0 < startTimeInMillis, "Runnable has not been executed");
        final long waitTimeInMillis = startTimeInMillis - scheduleTimeInMillis;
        assertTrue(waitTimeInMillis <= EXPECTED_WAIT_TIME_IN_MILLIS, "Runnable had to wait longer than "
                + EXPECTED_WAIT_TIME_IN_MILLIS + "ms (actual wait time: " + waitTimeInMillis + "ms)");
    }

    @Test
    void givenRunnable_whenScheduledAtFixedRateAndWithDelay_thenExecutedThreeTimesAfterDelay()
            throws InterruptedException {
        final DynamicPoolSizeScheduledExecutor executor = createSingleThreadedDynamicPoolSizeScheduledExecutor();
        final int numberOfExecutions = 3;
        final long scheduleTimeInMillis = System.currentTimeMillis();
        final RecordingRunnable recordingRunnable = new RecordingRunnable(numberOfExecutions);

        actScheduleAtFixedRateAndWithDelay(executor, recordingRunnable);

        assertExecutedThreeTimesAtFixedRate(scheduleTimeInMillis, recordingRunnable);

        executor.shutdown();
    }

    private void actScheduleAtFixedRateAndWithDelay(
            DynamicPoolSizeScheduledExecutor executor, RecordingRunnable recordingRunnable)
            throws InterruptedException {
        final ScheduledFuture<?> scheduledFuture = executor.scheduleAtFixedRate(
                recordingRunnable, 2, 1, SECONDS);

        final long maxWaitTimeInSecondsForLatch = 2 + 3 + 1 /* second for tolerance */;
        assertTrue(recordingRunnable.getCountDownLatch().await(maxWaitTimeInSecondsForLatch, SECONDS),
                "Runnable has not been executed " + 3 + " times every second yet after "
                        + maxWaitTimeInSecondsForLatch + "s.");
        assertTrue(scheduledFuture.cancel(false),
                "Runnable could not be prevented from executing again");
    }

    private void assertExecutedThreeTimesAtFixedRate(long scheduleTimeInMillis, RecordingRunnable recordingRunnable) {
        final AtomicLongArray scheduleTimesInMillisReference = recordingRunnable.getScheduleTimesInMillisReference();
        final long scheduleTimeInMillisWithInitialDelay = scheduleTimeInMillis + 2 * 1000L;
        for (int i = 0; i < 3; i++) {
            final long startTimeInMillis = scheduleTimesInMillisReference.get(i);
            assertTrue(0 < startTimeInMillis, "Runnable has not been executed " + (i + 1) + " times.");
            final long actualElapsedTimeInMillis = startTimeInMillis - scheduleTimeInMillisWithInitialDelay;
            final long expectedElapsedTimeInMillis =
                    i * 1000L + (i + 1) * TOLERANCE_FOR_WAIT_TIME_IN_MILLIS;
            assertTrue(actualElapsedTimeInMillis <= expectedElapsedTimeInMillis,
                    "Runnable had to wait longer than "
                    + expectedElapsedTimeInMillis + "ms (actual wait time: " + actualElapsedTimeInMillis + "ms)");
        }
    }

    @Test
    void givenRunnable_whenScheduledAtFixedDelayAndWithDelay_thenExecutedThreeTimesAfterDelay()
            throws InterruptedException {
        final DynamicPoolSizeScheduledExecutor executor = createSingleThreadedDynamicPoolSizeScheduledExecutor();
        final int rateTimeInSeconds = 1;
        final int numberOfExecutions = 3;
        final int initialDelayInSeconds = 2;
        final long sleepTimeInMillis = 500;
        final long scheduleTimeInMillis = System.currentTimeMillis();

        final RecordingRunnable recordingRunnable =
                new RecordingRunnable(numberOfExecutions, () -> sleepUninterruptedly(sleepTimeInMillis));
        final ScheduledFuture<?> scheduledFuture = executor.scheduleWithFixedDelay(
                recordingRunnable, initialDelayInSeconds, rateTimeInSeconds, SECONDS);

        final long maxWaitTimeInSecondsForLatch =
                initialDelayInSeconds + numberOfExecutions * rateTimeInSeconds +
                        numberOfExecutions * sleepTimeInMillis + 2 /* seconds for tolerance */;
        assertTrue(recordingRunnable.getCountDownLatch().await(maxWaitTimeInSecondsForLatch, SECONDS),
                "Runnable has not been executed " + numberOfExecutions + " times every second yet after "
                        + maxWaitTimeInSecondsForLatch + "s.");
        assertTrue(scheduledFuture.cancel(false),
                "Runnable could not be prevented from executing again");

        final AtomicLongArray scheduleTimesInMillisReference = recordingRunnable.getScheduleTimesInMillisReference();
        final long scheduleTimeInMillisWithInitialDelay = scheduleTimeInMillis + initialDelayInSeconds * 1000L;
        for (int i = 0; i < numberOfExecutions; i++) {
            final long startTimeInMillis = scheduleTimesInMillisReference.get(i);
            assertTrue(0 < startTimeInMillis, "Runnable has not been executed " + (i + 1) + " times.");
            final long actualElapsedTimeInMillis = startTimeInMillis - scheduleTimeInMillisWithInitialDelay;
            final long expectedElapsedTimeInMillis =
                    i * rateTimeInSeconds * 1000L + i * sleepTimeInMillis + (i + 1) * TOLERANCE_FOR_WAIT_TIME_IN_MILLIS;
            assertTrue(actualElapsedTimeInMillis <= expectedElapsedTimeInMillis,
                    "Runnable had to wait longer than "
                            + expectedElapsedTimeInMillis + "ms (actual wait time: " + actualElapsedTimeInMillis + "ms)");
        }

        executor.shutdown();
    }

    public void sleepUninterruptedly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // We ignore it here
        }
    }
}
