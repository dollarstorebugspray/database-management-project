package com.grocerystore.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.grocerystore.backend.dto.ReportFilter;
import com.grocerystore.backend.exception.BadRequestException;

class ReportServiceTests {

    @Test
    void filterRejectsAnInvertedDateRange() {
        assertThatThrownBy(() -> new ReportFilter(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 10, 1), null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("from must be on or before to");
    }

    @Test
    void collectedCountsOnlyPaidStatuses() {
        assertThat(ReportService.PAID_STATUSES).isEqualTo("('PAID', 'PROCESSING', 'COMPLETED')");
    }

    @Test
    void percentAndAverageHandleZeroTotals() {
        assertThat(ReportService.percent(new BigDecimal("25"), new BigDecimal("200")))
                .isEqualByComparingTo("12.5");
        assertThat(ReportService.percent(BigDecimal.TEN, BigDecimal.ZERO)).isEqualByComparingTo("0");
        assertThat(ReportService.average(new BigDecimal("100.00"), 3)).isEqualByComparingTo("33.33");
        assertThat(ReportService.average(BigDecimal.TEN, 0)).isEqualByComparingTo("0");
    }
}
