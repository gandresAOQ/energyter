package com.energyter.app.quarkus.memory.aspects;

import com.energyter.app.quarkus.memory.annotations.RecordMemoryMultiAnnotation;

import javax.annotation.Priority;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@RecordMemoryMultiAnnotation
@Priority(Interceptor.Priority.APPLICATION)
public class RecordMemoryMultiAspect {

    public Object measureMemory(InvocationContext context) throws Throwable {
        throw new Exception("NOT_IMPLEMENTED");
    }
}
