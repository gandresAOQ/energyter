package com.energyter.app.quarkus.time.impl;

import com.energyter.app.aspectj.time.MeasureTime;

public class MeasureTimeImpl implements MeasureTime {
    @Override
    public long getCurrentTimeInMillis() {
        return System.nanoTime() / 1000000;
    }
}
