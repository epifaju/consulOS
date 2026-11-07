package com.consulos.encaissement;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class EncaissementDayBoundsTest {

    @Test
    void startOfDay_usesJvmZoneNotUtcWhenOffsetDiffers() {
        LocalDate date = LocalDate.of(2026, 11, 7);
        OffsetDateTime start = EncaissementDayBounds.startOfDay(date);
        OffsetDateTime utcMidnight = date.atStartOfDay().atOffset(ZoneOffset.UTC);

        assertThat(start).isEqualTo(date.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime());
        if (!ZoneOffset.UTC.equals(ZoneId.systemDefault().getRules().getOffset(date.atStartOfDay()))) {
            assertThat(start).isNotEqualTo(utcMidnight);
        }
    }

    @Test
    void toLocal_mapsUtcInstantToJvmLocalDateTime() {
        OffsetDateTime utc = OffsetDateTime.of(2026, 11, 6, 23, 0, 0, 0, ZoneOffset.UTC);
        assertThat(EncaissementDayBounds.toLocal(utc))
                .isEqualTo(utc.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());
    }
}
