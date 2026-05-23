package com.lebhas.creativesaas.generation.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.generation.domain.CreativeOutputFormat;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CreativeGenerationValidator {

    private static final int MAX_PROMPT_LENGTH = 32_000;
    private static final int MIN_IMAGE_SIDE = 256;
    private static final int MAX_IMAGE_SIDE = 4096;
    private static final long MAX_IMAGE_PIXELS = 8_388_608L;
    private static final long MIN_VIDEO_DURATION_SECONDS = 1;
    private static final long MAX_VIDEO_DURATION_SECONDS = 90;

    private final CreativeGenerationActivityLogger activityLogger;

    public CreativeGenerationValidator(CreativeGenerationActivityLogger activityLogger) {
        this.activityLogger = activityLogger;
    }

    public void validate(CreativeGenerationContext context, java.util.UUID workspaceId, java.util.UUID actorUserId) {
        try {
            validateRequired(context);
            validatePrompt(context);
            validateFormat(context.creativeType(), context.outputFormat());
            validateLanguage(context.language());
            validateDimensions(context);
            validateDuration(context);
            validateGenerationConfig(context.generationConfigJson());
        } catch (BusinessException exception) {
            activityLogger.logValidationFailure(workspaceId, actorUserId, exception.getMessage());
            throw exception;
        }
    }

    private void validateRequired(CreativeGenerationContext context) {
        if (context.creativeType() == null) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "creativeType is required");
        }
        if (context.platform() == null) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "platform is required");
        }
        if (context.campaignObjective() == null) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "campaignObjective is required");
        }
        if (context.outputFormat() == null) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "outputFormat is required");
        }
        if (context.language() == null) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "language is required");
        }
    }

    private void validatePrompt(CreativeGenerationContext context) {
        if (!StringUtils.hasText(context.providerPrompt())) {
            throw new BusinessException(ErrorCode.PROMPT_LENGTH_INVALID, "sourcePrompt, enhancedPrompt, or promptHistoryId is required");
        }
        int sourceLength = context.sourcePrompt() == null ? 0 : context.sourcePrompt().length();
        int enhancedLength = context.enhancedPrompt() == null ? 0 : context.enhancedPrompt().length();
        if (sourceLength > MAX_PROMPT_LENGTH || enhancedLength > MAX_PROMPT_LENGTH || context.providerPrompt().length() > MAX_PROMPT_LENGTH) {
            throw new BusinessException(ErrorCode.PROMPT_LENGTH_INVALID, "Prompt length must be " + MAX_PROMPT_LENGTH + " characters or fewer");
        }
    }

    private void validateFormat(CreativeType creativeType, CreativeOutputFormat outputFormat) {
        if (creativeType.isImage() && !outputFormat.isImage()) {
            throw new BusinessException(ErrorCode.GENERATION_UNSUPPORTED_FORMAT);
        }
        if (creativeType.isVideo() && !outputFormat.isVideo()) {
            throw new BusinessException(ErrorCode.GENERATION_UNSUPPORTED_FORMAT);
        }
    }

    private void validateLanguage(PromptLanguage language) {
        if (language != PromptLanguage.ENGLISH && language != PromptLanguage.BANGLA) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "Only ENGLISH and BANGLA generation are supported");
        }
    }

    private void validateDimensions(CreativeGenerationContext context) {
        if (context.creativeType().isVideo()) {
            return;
        }
        Integer width = context.width();
        Integer height = context.height();
        if (width == null && height == null) {
            return;
        }
        if (width == null || height == null) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "Both width and height are required when either is provided");
        }
        if (width < MIN_IMAGE_SIDE || height < MIN_IMAGE_SIDE || width > MAX_IMAGE_SIDE || height > MAX_IMAGE_SIDE) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "Image width and height must be between 256 and 4096 pixels");
        }
        if ((long) width * height > MAX_IMAGE_PIXELS) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "Generated image dimensions are too large");
        }
    }

    private void validateDuration(CreativeGenerationContext context) {
        if (!context.creativeType().isVideo()) {
            if (context.duration() != null) {
                throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "duration is only supported for video creative types");
            }
            return;
        }
        Long duration = context.duration();
        if (duration == null) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "duration is required for video creative types");
        }
        if (duration < MIN_VIDEO_DURATION_SECONDS || duration > MAX_VIDEO_DURATION_SECONDS) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "Video duration must be between 1 and 90 seconds");
        }
    }

    private void validateGenerationConfig(String generationConfigJson) {
        if (generationConfigJson != null && generationConfigJson.length() > 12_000) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "Generation config is too large");
        }
    }
}
