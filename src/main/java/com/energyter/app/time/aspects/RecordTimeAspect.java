package com.energyter.app.time.aspects;

import com.energyter.app.time.MeasureTime;
import com.energyter.app.time.impl.MeasureTimeImpl;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class RecordTimeAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordTimeAspect.class);

    @Around("@annotation(com.energyter.app.time.annotations.RecordTimeAnnotation)")
    public Object measureExecutionTime(ProceedingJoinPoint jointPoint) throws Throwable {
        MeasureTime measureTime = new MeasureTimeImpl();
        String methodName = jointPoint.getSignature().getName();

        LOGGER.info("Start time recording for method: {}", methodName);
        long startTime = measureTime.getCurrentTimeInMillis();
        try {
            return jointPoint.proceed();
        } finally {
            long executionTime = startTime - measureTime.getCurrentTimeInMillis();
            LOGGER.info("Execution time for method: {} - {}", methodName, executionTime);
        }
    }
}
