package com.energyter.app.quarkus.cpu.ascpects;


import com.energyter.app.quarkus.cpu.annotations.RecordCpuUsageUniAnnotation;
import com.energyter.app.quarkus.memory.aspects.RecordMemoryUniAspect;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Interceptor
@RecordCpuUsageUniAnnotation
@Priority(Interceptor.Priority.APPLICATION)
public class RecordCpuUsageUniAspect {

    private static final Logger LOG = Logger.getLogger(RecordCpuUsageUniAspect.class);

    @AroundInvoke
    public Object measureCpuUsage(InvocationContext context) throws Throwable {
        if (context.getMethod().getReturnType() == Uni.class) {
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            if (!threadMXBean.isThreadCpuTimeEnabled()) {
                threadMXBean.setThreadCpuTimeEnabled(true);
            }

            OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
            long startThreadCpuTime = threadMXBean.getCurrentThreadCpuTime();
            double startProcessCpuLoad = getProcessCpuLoad(osMXBean);

            AtomicReference<Double> peakCpuLoad = new AtomicReference<>(startProcessCpuLoad);
            AtomicBoolean monitoring = new AtomicBoolean(false);

            Thread monitorThread = createMonitorThread(osMXBean, peakCpuLoad, monitoring);

            Uni<?> result = (Uni<?>) context.proceed();

            return result
                    .onSubscription().invoke(() -> {
                        monitoring.set(true);
                        monitorThread.start();
                    })
                    .onTermination().invoke(() -> {
                        monitoring.set(false);
                        try {
                            monitorThread.join(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }

                        long endThreadCpuTime = threadMXBean.getCurrentThreadCpuTime();
                        double endProcessCpuLoad = getProcessCpuLoad(osMXBean);

                        long cpuTimeNanos = endThreadCpuTime - startThreadCpuTime;
                        double cpuTimeMs = cpuTimeNanos / 1_000_000.0;

                        LOG.infof("Reactive CPU usage for %s.%s: Thread CPU time=%f ms, Process CPU load: start=%d, end=%d, peak=%d",
                                context.getTarget().getClass().getSimpleName(),
                                context.getMethod().getName(),
                                cpuTimeMs,
                                startProcessCpuLoad * 100,
                                endProcessCpuLoad * 100,
                                peakCpuLoad.get().doubleValue() * 100);
                    });
        }
        return context.proceed();
    }

    private Thread createMonitorThread(OperatingSystemMXBean osMXBean, AtomicReference<Double> peakCpuLoad, AtomicBoolean monitoring) {
        Thread thread = new Thread(() -> {
            while (monitoring.get()) {
                double currentLoad = getProcessCpuLoad(osMXBean);
                double currentPeak = peakCpuLoad.get();
                if (currentLoad > currentPeak) {
                    peakCpuLoad.compareAndSet(currentPeak, currentLoad);
                }

                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        thread.setDaemon(true);
        return thread;
    }

    private double getProcessCpuLoad(OperatingSystemMXBean osMXBean) {
        try {
            Method method = osMXBean.getClass().getMethod("getProcessCpuLoad");
            return (double) method.invoke(osMXBean);
        } catch (Exception e) {
            return osMXBean.getSystemLoadAverage() / osMXBean.getAvailableProcessors();
        }
    }

}
