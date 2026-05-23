package com.lebhas.creativesaas.generatedversion.application;

import com.lebhas.creativesaas.generatedversion.application.dto.GeneratedVersionView;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import org.springframework.stereotype.Component;

@Component
public class GeneratedVersionMapper {

    private final GeneratedVersionViewMapper viewMapper;

    public GeneratedVersionMapper(GeneratedVersionViewMapper viewMapper) {
        this.viewMapper = viewMapper;
    }

    public GeneratedVersionView toView(GeneratedVersionEntity entity) {
        return viewMapper.toView(entity);
    }
}
