package com.energyter.app.quarkus.time.aspects;

import com.energyter.app.quarkus.time.annotations.RecordTimeAnnotation;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@RecordTimeAnnotation
@Priority(Interceptor.Priority.APPLICATION)
public class RecordTimeUniAspect {

    private static final Logger LOG = Logger.getLogger(RecordTimeUniAspect.class);

    @AroundInvoke
    public Object measureExecutionTime(InvocationContext context) throws Throwable {
        if (context.getMethod().getReturnType() == Uni.class) {
            LOG.info("Starting reactive method: " + context.getMethod().getName());
            long start = System.currentTimeMillis();

            Uni<?> result = (Uni<?>) context.proceed();

            // Transform the Uni to include timing
            return result.onItem().invoke(item -> {
                long executionTime = System.currentTimeMillis() - start;
                LOG.info("Reactive method completed in " + executionTime + "ms: " + context.getMethod().getName());
            }).onFailure().invoke(failure -> {
                long executionTime = System.currentTimeMillis() - start;
                LOG.error("Reactive method failed after " + executionTime + "ms: " + context.getMethod().getName(), failure);
            });
        }

        // For non-Uni methods, proceed normally
        return context.proceed();
    }
}
