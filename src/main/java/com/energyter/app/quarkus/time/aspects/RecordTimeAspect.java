package com.energyter.app.quarkus.time.aspects;

import com.energyter.app.quarkus.time.MeasureTime;
import com.energyter.app.quarkus.time.impl.MeasureTimeImpl;
import com.energyter.app.quarkus.time.annotations.RecordTimeAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@RecordTimeAnnotation
@Priority(Interceptor.Priority.APPLICATION)
public class RecordTimeAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordTimeAspect.class);

    @AroundInvoke
    public Object measureExecutionTime(InvocationContext context) throws Throwable {
        MeasureTime measureTime = new MeasureTimeImpl();
        String methodName = context.getMethod().getName();

        LOGGER.debug("Start time recording for method: {}", methodName);
        System.out.print("Start time recording for method: " + methodName);
        long startTime = measureTime.getCurrentTimeInMillis();
        LOGGER.debug("Start time: {}", startTime);
        try {
            return context.proceed();
        } finally {
            long endTime = measureTime.getCurrentTimeInMillis();
            LOGGER.debug("End time: {}", endTime);
            long executionTime = startTime - endTime;
            System.out.print("Execution time for method: " + methodName + " - " + executionTime);
            LOGGER.debug("Execution time for method: {} - {}", methodName, executionTime);
        }
    }
}
