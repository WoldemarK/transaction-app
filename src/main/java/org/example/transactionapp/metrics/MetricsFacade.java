package org.example.transactionapp.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MetricsFacade {

    private final MeterRegistry meterRegistry;

    public void walletCreated(String currency) {
        meterRegistry.counter("wallet.create.count", "currency", currency).increment();
    }

    public void walletCreateError(String reason) {
        meterRegistry.counter("wallet.create.error.count", "reason", reason).increment();
    }

    public void walletFetched(boolean found) {
        meterRegistry.counter("wallet.get.count", "status", found ? "FOUND" : "NOT_FOUND").increment();
    }
}
