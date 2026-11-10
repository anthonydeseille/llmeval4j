package io.github.anthonydeseille.llmeval4j.pricing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * Prices of models by model id, to turn token counts into dollars.
 * <p>
 * Prices change and new models come out: {@link #defaults()} is only a starting point, dated in
 * {@link #DEFAULTS_AS_OF}. Override or complete it with your own prices ({@link #with},
 * {@link #withProperties}) rather than trusting it blindly.
 * <p>
 * An unknown model has an unknown cost: {@link #cost} returns {@code NaN}, never a wrong number, so
 * that a report shows "n/a" instead of a misleading zero.
 * <p>
 * Instances are immutable.
 */
public final class PriceTable {

    /** Date the default prices were checked against the providers' pricing pages. */
    public static final String DEFAULTS_AS_OF = "2026-10";

    private static final PriceTable DEFAULTS = new PriceTable(Map.of(
            "claude-opus-5-5", new ModelPrice(4.0, 20.0),
            "claude-sonnet-5-5", new ModelPrice(2.0, 10.0),
            "claude-haiku-5-5", new ModelPrice(0.10, 0.50)));

    private static final PriceTable EMPTY = new PriceTable(Map.of());

    private final Map<String, ModelPrice> prices;

    private PriceTable(Map<String, ModelPrice> prices) {
        this.prices = Collections.unmodifiableMap(new LinkedHashMap<>(prices));
    }

    /** Built-in prices of common models, as of {@link #DEFAULTS_AS_OF}. */
    public static PriceTable defaults() {
        return DEFAULTS;
    }

    /** A table without any price, to fill with your own. */
    public static PriceTable empty() {
        return EMPTY;
    }

    /** A copy of this table where {@code model} has the given price, replacing any previous one. */
    public PriceTable with(String model, ModelPrice price) {
        Map<String, ModelPrice> copy = new LinkedHashMap<>(prices);
        copy.put(model, price);
        return new PriceTable(copy);
    }

    /**
     * A copy of this table completed or overridden by properties such as
     * {@code claude-opus-5-5.input=4.0} and {@code claude-opus-5-5.output=20.0}, in dollars per
     * million tokens. Both values are required for each model.
     *
     * @throws IllegalArgumentException if a model has only one of the two values, or a value is not a
     *                                  number
     */
    public PriceTable withProperties(Properties properties) {
        Set<String> models = new TreeSet<>();
        for (String key : properties.stringPropertyNames()) {
            int dot = key.lastIndexOf('.');
            String suffix = dot < 0 ? "" : key.substring(dot);
            if (!suffix.equals(".input") && !suffix.equals(".output")) {
                throw new IllegalArgumentException("Expected <model>.input or <model>.output: " + key);
            }
            models.add(key.substring(0, dot));
        }
        Map<String, ModelPrice> copy = new LinkedHashMap<>(prices);
        for (String model : models) {
            copy.put(model, new ModelPrice(number(properties, model + ".input"), number(properties, model + ".output")));
        }
        return new PriceTable(copy);
    }

    public Optional<ModelPrice> find(String model) {
        return Optional.ofNullable(prices.get(model));
    }

    /** Cost in dollars of a call; {@code NaN} if the model has no price in this table. */
    public double cost(String model, long inputTokens, long outputTokens) {
        return find(model).map(p -> p.cost(inputTokens, outputTokens)).orElse(Double.NaN);
    }

    /** Model ids and their prices, in insertion order. */
    public Map<String, ModelPrice> asMap() {
        return prices;
    }

    private static double number(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing " + key);
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a number for " + key + ": " + value, e);
        }
    }
}
