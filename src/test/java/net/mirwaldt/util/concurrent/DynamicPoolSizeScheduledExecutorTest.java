package net.mirwaldt.util.concurrent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DynamicPoolSizeScheduledExecutorTest {
    public static final long TOLERANCE_FOR_WAIT_TIME_IN_MILLIS = 100;
    public static final long EXPECTED_WAIT_TIME_IN_MILLIS = 1000L + TOLERANCE_FOR_WAIT_TIME_IN_MILLIS;

    @SuppressWarnings("unused")
    private static Stream<Arguments> argumentsForOneExecution() {
        return Stream.of(
                Arguments.of(new RecordingRunnable(1), true),
                Arguments.of(new RecordingCallable<>(1), true),
                Arguments.of(new RecordingRunnable(1), false),
                Arguments.of(new RecordingCallable<>(1), false)
        );
    }

    @Timeout(5)
    @ParameterizedTest
    @MethodSource("argumentsForOneExecution")
    void givenRunnableOrCallable_whenScheduledWithDelay_thenExecutedAfterDelay(
            Recording recording, boolean useTimeoutOfGetMethod)
            throws InterruptedException, ExecutionException, TimeoutException {
        final DynamicPoolSizeScheduledExecutor executor = createSingleThreadedDynamicPoolSizeScheduledExecutor();
        final long scheduleTimeInMillis = System.currentTimeMillis();

        final ScheduledFuture<?> scheduledFuture = actScheduleWithDelay(recording, executor);

        assertStatesAndWaitForHavingBeenExecutedOnceWithDelay(useTimeoutOfGetMethod, scheduledFuture);
        assertExecutedAfterDelayInTime(recording, scheduleTimeInMillis);

        executor.shutdown();
    }

    private void assertStatesAndWaitForHavingBeenExecutedOnceWithDelay(
            boolean useTimeoutOfGetMethod, ScheduledFuture<?> scheduledFuture)
            throws InterruptedException, ExecutionException, TimeoutException {
        assertFalse(scheduledFuture.isDone(),
                "Runnable/Callable cannot be 'done' before it has been executed.");
        assertFalse(scheduledFuture.isCancelled(),
                "Runnable/Callable cannot be can 'cancelled' if it has never been cancelled.");
        if(useTimeoutOfGetMethod) {
            scheduledFuture.get(2, SECONDS);
        } else {
            scheduledFuture.get();
        }
        assertTrue(scheduledFuture.isDone(),
                "Runnable/Callable must be 'done' after it has been executed.");
        assertFalse(scheduledFuture.isCancelled(),
                "Runnable/Callable cannot be can 'cancelled' if it has never been cancelled.");
    }

    private DynamicPoolSizeScheduledExecutor createSingleThreadedDynamicPoolSizeScheduledExecutor() {
        return new DynamicPoolSizeScheduledExecutor(
                Executors.newSingleThreadScheduledExecutor(),
                Executors.newSingleThreadExecutor());
    }

    private ScheduledFuture<?> actScheduleWithDelay(Recording recording, DynamicPoolSizeScheduledExecutor executor) {
        if(recording instanceof Runnable) {
            return executor.schedule((Runnable) recording, 1000, TimeUnit.MILLISECONDS);
        } else {
            return executor.schedule((Callable<?>) recording, 1000, TimeUnit.MILLISECONDS);
        }
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
        final long scheduleTimeInMillis = System.currentTimeMillis();
        final RecordingRunnable recordingRunnable = new RecordingRunnable(3);

        final ScheduledFuture<?> scheduledFuture = executor.scheduleAtFixedRate(
                recordingRunnable, 2, 1, SECONDS);

        assertStatesAndWaitForHavingBeenExecutedThreeTimesAtFixedRate(scheduledFuture, recordingRunnable);
        assertExecutedThreeTimesAtFixedRate(scheduleTimeInMillis, recordingRunnable);

        executor.shutdown();
    }

    private void assertStatesAndWaitForHavingBeenExecutedThreeTimesAtFixedRate(
            ScheduledFuture<?> scheduledFuture, RecordingRunnable recordingRunnable)
            throws InterruptedException {
        assertFalse(scheduledFuture.isDone(),
                "Runnable/Callable cannot be 'done' before it has been executed.");
        assertFalse(scheduledFuture.isCancelled(),
                "Runnable/Callable cannot be can 'cancelled' if it has never been cancelled.");

        final long maxWaitTimeInSecondsForLatch = 2 + 3 + 1 /* second for tolerance */;
        assertTrue(recordingRunnable.getCountDownLatch().await(maxWaitTimeInSecondsForLatch, SECONDS),
                "Runnable has not been executed " + 3 + " times every second yet after "
                        + maxWaitTimeInSecondsForLatch + "s.");

        assertFalse(scheduledFuture.isDone(),
                "Runnable/Callable cannot be 'done' before it has been executed.");
        assertFalse(scheduledFuture.isCancelled(),
                "Runnable/Callable cannot be can 'cancelled' if it has never been cancelled.");

        assertTrue(scheduledFuture.cancel(false),
                "Runnable could not be prevented from executing again");

        assertTrue(scheduledFuture.isDone(),
                "Runnable/Callable must be 'done' after it has been cancelled.");
        assertTrue(scheduledFuture.isCancelled(),
                "Runnable/Callable must be 'cancelled' if it has been cancelled.");
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
        final long scheduleTimeInMillis = System.currentTimeMillis();

        final RecordingRunnable recordingRunnable =
                new RecordingRunnable(3, () -> sleepUninterruptedly(500));
        final ScheduledFuture<?> scheduledFuture = executor.scheduleWithFixedDelay(
                recordingRunnable, 2, 1, SECONDS);

        assertStatesAndWaitForHavingBeenExecutedThreeTimesAtFixedDelay(recordingRunnable, scheduledFuture);
        assertExecutedThreeTimesAtFixedDelay(scheduleTimeInMillis, recordingRunnable);

        executor.shutdown();
    }

    private void assertStatesAndWaitForHavingBeenExecutedThreeTimesAtFixedDelay(
            RecordingRunnable recordingRunnable, ScheduledFuture<?> scheduledFuture) throws InterruptedException {
        assertFalse(scheduledFuture.isDone(),
                "Runnable/Callable cannot be 'done' before it has been executed.");
        assertFalse(scheduledFuture.isCancelled(),
                "Runnable/Callable cannot be can 'cancelled' if it has never been cancelled.");

        final long maxWaitTimeInSecondsForLatch = 2 + 3 + 3 * (long) 500 + 2 /* seconds for tolerance */;

        assertTrue(recordingRunnable.getCountDownLatch().await(maxWaitTimeInSecondsForLatch, SECONDS),
                "Runnable has not been executed " + 3 + " times every second yet after "
                        + maxWaitTimeInSecondsForLatch + "s.");

        assertFalse(scheduledFuture.isDone(),
                "Runnable/Callable cannot be 'done' before it has been executed.");
        assertFalse(scheduledFuture.isCancelled(),
                "Runnable/Callable cannot be can 'cancelled' if it has never been cancelled.");

        assertTrue(scheduledFuture.cancel(false),
                "Runnable could not be prevented from executing again");

        assertTrue(scheduledFuture.isDone(),
                "Runnable/Callable must be 'done' after it has been cancelled.");
        assertTrue(scheduledFuture.isCancelled(),
                "Runnable/Callable must be 'cancelled' if it has been cancelled.");
    }

    private void assertExecutedThreeTimesAtFixedDelay(long scheduleTimeInMillis, RecordingRunnable recordingRunnable) {
        final AtomicLongArray scheduleTimesInMillisReference = recordingRunnable.getScheduleTimesInMillisReference();
        final long scheduleTimeInMillisWithInitialDelay = scheduleTimeInMillis + 2 * 1000L;
        for (int i = 0; i < 3; i++) {
            final long startTimeInMillis = scheduleTimesInMillisReference.get(i);
            assertTrue(0 < startTimeInMillis, "Runnable has not been executed " + (i + 1) + " times.");
            final long actualElapsedTimeInMillis = startTimeInMillis - scheduleTimeInMillisWithInitialDelay;
            final long expectedElapsedTimeInMillis =
                    i * 1000L + i * (long) 500 + (i + 1) * TOLERANCE_FOR_WAIT_TIME_IN_MILLIS;
            assertTrue(actualElapsedTimeInMillis <= expectedElapsedTimeInMillis,
                    "Runnable had to wait longer than " + expectedElapsedTimeInMillis
                            + "ms (actual wait time: " + actualElapsedTimeInMillis + "ms)");
        }
    }

    public void sleepUninterruptedly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // We ignore it here
        }
    }
}
