package com.touchkiss.mindme.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.touchkiss.mindme.domain.TrendItem;
import com.touchkiss.mindme.repository.TrendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrendService {

    private final TrendRepository trendRepository;
    private final AiAnalysisService aiService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient client = new OkHttpClient();
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    /**
     * Get trends, optionally filtered and sorted by AI relevance.
     */
    public List<TrendItem> getTrends(boolean aiFilter) {
        if (aiFilter) {
            return trendRepository.findTopRelevant(PageRequest.of(0, 20));
        } else {
            return trendRepository.findTopPopular(PageRequest.of(0, 20));
        }
    }

    /**
     * Run every 5 minutes
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void fetchAndProcessTrends() {
        log.info("Starting scheduled trend fetch...");

        List<CompletableFuture<List<TrendItem>>> futures = new ArrayList<>();
        // futures.add(CompletableFuture.supplyAsync(this::fetchHackerNews, executor));
        futures.add(CompletableFuture.supplyAsync(this::fetchITHome, executor));
        futures.add(CompletableFuture.supplyAsync(this::fetchZhihu, executor));
        futures.add(CompletableFuture.supplyAsync(this::fetchGitHub, executor));
        futures.add(CompletableFuture.supplyAsync(this::fetchV2EX, executor));
        futures.add(CompletableFuture.supplyAsync(this::fetchJuejinBoilingPoint, executor));

        List<TrendItem> fetchedItems = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        if (fetchedItems.isEmpty()) {
            log.warn("No trends fetched.");
            return;
        }

        // 1. Filter out duplicates (already in DB)
        List<TrendItem> newItems = fetchedItems.stream()
                .filter(item -> trendRepository.findByUrl(item.getUrl()).isEmpty())
                .collect(Collectors.toList());

        if (newItems.isEmpty()) {
            log.info("No new trends found.");
            return;
        }

        log.info("Found {} new items. Scoring with AI...", newItems.size());

        // 2. Score with AI (Batch processing for efficiency)
        // Note: For simplicity, we score in batches of 30 or less
        Map<String, Integer> scores = new HashMap<>();
        List<String> validTitles = newItems.stream().map(TrendItem::getTitle).collect(Collectors.toList());

        // This could be optimized to chunk validTitles if list is very large
        if (!validTitles.isEmpty()) {
            scores = aiService.scoreRelevance(validTitles);
        }

        // 3. Save to DB
        for (TrendItem item : newItems) {
            int relevance = scores.getOrDefault(item.getTitle(), 0);
            item.setAiScore(relevance);
            try {
                trendRepository.save(item);
            } catch (Exception e) {
                // Ignore duplicate key errors if race condition
                log.warn("Failed to save item: {}", item.getUrl());
            }
        }

        log.info("Successfully processed and saved {} new trends.", newItems.size());
    }

    // --- Private Fetch Methods (Migrated from Controller) ---

    private List<TrendItem> fetchHackerNews() {
        try {
            Request request = new Request.Builder()
                    .url("https://hacker-news.firebaseio.com/v0/topstories.json")
                    .build();

            List<Integer> topIds;
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null)
                    return Collections.emptyList();
                topIds = mapper.readValue(response.body().string(),
                        mapper.getTypeFactory().constructCollectionType(List.class, Integer.class));
            }

            return topIds.stream()
                    .limit(5) // Limit to 5 per source
                    .map(this::fetchHNItem)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("HN Fetch Error", e);
            return Collections.emptyList();
        }
    }

    private TrendItem fetchHNItem(Integer id) {
        try {
            Request request = new Request.Builder()
                    .url("https://hacker-news.firebaseio.com/v0/item/" + id + ".json")
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null)
                    return null;
                JsonNode node = mapper.readTree(response.body().string());
                String title = node.has("title") ? node.get("title").asText() : "No Title";
                String url = node.has("url") ? node.get("url").asText() : null;
                return new TrendItem("HackerNews", title, url, 80); // Default high score for HN
            }
        } catch (Exception e) {
            return null;
        }
    }

    private List<TrendItem> fetchITHome() {
        try {
            Request request = new Request.Builder().url("https://www.ithome.com/rss/").build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null)
                    return Collections.emptyList();

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new ByteArrayInputStream(response.body().bytes()));

                NodeList items = doc.getElementsByTagName("item");
                List<TrendItem> result = new ArrayList<>();
                for (int i = 0; i < Math.min(items.getLength(), 5); i++) {
                    Element element = (Element) items.item(i);
                    String title = getTagValue("title", element);
                    String link = getTagValue("link", element);
                    result.add(new TrendItem("ITHome", title, link, 70));
                }
                return result;
            }
        } catch (Exception e) {
            log.error("ITHome Fetch Error", e);
            return Collections.emptyList();
        }
    }

    private List<TrendItem> fetchZhihu() {
        try {
            Request request = new Request.Builder()
                    .url("https://www.zhihu.com/api/v3/feed/topstory/hot-lists/total?limit=10")
                    .header("User-Agent", "Mozilla/5.0")
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null)
                    return Collections.emptyList();
                JsonNode root = mapper.readTree(response.body().string());
                List<TrendItem> result = new ArrayList<>();
                if (root.has("data")) {
                    for (JsonNode item : root.get("data")) {
                        if (item.has("target") && item.get("target").has("title")) {
                            String title = item.get("target").get("title").asText();
                            String url = "https://www.zhihu.com/question/" + item.get("target").get("id").asText();
                            result.add(new TrendItem("Zhihu", title, url, 60));
                        }
                    }
                }
                return result.subList(0, Math.min(result.size(), 5));
            }
        } catch (Exception e) {
            log.error("Zhihu Fetch Error", e);
            return Collections.emptyList();
        }
    }

    private List<TrendItem> fetchGitHub() {
        try {
            String date = java.time.LocalDate.now().minusDays(3).toString();
            Request request = new Request.Builder()
                    .url("https://api.github.com/search/repositories?q=created:>" + date
                            + "&sort=stars&order=desc&per_page=5")
                    .header("User-Agent", "MindMe-Backend")
                    .build();
            try (Response response = client.newCall(request).execute()) {
                try {
                    if (!response.isSuccessful() || response.body() == null)
                        return Collections.emptyList();
                    JsonNode root = mapper.readTree(response.body().string());
                    List<TrendItem> result = new ArrayList<>();
                    if (root.has("items")) {
                        for (JsonNode item : root.get("items")) {
                            String title = item.get("full_name").asText() + ": " + item.get("description").asText();
                            String url = item.get("html_url").asText();
                            result.add(new TrendItem("GitHub", title, url, 85));
                        }
                    }
                    return result;
                } catch (Exception e) {
                    return Collections.emptyList();
                }
            }
        } catch (Exception e) {
            log.error("GitHub Fetch Error", e);
            return Collections.emptyList();
        }
    }

    private List<TrendItem> fetchV2EX() {
        try {
            Request request = new Request.Builder()
                    .url("https://www.v2ex.com/api/topics/hot.json")
                    .header("User-Agent", "MindMe-Backend/1.0")
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null)
                    return Collections.emptyList();
                JsonNode root = mapper.readTree(response.body().string());
                List<TrendItem> result = new ArrayList<>();
                if (root.isArray()) {
                    for (JsonNode item : root) {
                        if (result.size() >= 5)
                            break;
                        String title = item.has("title") ? item.get("title").asText() : null;
                        String url = item.has("url") ? item.get("url").asText() : null;
                        if (title != null && url != null) {
                            result.add(new TrendItem("V2EX", title, url, 75));
                        }
                    }
                }
                return result;
            }
        } catch (Exception e) {
            log.error("V2EX Fetch Error", e);
            return Collections.emptyList();
        }
    }

    private List<TrendItem> fetchJuejinBoilingPoint() {
        try {
            String requestBody = "{\"id_type\":4,\"sort_type\":200,\"cursor\":\"0\",\"limit\":10}";
            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                    requestBody,
                    okhttp3.MediaType.parse("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url("https://api.juejin.cn/recommend_api/v1/short_msg/hot")
                    .header("User-Agent", "MindMe-Backend/1.0")
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null)
                    return Collections.emptyList();
                JsonNode root = mapper.readTree(response.body().string());
                List<TrendItem> result = new ArrayList<>();
                if (root.has("data") && root.get("data").isArray()) {
                    for (JsonNode item : root.get("data")) {
                        if (result.size() >= 5)
                            break;
                        JsonNode msgInfo = item.has("msg_Info") ? item.get("msg_Info") : item.get("msg_info");
                        if (msgInfo != null) {
                            String content = msgInfo.has("content") ? msgInfo.get("content").asText() : null;
                            String msgId = msgInfo.has("msg_id") ? msgInfo.get("msg_id").asText() : null;
                            if (content != null && !content.isEmpty()) {
                                String title = content.length() > 50 ? content.substring(0, 50) + "..." : content;
                                String url = msgId != null ? "https://juejin.cn/pin/" + msgId
                                        : "https://juejin.cn/pins";
                                result.add(new TrendItem("掘金沸点", title, url, 72));
                            }
                        }
                    }
                }
                return result;
            }
        } catch (Exception e) {
            log.error("Juejin Boiling Point Fetch Error", e);
            return Collections.emptyList();
        }
    }

    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag).item(0).getChildNodes();
        if (nodeList.getLength() > 0)
            return nodeList.item(0).getNodeValue();
        return "";
    }
}
