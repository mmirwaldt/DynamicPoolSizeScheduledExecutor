package net.mirwaldt.util.concurrent;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WaitTimerTest {
    @Nested
    class SelectedTimeUnitTest {
        @Test
        void givenNanosAsTimeUnit_whenWaitTimerCreated_thenExpectNanosAsSelectedMillis() {
            assertEquals(NANOSECONDS, new WaitTimer(1, NANOSECONDS).getSelectedUnit());
        }

        @Test
        void givenMicrosAsTimeUnit_whenWaitTimerCreated_thenExpectNanosAsSelectedMillis() {
            assertEquals(NANOSECONDS, new WaitTimer(1, MICROSECONDS).getSelectedUnit());
        }

        @Test
        void givenMillisAsTimeUnit_whenWaitTimerCreated_thenExpectMillisAsSelectedMillis() {
            assertEquals(MILLISECONDS, new WaitTimer(1, MILLISECONDS).getSelectedUnit());
        }

        @Test
        void givenSecondsAsTimeUnit_whenWaitTimerCreated_thenExpectMillisAsSelectedMillis() {
            assertEquals(MILLISECONDS, new WaitTimer(1, SECONDS).getSelectedUnit());
        }

        @Test
        void givenMinutesAsTimeUnit_whenWaitTimerCreated_thenExpectMillisAsSelectedMillis() {
            assertEquals(MILLISECONDS, new WaitTimer(1, MINUTES).getSelectedUnit());
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class NextRemainingTimeout {
        @SuppressWarnings("unused")
        private Stream<Arguments> arguments() {
            final Supplier<LongSupplier> millisSupplierFactory =
                    () -> longSupplier(0L, 10_000L, 50_000L, 100_000L);
            final Supplier<LongSupplier> nanosSupplierFactory =
                    () -> longSupplier(0L, 10_000_000_000L, 50_000_000_000L, 100_000_000_000L);
            return Stream.of(
                    Arguments.of(NANOSECONDS, longSupplier(), nanosSupplierFactory.get()),
                    Arguments.of(MICROSECONDS, longSupplier(), nanosSupplierFactory.get()),
                    Arguments.of(MILLISECONDS, millisSupplierFactory.get(), longSupplier()),
                    Arguments.of(SECONDS, millisSupplierFactory.get(), longSupplier())
            );
        }

        @ParameterizedTest
        @MethodSource("arguments")
        void givenTimeoutIsNegative_whenStartAndNextRemainingTimeouts_thenExpectAlwaysTimeoutIsZero(
                TimeUnit unit, LongSupplier millisSupplier, LongSupplier nanosSupplier) {
            final WaitTimer waitTimer = new WaitTimer(-1, unit, millisSupplier, nanosSupplier);
            assertEquals(0, waitTimer.start());
            assertEquals(0, waitTimer.nextRemainingTimeout());
            assertEquals(0, waitTimer.nextRemainingTimeout());
        }

        @ParameterizedTest
        @MethodSource("arguments")
        void givenTimeoutIsZero_whenStartAndNextRemainingTimeouts_thenExpectAlwaysTimeoutIsZero(
                TimeUnit unit, LongSupplier millisSupplier, LongSupplier nanosSupplier) {
            final WaitTimer waitTimer = new WaitTimer(0, unit, millisSupplier, nanosSupplier);
            assertEquals(0, waitTimer.start());
            assertEquals(0, waitTimer.nextRemainingTimeout());
            assertEquals(0, waitTimer.nextRemainingTimeout());
        }

        @ParameterizedTest
        @MethodSource("arguments")
        void givenTimeoutIsPositive_whenOneStartAndTwoTimesNextRemainingTimeouts_thenExpectOneValueAndThenZero(
                TimeUnit unit, LongSupplier millisSupplier, LongSupplier nanosSupplier) {
            final long timeoutInNanos = 10_000_000_000L;
            final long timeout = unit.convert(timeoutInNanos, NANOSECONDS);

            final WaitTimer waitTimer = new WaitTimer(timeout, unit, millisSupplier, nanosSupplier);
            final TimeUnit selectedUnit = waitTimer.getSelectedUnit();
            assertEquals(selectedUnit.convert(timeoutInNanos, NANOSECONDS), waitTimer.start());
            assertEquals(0, waitTimer.nextRemainingTimeout());
            assertEquals(0, waitTimer.nextRemainingTimeout());
        }

        @ParameterizedTest
        @MethodSource("arguments")
        void givenTimeoutIsPositive_whenOneStartAndTwoTimesNextRemainingTimeouts_thenExpectTwoValuesAndThenThenZero(
                TimeUnit unit, LongSupplier millisSupplier, LongSupplier nanosSupplier) {
            final long timeoutInNanos = 20_000_000_000L;
            final long timeout = unit.convert(timeoutInNanos, NANOSECONDS);

            final WaitTimer waitTimer = new WaitTimer(timeout, unit, millisSupplier, nanosSupplier);
            final TimeUnit selectedUnit = waitTimer.getSelectedUnit();
            assertEquals(selectedUnit.convert(timeoutInNanos, NANOSECONDS), waitTimer.start());
            assertEquals(selectedUnit.convert(10_000_000_000L, NANOSECONDS),
                    waitTimer.nextRemainingTimeout());
            assertEquals(0, waitTimer.nextRemainingTimeout());
        }

        @ParameterizedTest
        @MethodSource("arguments")
        void givenTimeoutIsPositive_whenOneStartAndThreeTimesNextRemainingTimeouts_thenExpectThreeValuesAndThenZero(
                TimeUnit unit, LongSupplier millisSupplier, LongSupplier nanosSupplier) {
            final long timeoutInNanos = 100_000_000_000L;
            final long timeout = unit.convert(timeoutInNanos, NANOSECONDS);

            final WaitTimer waitTimer = new WaitTimer(timeout, unit, millisSupplier, nanosSupplier);
            final TimeUnit selectedUnit = waitTimer.getSelectedUnit();
            assertEquals(selectedUnit.convert(timeoutInNanos, NANOSECONDS), waitTimer.start());
            assertEquals(selectedUnit.convert(90_000_000_000L, NANOSECONDS),
                    waitTimer.nextRemainingTimeout());
            assertEquals(selectedUnit.convert(50_000_000_000L, NANOSECONDS),
                    waitTimer.nextRemainingTimeout());
            assertEquals(0, waitTimer.nextRemainingTimeout());
        }
    }

    public static LongSupplier longSupplier(Long... rest) {
        return new LongSupplier() {
            private final Deque<Long> deque = new ArrayDeque<>(asList(rest));
            @Override
            public long getAsLong() {
                return deque.removeFirst();
            }
        };
    }
}
