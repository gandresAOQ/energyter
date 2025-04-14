package com.energyter.app.aspectj.time.impl;

import com.energyter.app.aspectj.time.MeasureTime;

public class MeasureTimeImpl implements MeasureTime {
    @Override
    public long getCurrentTimeInMillis() {
        return System.currentTimeMillis();
    }
}
