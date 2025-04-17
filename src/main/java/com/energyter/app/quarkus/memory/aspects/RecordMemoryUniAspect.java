package com.energyter.app.quarkus.memory.aspects;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import com.energyter.app.mongodb.services.InsertOneDocument;
import com.energyter.app.mongodb.services.impl.InsertOneDocumentImpl;
import com.energyter.app.quarkus.memory.annotations.RecordMemoryUniAnnotation;
import io.smallrye.mutiny.Uni;
import org.bson.Document;
import org.jboss.logging.Logger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Interceptor
@RecordMemoryUniAnnotation
@Priority(Interceptor.Priority.APPLICATION)
public class RecordMemoryUniAspect {

    private static final Logger LOG = Logger.getLogger(RecordMemoryUniAspect.class);

    @AroundInvoke
    public Object measureMemory(InvocationContext context) throws Throwable {
        if (context.getMethod().getReturnType() == Uni.class) {
            System.gc();
            Runtime runtime = Runtime.getRuntime();
            long initialUsed = runtime.totalMemory() - runtime.freeMemory();
            AtomicLong peakMemory = new AtomicLong(initialUsed);
            AtomicBoolean monitoring = new AtomicBoolean(false);

            Thread monitorThread = createMonitorThread(runtime, peakMemory, monitoring);

            Uni<?> result = (Uni<?>) context.proceed();
            InsertOneDocument insertOneDocument = new InsertOneDocumentImpl();

            return result
                    .onSubscription().invoke(() -> {
                        // Start monitoring when subscription happens
                        monitoring.set(true);
                        monitorThread.start();
                    })
                    .onTermination().invoke(() -> {
                        // Stop monitoring when Uni terminates (with item or failure)
                        stopMonitoring(monitoring, monitorThread);

                        System.gc();
                        long finalUsed = runtime.totalMemory() - runtime.freeMemory();

                        LOG.infof("Reactive memory usage for %s.%s: Initial=%d bytes, Final=%d bytes, Peak=%d bytes, Delta=%d bytes",
                                context.getTarget().getClass().getSimpleName(),
                                context.getMethod().getName(),
                                initialUsed,
                                finalUsed,
                                peakMemory.get(),
                                finalUsed - initialUsed);
                        insertOneDocument.insertOne(new Document()
                                .append("class", context.getTarget().getClass().getSimpleName())
                                .append("method", context.getMethod().getName())
                                .append("measure", "memory")
                                .append("initial", initialUsed)
                                .append("final", finalUsed)
                                .append("peak", peakMemory.get())
                                .append("delta", finalUsed - initialUsed));
                    });
        }
        return context.proceed();
    }

    private Thread createMonitorThread(Runtime runtime, AtomicLong peakMemory, AtomicBoolean monitoring) {
        Thread thread = new Thread(() -> {
            while (monitoring.get()) {
                long used = runtime.totalMemory() - runtime.freeMemory();
                long currentPeak = peakMemory.get();
                if (used > currentPeak) {
                    peakMemory.compareAndSet(currentPeak, used);
                }

                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        thread.setDaemon(true);
        return thread;
    }

    private void stopMonitoring(AtomicBoolean monitoring, Thread monitorThread) {
        monitoring.set(false);
        try {
            monitorThread.join(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
