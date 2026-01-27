package com.touchkiss.mindme.service;

import com.touchkiss.mindme.domain.ActivityRecord;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VectorSearchServiceTest {

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @Mock
    private EmbeddingModel embeddingModel;

    private VectorSearchService vectorSearchService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        vectorSearchService = new VectorSearchService(embeddingStore, embeddingModel);

        // Mock embedding model behavior
        when(embeddingModel.embed(any(TextSegment.class)))
                .thenReturn(Response.from(new Embedding(new float[] { 0.1f, 0.2f })));
    }

    @Test
    void testIndexActivityWithShortContent() {
        ActivityRecord record = new ActivityRecord();
        record.setId(UUID.randomUUID());
        record.setUrl("http://example.com/short");
        record.setTitle("Short Title");
        record.setContentSummary("This is a short summary.");
        record.setVisitTime(ZonedDateTime.now());

        vectorSearchService.indexActivity(record);

        // Should be called once for embedding and once for storage
        verify(embeddingModel, times(1)).embed(any(TextSegment.class));
        verify(embeddingStore, times(1)).add(any(Embedding.class), any(TextSegment.class));
    }

    @Test
    void testIndexActivityWithLongContent() {
        ActivityRecord record = new ActivityRecord();
        record.setId(UUID.randomUUID());
        record.setUrl("http://example.com/long");
        record.setTitle("Long Title");
        record.setVisitTime(ZonedDateTime.now());

        // Generate a long content string (> 2000 chars)
        // 2500 chars should result in 2 segments (2000 split size)
        String longContent = String.join("", Collections.nCopies(250, "0123456789"));
        record.setContentSummary(longContent);

        vectorSearchService.indexActivity(record);

        // Verify that split happened (more than 1 call)
        ArgumentCaptor<TextSegment> segmentCaptor = ArgumentCaptor.forClass(TextSegment.class);
        verify(embeddingModel, atLeast(2)).embed(segmentCaptor.capture());

        List<TextSegment> capturedSegments = segmentCaptor.getAllValues();
        assertTrue(capturedSegments.size() >= 2, "Should have split into at least 2 segments");

        // Verify storage calls
        verify(embeddingStore, atLeast(2)).add(any(Embedding.class), any(TextSegment.class));
    }
}
