package com.consulos.dashboard.dto;

import java.time.LocalDate;

public record ChartPointDTO(
        LocalDate date,
        long count
) {
}
