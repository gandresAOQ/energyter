package com.energyter.app.factory;

import com.energyter.app.time.MeasureTime;
import com.energyter.app.time.impl.MeasureTimeImpl;

public class EnergyterFactory {

    public static MeasureTime createMeasureTime() {
        return new MeasureTimeImpl();
    }
}
