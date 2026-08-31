package com.anshun.dms.service;

import com.anshun.dms.model.statistics.StatisticsModels;
import com.anshun.dms.repository.StatisticsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {
    @Mock StatisticsRepository repository;
    @InjectMocks StatisticsService statisticsService;

    @Test
    void overviewCalculatesLeaseRateAndAggregatesRepositoryValues() {
        given(repository.countPositions()).willReturn(121L);
        given(repository.countLeasedPositions()).willReturn(81L);
        given(repository.totalRevenue()).willReturn(new BigDecimal("14857.23"));
        given(repository.arrearsAmount()).willReturn(new BigDecimal("6919.16"));

        StatisticsModels.Overview result = statisticsService.overview();

        assertThat(result.totalPositions()).isEqualTo(121);
        assertThat(result.leasedPositions()).isEqualTo(81);
        assertThat(result.leasedRate()).isEqualByComparingTo("66.94");
        assertThat(result.totalRevenue()).isEqualByComparingTo("14857.23");
    }
}
