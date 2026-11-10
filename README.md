# llmeval4j

**LLM evals for Java, with cost tracking.**

Most LLM evaluation tools (promptfoo, DeepEval, Ragas) live in the Python and JavaScript worlds.
llmeval4j brings the same discipline to Java projects, with plain JUnit tests and Maven:

- **know what each call costs**, with a price table you control;
- **version your prompts by their content**, so every result says which prompt produced it;
- *(coming)* **cap daily spending**, with alerts before the cap;
- *(coming)* **keep a history of evaluations** and **fail a build on a real regression**, not on noise.

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

## Requirements

Java 17 or later.

## License

[Apache License 2.0](LICENSE)
