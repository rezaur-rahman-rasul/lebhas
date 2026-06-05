package com.lebhas.ai.creative.enums;

import com.lebhas.ai.creative.enums.ModelQuality;

public enum CreativeQuality {
    low,
    medium,
    high;

    public static CreativeQuality fromModelQuality(ModelQuality quality) {
        return quality == ModelQuality.PREMIUM ? high : medium;
    }
}
