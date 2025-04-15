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
            double startSystemCpuLoad = getSystemCpuLoad(osMXBean);

            AtomicReference<Double> peakProcessCpuLoad = new AtomicReference<>(startProcessCpuLoad);
            AtomicReference<Double> peakSystemCpuLoad = new AtomicReference<>(startSystemCpuLoad);

            AtomicBoolean monitoring = new AtomicBoolean(false);

            Thread monitorThread = createMonitorThread(osMXBean, peakProcessCpuLoad, peakSystemCpuLoad, monitoring);

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
                        double endSystemCpuLoad = getSystemCpuLoad(osMXBean);

                        long cpuTimeNanos = endThreadCpuTime - startThreadCpuTime;
                        double cpuTimeMs = cpuTimeNanos / 1_000_000.0;

                        LOG.infof("CPU usage for %s.%s:\n" +
                                        "  - Thread CPU time: %f ms\n" +
                                        "  - Process CPU load: start=%f, end=%f, peak=%f \n" +
                                        "  - System CPU load: start=%f, end=%f, peak=%f",
                                context.getTarget().getClass().getSimpleName(),
                                context.getMethod().getName(),
                                cpuTimeMs,
                                startProcessCpuLoad * 100,
                                endProcessCpuLoad * 100,
                                peakProcessCpuLoad.get() * 100,
                                startSystemCpuLoad * 100,
                                endSystemCpuLoad * 100,
                                peakSystemCpuLoad.get() * 100);
                    });
        }
        return context.proceed();
    }

    private Thread createMonitorThread(OperatingSystemMXBean osMXBean, AtomicReference<Double> peakCpuLoad, AtomicReference<Double> peakSystemCpuLoad, AtomicBoolean monitoring) {
        Thread thread = new Thread(() -> {
            while (monitoring.get()) {
                double currentLoad = getProcessCpuLoad(osMXBean);
                double currentSystemLoad = getSystemCpuLoad(osMXBean);
                double currentPeak = peakCpuLoad.get();
                if (currentLoad > currentPeak) {
                    peakCpuLoad.compareAndSet(currentPeak, currentLoad);
                }

                double currentSystemPeak = peakSystemCpuLoad.get();
                if (currentSystemLoad > currentSystemPeak) {
                    peakSystemCpuLoad.compareAndSet(currentSystemPeak, currentSystemLoad);
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
    private double getSystemCpuLoad(OperatingSystemMXBean osMXBean) {
        try {
            // Try using the com.sun.management API first
            Method method = osMXBean.getClass().getMethod("getSystemCpuLoad");
            double load = (double) method.invoke(osMXBean);
            if (load >= 0) return load;

            // If that returns negative value, fall back to system load average
            return osMXBean.getSystemLoadAverage() / osMXBean.getAvailableProcessors();
        } catch (Exception e) {
            // Use getSystemLoadAverage as fallback
            double loadAvg = osMXBean.getSystemLoadAverage();
            return loadAvg >= 0 ? loadAvg / osMXBean.getAvailableProcessors() : 0;
        }
    }


}
