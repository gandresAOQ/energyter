package com.energyter.app.time.impl;

import com.energyter.app.time.MeasureTime;

public class MeasureTimeImpl implements MeasureTime {
    @Override
    public long getCurrentTimeInMillis() {
        return System.currentTimeMillis();
    }
}
