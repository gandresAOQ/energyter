package com.energyter.app.aspectj.factory;

import com.energyter.app.aspectj.time.impl.MeasureTimeImpl;
import com.energyter.app.aspectj.time.MeasureTime;

public class EnergyterFactory {

    public static MeasureTime createMeasureTime() {
        return new MeasureTimeImpl();
    }
}
