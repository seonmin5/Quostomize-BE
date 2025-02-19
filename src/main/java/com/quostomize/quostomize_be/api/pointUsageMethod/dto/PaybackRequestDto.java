package com.quostomize.quostomize_be.api.pointUsageMethod.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaybackRequestDto(
        @Positive Long pointUsageTypeId,
        @NotNull Long cardSequenceId,
        @NotNull Boolean isPayback
) {}