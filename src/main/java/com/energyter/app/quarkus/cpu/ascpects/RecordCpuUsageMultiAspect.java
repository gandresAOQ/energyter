package com.energyter.app.quarkus.cpu.ascpects;


import com.energyter.app.quarkus.cpu.annotations.RecordCpuUsageMultiAnnotation;
import com.energyter.app.quarkus.memory.annotations.RecordMemoryMultiAnnotation;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@RecordCpuUsageMultiAnnotation
@Priority(Interceptor.Priority.APPLICATION)
public class RecordCpuUsageMultiAspect {


    @AroundInvoke
    public Object measureCpuUsage(InvocationContext context) throws Throwable {
        throw new Exception("NOT_IMPLEMENTED");
    }

}
