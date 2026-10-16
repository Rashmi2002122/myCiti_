package com.myciti.dto;

import java.time.Instant;

public record UserLocationDto(
    Double coarseLat,
    Double coarseLng,
    String areaName,
    Instant locationUpdatedAt
) {}
