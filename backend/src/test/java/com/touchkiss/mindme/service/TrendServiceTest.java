package com.touchkiss.mindme.service;

import com.touchkiss.mindme.domain.TrendItem;
import com.touchkiss.mindme.repository.TrendRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TrendServiceTest {

    @Mock
    private TrendRepository trendRepository;

    @Mock
    private AiAnalysisService aiService;

    @InjectMocks
    private TrendService trendService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetTrendsWithAiFilter() {
        TrendItem item1 = new TrendItem("Source A", "Title 1", "http://a.com", 100);
        item1.setAiScore(50);

        when(trendRepository.findTopRelevant(any(Pageable.class)))
                .thenReturn(Collections.singletonList(item1));

        List<TrendItem> result = trendService.getTrends(true);

        assertEquals(1, result.size());
        assertEquals("Title 1", result.get(0).getTitle());
        verify(trendRepository).findTopRelevant(any(Pageable.class));
    }

    @Test
    void testGetTrendsWithoutAiFilter() {
        TrendItem item1 = new TrendItem("Source B", "Title 2", "http://b.com", 200);

        when(trendRepository.findTopPopular(any(Pageable.class)))
                .thenReturn(Collections.singletonList(item1));

        List<TrendItem> result = trendService.getTrends(false);

        assertEquals(1, result.size());
        assertEquals("Title 2", result.get(0).getTitle());
        verify(trendRepository).findTopPopular(any(Pageable.class));
    }
}
