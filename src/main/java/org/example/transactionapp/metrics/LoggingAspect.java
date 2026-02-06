package org.example.transactionapp.metrics;

import groovy.util.logging.Slf4j;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;


@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);
    private  final MeterRegistry meterRegistry;

    @Around("@annotation(loggable)")
    public Object around(ProceedingJoinPoint joinPoint, Loggable loggable) throws Throwable {

        String operation = loggable.value();
        long start = System.currentTimeMillis();
        log.info("Operation START name={} args={}", operation, Arrays.toString(joinPoint.getArgs()));

        try {
            Object result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - start;

            meterRegistry.timer("operation.latency", "operation", operation, "status", "SUCCESS"
            ).record(duration, TimeUnit.MILLISECONDS);

            meterRegistry.counter("operation.count", "operation", operation, "status", "SUCCESS"
            ).increment();

            log.info("Operation SUCCESS name={} durationMs={}", operation, duration);

            return result;

        } catch (Throwable ex) {

            long duration = System.currentTimeMillis() - start;

            meterRegistry.timer("operation.latency", "operation", operation, "status", "ERROR"
            ).record(duration, TimeUnit.MILLISECONDS);

            meterRegistry.counter("operation.count", "operation", operation, "status", "ERROR",
                    "exception", ex.getClass().getSimpleName()
            ).increment();

            log.error("Operation ERROR name={} durationMs={} error={}", operation, duration, ex.getMessage(), ex);

            throw ex;
        }
    }
}
