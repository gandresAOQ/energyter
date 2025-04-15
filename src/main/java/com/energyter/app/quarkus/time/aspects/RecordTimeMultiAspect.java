package com.energyter.app.quarkus.time.aspects;

import com.energyter.app.quarkus.time.annotations.RecordTimeMultiAnnotation;
import com.energyter.app.quarkus.time.annotations.RecordTimeUniAnnotation;
import io.smallrye.mutiny.Multi;
import org.jboss.logging.Logger;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@RecordTimeMultiAnnotation
@Priority(Interceptor.Priority.APPLICATION)
public class RecordTimeMultiAspect {

    private static final Logger LOG = Logger.getLogger(RecordTimeMultiAspect.class);

    @AroundInvoke
    public Object measureExecutionTime(InvocationContext context) throws Throwable {
        if (context.getMethod().getReturnType() == Multi.class) {
            LOG.info("Starting reactive Multi method: " + context.getMethod().getName());
            long start = System.currentTimeMillis();

            Multi<?> result = (Multi<?>) context.proceed();

            return result.onItem().invoke(item -> {
                LOG.debug("Multi emitted item in method: " + context.getMethod().getName());
            }).onCompletion().invoke(() -> {
                long executionTime = System.currentTimeMillis() - start;
                LOG.info("Multi stream completed in " + executionTime + "ms: " + context.getMethod().getName());
            }).onFailure().invoke(failure -> {
                long executionTime = System.currentTimeMillis() - start;
                LOG.error("Multi stream failed after " + executionTime + "ms: " + context.getMethod().getName(), failure);
            });
        }

        // For non-Multi methods, proceed normally
        return context.proceed();
    }
}
