package io.github.anthonydeseille.llmeval4j.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DailyBudgetTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-10T10:00:00Z"));
    private final List<String> events = new ArrayList<>();
    private final BudgetListener listener = new BudgetListener() {
        @Override
        public void onWarning(double spentToday, double dailyLimit) {
            events.add("warning " + spentToday);
        }

        @Override
        public void onExceeded(double spentToday, double dailyLimit) {
            events.add("exceeded " + spentToday);
        }

        @Override
        public void onRefused(double spentToday, double dailyLimit) {
            events.add("refused");
        }
    };
    private final DailyBudget budget = DailyBudget.builder(10).clock(clock).listener(listener).build();

    @Test
    void calls_pass_while_the_budget_is_not_spent() {
        budget.add(5);

        assertThatCode(budget::check).doesNotThrowAnyException();
        assertThat(budget.spentToday()).isEqualTo(5);
        assertThat(budget.remainingToday()).isEqualTo(5);
    }

    @Test
    void calls_are_refused_once_the_budget_is_spent() {
        budget.add(6);
        budget.add(4);

        assertThatThrownBy(budget::check).isInstanceOf(BudgetExceededException.class)
                .hasMessage("Daily budget reached: $10.00 spent of $10.00");
        assertThat(budget.remainingToday()).isZero();
    }

    @Test
    void the_listener_is_told_once_at_the_warning_threshold_and_once_at_the_limit() {
        budget.add(5);
        budget.add(3);
        budget.add(1);
        budget.add(2);
        budget.add(2);

        assertThat(events).containsExactly("warning 8.0", "exceeded 11.0");
    }

    @Test
    void a_single_large_call_goes_straight_to_exceeded_without_a_warning() {
        budget.add(15);

        assertThat(events).containsExactly("exceeded 15.0");
    }

    @Test
    void every_refusal_is_reported() {
        budget.add(10);
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(budget::check).isInstanceOf(BudgetExceededException.class);
        }

        assertThat(events).containsExactly("exceeded 10.0", "refused", "refused");
    }

    @Test
    void spending_and_alerts_reset_the_next_day() {
        budget.add(10);
        clock.advance(Duration.ofDays(1));

        assertThatCode(budget::check).doesNotThrowAnyException();
        assertThat(budget.spentToday()).isZero();
        budget.add(9);
        assertThat(events).containsExactly("exceeded 10.0", "warning 9.0");
    }

    @Test
    void unknown_and_zero_costs_are_ignored() {
        budget.add(Double.NaN);
        budget.add(0);
        budget.add(-1);

        assertThat(budget.spentToday()).isZero();
    }

    @Test
    void an_unlimited_budget_tracks_spending_but_never_refuses() {
        DailyBudget unlimited = DailyBudget.unlimited();
        unlimited.add(1_000_000);

        assertThatCode(unlimited::check).doesNotThrowAnyException();
        assertThat(unlimited.spentToday()).isEqualTo(1_000_000);
    }

    @Test
    void the_warning_threshold_can_be_changed() {
        DailyBudget half = DailyBudget.builder(10).warningThreshold(0.5).clock(clock).listener(listener).build();
        half.add(4.9);
        half.add(0.1);

        assertThat(events).containsExactly("warning 5.0");
    }

    @Test
    void invalid_settings_are_rejected() {
        assertThatThrownBy(() -> DailyBudget.of(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DailyBudget.builder(1).warningThreshold(0).build())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DailyBudget.builder(1).warningThreshold(1.5).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void concurrent_calls_lose_no_spending() throws InterruptedException {
        DailyBudget shared = DailyBudget.unlimited();
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < 8; t++) {
            Thread thread = new Thread(() -> {
                for (int i = 0; i < 1_000; i++) {
                    shared.add(0.01);
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        assertThat(shared.spentToday()).isCloseTo(80, within(1e-6));
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
