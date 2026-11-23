package io.github.anthonydeseille.llmeval4j.regression;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** A metric shown without a threshold: see {@link Metric#informative(Metric)}. */
record InformativeMetric(Metric metric) implements Metric {

    InformativeMetric {
        Objects.requireNonNull(metric, "metric");
    }

    @Override
    public String label() {
        return metric.label();
    }

    @Override
    public Optional<Change> compare(Map<String, ?> reference, Map<String, ?> current) {
        return metric.compare(reference, current)
                .map(c -> new Change(c.metric(), c.reference(), c.current(), c.delta(), false, true));
    }
}
