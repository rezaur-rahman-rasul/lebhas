package com.lebhas.creativesaas.texttool.application;

import java.util.Map;

public interface CreativeTextToolProvider {

    Map<String, Object> generate(TextToolGenerationContext context);
}
