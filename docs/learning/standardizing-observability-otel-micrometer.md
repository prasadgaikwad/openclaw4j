# Learning: Standardizing Observability with OpenTelemetry and Micrometer

> **Problem:** Deep visibility into agent "ReAct" cycles (Reasoning loops) is difficult with logs alone. We need standardized tracing and metrics to debug multi-turn AI interactions and measure performance.
> **Concepts covered:** Micrometer Tracing, OpenTelemetry (OTel), OTLP (OpenTelemetry Protocol), Spring Boot 3 Observation API, Distributed Tracing, Context Propagation.

---

## 1. Why Micrometer Tracing + OpenTelemetry?

In the Spring Boot 3 ecosystem, observability is unified under the **Micrometer Observation API**. We chose this over a direct OpenTelemetry SDK integration for several reasons:

| Feature | Advantage |
| :--- | :--- |
| **Unified API** | One instrumentation point produces both a **Metric** (stats) and a **Span** (tracing). |
| **Spring Native** | Spring Boot 3, Spring AI, and RestClient have built-in support for Observations. |
| **Vendor Neutral** | Output can be bridged to any protocol. Using the `otel` bridge ensures we output OTLP-compliant data. |
| **Low Footprint** | Decouples our business logic from specific tracing vendor libraries. |

---

## 2. Implementation Architecture

### 2.1 The Observation Registry
The `ObservationRegistry` is the heart of the system. It manages the lifecycle of observations. We configure an `ObservedAspect` to enable the `@Observed` annotation on Spring beans.

```java
@Configuration(proxyBeanMethods = false)
public class ObservabilityConfig {
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }
}
```

### 2.2 Manual Instrumentation (AgentService)
For the main entrance of our agent, we use manual instrumentation to gain fine-grained control over naming and tags (labels).

```java
return Observation.createNotStarted("agent.process", observationRegistry)
    .contextualName("agent-process-" + message.source())
    .lowCardinalityKeyValue("channel", message.channelId())
    .observe(() -> {
        // Business logic here
        return agentPlanner.plan(context);
    });
```

### 2.3 Annotation-Based Tracing (AgentPlanner)
For internal reasoning loops, we use `@Observed` for low-boilerplate instrumentation.

```java
@Observed(name = "agent.planner", contextualName = "agent-planning")
public String plan(AgentContext context) { ... }
```

---

## 3. Configuration (application.yml)

To make traces visible, we enable sampling and the OTLP exporter:

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # Captures 100% of traces for development
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces # Standard OTLP HTTP port and path
  endpoints:
    web:
      exposure:
        include: health, info, prometheus # Export metrics for Prometheus
```

---

## 4. Visualization with Jaeger

Since we use standard OpenTelemetry protocols, any OTel-compliant backend works. For local development, **Jaeger** is recommended:

```bash
docker run -d --name jaeger \
  -e COLLECTOR_OTLP_ENABLED=true \
  -p 16686:16686 \
  -p 4317:4317 \
  -p 4318:4318 \
  jaegertracing/all-in-one:latest
```

Once running, traces appear at `http://localhost:16686`.

---

## 5. Alternative OTLP Backends

Since the project uses the standardized OTLP protocol, you can switch backends by only changing the `management.otlp.tracing.endpoint` in `application.yml`.

### 5.1 Self-Hosted (Open Source)
- **Grafana Tempo**: Best if you already use Grafana/Prometheus. Allows "Metrics to Traces" jumping.
- **Zipkin**: Lightweight and simple classic alternative.
- **SigNoz**: All-in-one platform (Metrics, Traces, Logs) with a Datadog-like UI.
- **Uptrace**: Fast, SQL-powered tracing with a polished UI.

### 5.2 Managed Services (SaaS)
- **Honeycomb**: The gold standard for high-cardinality debugging and exploratory analysis.
- **Datadog / New Relic**: Enterprise leaders with deep feature sets.
- **Cloud Native**: AWS X-Ray, Google Cloud Trace, or Azure Monitor.

### 5.3 Local Development
- **.NET Aspire Dashboard**: A beautiful, standalone OTLP dashboard that works perfectly with Java.
- **Digma**: IntelliJ plugin that highlights bottlenecks directly in your IDE based on trace data.

---

## 6. Gotchas

### 6.1 Context Propagation
Traces are linked via a `TraceID` stored in a thread-safe context. If you spawn new threads manually (e.g., `new Thread().start()`), the trace context will be lost. Use Spring's `TaskExecutor` or `ObservationRegistry.wrap()` to ensure context propagates across async boundaries.

### 6.2 Cardinality
Taint tags/labels with high cardinality (e.g., specific user IDs, timestamps) sparingly. High cardinality data can overwhelm metrics backends. Use `highCardinalityKeyValue` for data that should only appear in traces, and `lowCardinalityKeyValue` for data that should be available as metric labels.

### 6.3 Spring AI Support
Spring AI `1.1.2` is already instrumented with Micrometer. By adding the dependencies, you automatically get spans for LLM calls (e.g., `openai.chat.completion`).

### 6.4 Protocol Mismatch (gRPC vs HTTP)
OpenTelemetry collectors (like Jaeger) usually listen on two different ports for OTLP:
- **Port 4317**: gRPC protocol.
- **Port 4318**: HTTP protocol.

The `micrometer-tracing-bridge-otel` used in this project typically defaults to the OTLP **HTTP** sender. If you point it to the gRPC port (`4317`) without specifying the correct path, you will see `unexpected end of stream` or `EOFException` errors. 

**Correct configuration for HTTP:**
- Endpoint: `http://localhost:4318/v1/traces`
- Port: `4318`

---

## 7. Exercises

1. **Custom Tags:** Add a tag to `agent.planner` that records the LLM model name being used.
2. **Error Spans:** Research how to use `Observation.error(Throwable)` to ensure tool failures show up as "error" spans in Jaeger.
3. **Metrics Dashboards:** Configure a local Prometheus and Grafana instance to consume the `/actuator/prometheus` endpoint and build a dashboard for agent latency.
4. **Baggage Propagation:** Implement "Baggage" to pass a specific piece of metadata (like a correlation ID) through the entire stack, even across tool calls.
