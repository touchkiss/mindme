package com.touchkiss.mindme.service;

import com.touchkiss.mindme.domain.ActivityRecord;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorSearchService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    @Async
    public void indexActivity(ActivityRecord record) {
        if (record.getContentSummary() == null || record.getContentSummary().isEmpty()) {
            return;
        }

        try {
            Metadata metadata = new Metadata();
            metadata.put("url", record.getUrl());
            metadata.put("title", record.getTitle() != null ? record.getTitle() : "");
            metadata.put("timestamp", record.getVisitTime() != null ? record.getVisitTime().toString() : "");
            metadata.put("recordId", record.getId().toString());

            // Create text segment from summary (or full content if available)
            String content = record.getContentSummary();
            // Append title for better context
            String text = "Title: " + record.getTitle() + "\n" + content;

            dev.langchain4j.data.document.Document document = dev.langchain4j.data.document.Document.from(text,
                    metadata);
            // Split document into segments
            // Split into segments of 2000 chars with 200 overlap to stay well within 4096
            // token limit
            // (assuming 1 token ~= 4 chars, 2000 chars is ~500 tokens, safe margin)

            int maxSegmentSizeInChars = 2000;
            int maxOverlapSizeInChars = 200;

            // Note: We might want to make these configurable or use TokenTextSplitter if
            // precise token count is needed
            // But Recursive character splitter is generally good for semantic chunks.
            List<TextSegment> segments = dev.langchain4j.data.document.splitter.DocumentSplitters.recursive(
                    maxSegmentSizeInChars,
                    maxOverlapSizeInChars).split(document);

            // Generate embedding and store for each segment
            for (TextSegment segment : segments) {
                dev.langchain4j.data.embedding.Embedding embedding = embeddingModel.embed(segment).content();
                embeddingStore.add(embedding, segment);
            }

            log.info("Indexed activity: {}", record.getUrl());
        } catch (Exception e) {
            log.error("Failed to index activity: {}", record.getUrl(), e);
        }
    }

    public List<String> searchSimilar(String query, int maxResults) {
        dev.langchain4j.data.embedding.Embedding queryEmbedding = embeddingModel.embed(query).content();

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

        return result.matches().stream()
                .map(match -> match.embedded().text())
                .collect(Collectors.toList());
    }
}
