package com.substrax.common.observability;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

public class TracingContextHolder {

    private final Tracer tracer;

    public TracingContextHolder(Tracer tracer){
        this.tracer = tracer;
    }

    public String getTraceId(){
        Span span = tracer.currentSpan();
        return span != null ? span.context().traceId() : "N/A";
    }

    public String getSpanID(){
        Span span = tracer.currentSpan();
        return span != null ? span.context().spanId() : "N/A";
    }
}
