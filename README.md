# llmeval4j

**LLM evals for Java, with cost tracking.**

Most LLM evaluation tools (promptfoo, DeepEval, Ragas) live in the Python and JavaScript worlds.
llmeval4j brings the same discipline to Java projects, with plain JUnit tests and Maven:

- **know what each call costs**, with a price table you control;
- **version your prompts by their content**, so every result says which prompt produced it;
- **cap daily spending**, with alerts before the cap;
- **keep a history of evaluations** in a CSV file next to your code;
- **fail a build on a real regression**, not on noise.

No runtime dependency: it works with any LLM client (Anthropic SDK, LangChain4j, Spring AI…) and any
framework.

> Status: early development, extracted from a real application. The API may change before 1.0.

## Quick start

```java
// A prompt from src/main/resources/prompts/, and its version to store next to each result
String prompt = Prompts.load("/prompts/greeting.md");
String promptVersion = Prompts.version(prompt);   // e.g. "3f2a9c1e", changes on every edit

// Default prices, completed with a model of your own
PriceTable prices = PriceTable.defaults()
        .with("my-fine-tuned-model", new ModelPrice(3.0, 15.0));

// Token counts as reported by your LLM client after the call
double dollars = prices.cost("claude-sonnet-5-5", 12_000, 800);   // 0.032
```

This example is a test: [`QuickStartTest`](src/test/java/io/github/anthonydeseille/llmeval4j/QuickStartTest.java).

## Prices

Prices change and models come and go, so the built-in table is only a starting point, dated in
`PriceTable.DEFAULTS_AS_OF`. Override it in code (`with`) or from a properties file:

```properties
claude-opus-5-5.input=4.0
claude-opus-5-5.output=20.0
my-local-model.input=0
my-local-model.output=0
```

```java
PriceTable prices = PriceTable.defaults().withProperties(properties);
```

A model without a price has an **unknown** cost (`NaN`), never a wrong one: a report should show
"n/a" rather than a misleading `$0.00`. `ModelPrice.batch()` gives the half price of batch APIs.

## Prompt versions

`Prompts.version(text)` is the first 8 hexadecimal characters of the SHA-256 of the prompt. It
changes by itself on every edit: no version number to forget to bump. `Prompts.load` normalizes line
endings and trailing whitespace, so the version is the same on every machine.

## Daily budget

A guard against a runaway bill: a bug in a loop, a traffic spike or a prompt that suddenly produces
very long answers.

```java
DailyBudget budget = DailyBudget.builder(5.00)          // dollars per day
        .listener(new BudgetListener() {
            @Override
            public void onWarning(double spent, double limit) {
                log.warn("80% of today's LLM budget spent: ${}", spent);
            }

            @Override
            public void onExceeded(double spent, double limit) {
                log.error("Today's LLM budget is spent: calls refused until tomorrow");
            }
        })
        .build();

budget.check();                                         // throws BudgetExceededException when spent
var response = client.call(request);
budget.add(prices.cost(model, response.inputTokens(), response.outputTokens()));
```

The listener is told once a day at 80% (configurable) and at 100%; plug your logs, metrics or
alerting there. No framework involved: in a web application, map `BudgetExceededException` to an
HTTP 429. Spending is recorded after each call, so concurrent calls can overshoot the cap by a few
calls: keep the monthly limit of your provider's console as the real safety net.

## Evaluation history

One row per evaluation run, in a CSV file committed with the code. The columns are yours:

```java
Map<String, Object> run = new LinkedHashMap<>();
run.put("date", LocalDate.now());
run.put("model", "claude-sonnet-5-5");
run.put("prompt", Prompts.version(prompt));
run.put("questions", 20);
run.put("quality_pct", 82);
run.put("errors", 1);
run.put("cost_usd", Dollars.roundToCents(cost));        // empty if the cost is unknown
run.put("comment", "shorter system prompt");

EvalHistory.append(Path.of("evals/history.csv"), run);  // creates the file and its header if needed
```

## Regression check

Compare a run with the latest comparable one of the history, and fail only on a clear drop. LLM
evaluations are noisy: run the same evaluation twice to see how much the scores move by themselves,
and set thresholds above that.

```java
RegressionCheck check = RegressionCheck.builder()
        .sameValuesOf("model", "questions")             // otherwise you compare different test sets
        .metric(Metric.percentage("quality_pct", "Quality", 5))          // fails below -5 points
        .metric(Metric.lowerIsBetter("errors", "Errors", 1))             // fails above +1
        .metric(Metric.ratio("verified", "citations", "Verified citations", 2))
        .metric(Metric.lowerIsBetter("cost_usd", "Cost ($)", Double.POSITIVE_INFINITY))  // shown only
        .build();

Comparison comparison = check.compare(EvalHistory.read(Path.of("evals/history.csv")), run);
assertThat(comparison.regression()).as(comparison.toMarkdown()).isFalse();
```

`toMarkdown()` gives a table ready for a pull request comment:

| Metric | Reference | Now | Change | |
|---|---|---|---|---|
| Quality | 80% | 74% | -6 pts | ❌ regression |
| Errors | 2 | 2 | 0 | ✅ |

A metric missing on either side (a judge that did not run, an empty cell) is not compared. With no
comparable run, nothing is compared and nothing fails. To try a new model, compare it with the
current one: `check.compare(history, run, Map.of("model", "claude-sonnet-5-5"))`. Implement
`Metric` for any other indicator.

## Requirements

Java 17 or later.

## License

[Apache License 2.0](LICENSE)
