package org.example.transactionapp.metrics;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TimedGuardAspect {

    @Before("@annotation(io.micrometer.core.annotation.Timed)")
    public void forbidTimed() {
        throw new IllegalStateException("@Timed запрещён. Используй @Loggable.");
    }
}
