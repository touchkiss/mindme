package com.touchkiss.mindme.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.touchkiss.mindme.service.AiAnalysisService;
import com.touchkiss.mindme.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
@RestController
@RequestMapping("/api/trends")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TrendController {

    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient client = new OkHttpClient();
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final AiAnalysisService aiService;
    private final RedisService redisService;

    @GetMapping
    public List<TrendItem> getTrends(@RequestParam(defaultValue = "false") boolean aiFilter) {
        try {
            String cacheKey = "trend_results_" + aiFilter;
            String cached = redisService.get(cacheKey);
            if (cached != null) {
                return mapper.readValue(cached, new TypeReference<List<TrendItem>>() {
                });
            }

            List<CompletableFuture<List<TrendItem>>> futures = new ArrayList<>();

            // Fetch from multiple sources in parallel
            futures.add(CompletableFuture.supplyAsync(this::fetchHackerNews, executor));
            futures.add(CompletableFuture.supplyAsync(this::fetchITHome, executor));
            futures.add(CompletableFuture.supplyAsync(this::fetchZhihu, executor));
            futures.add(CompletableFuture.supplyAsync(this::fetchGitHub, executor));

            List<TrendItem> allItems = futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            if (allItems.isEmpty())
                return Collections.emptyList();

            List<TrendItem> result;
            // AI Personalization
            if (aiFilter) {
                result = rankByAi(allItems);
            } else {
                // Shuffle mixed results if no AI filter
                Collections.shuffle(allItems);
                result = allItems.stream().limit(20).collect(Collectors.toList());
            }

            // Cache result (30 minutes)
            redisService.set(cacheKey, mapper.writeValueAsString(result), 30, java.util.concurrent.TimeUnit.MINUTES);

            return result;

        } catch (Exception e) {
            log.error("Failed to fetch trends", e);
            return Collections.emptyList();
        }
    }

    private List<TrendItem> rankByAi(List<TrendItem> items) {
        // 1. Get relevant scores
        List<String> titles = items.stream().map(TrendItem::title).limit(30).collect(Collectors.toList());
        Map<String, Integer> scores = aiService.scoreRelevance(titles);

        // 2. Sort by score
        List<TrendItem> scoredItems = items.stream()
                .filter(i -> scores.containsKey(i.title()))
                .sorted((a, b) -> scores.getOrDefault(b.title(), 0) - scores.getOrDefault(a.title(), 0))
                .collect(Collectors.toList());

        // 3. Mix: Top 70% personalized + 30% random (serendipity)
        List<TrendItem> result = new ArrayList<>();
        int topCount = Math.min(scoredItems.size(), 7);
        result.addAll(scoredItems.subList(0, topCount));

        // Add remaining as serendipity
        List<TrendItem> remaining = new ArrayList<>(items);
        remaining.removeAll(result);
        Collections.shuffle(remaining);
        result.addAll(remaining.stream().limit(3).collect(Collectors.toList()));

        return result;
    }

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
            // Using GH search API for trending repos is tricky without auth limits,
            // but we can search for repos created recently with high stars
            String date = java.time.LocalDate.now().minusDays(3).toString();
            Request request = new Request.Builder()
                    .url("https://api.github.com/search/repositories?q=created:>" + date
                            + "&sort=stars&order=desc&per_page=5")
                    .header("User-Agent", "MindMe-Backend")
                    .build();
            try (Response response = client.newCall(request).execute()) {
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
            }
        } catch (Exception e) {
            log.error("GitHub Fetch Error", e);
            return Collections.emptyList();
        }
    }

    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag).item(0).getChildNodes();
        if (nodeList.getLength() > 0)
            return nodeList.item(0).getNodeValue();
        return "";
    }

    public record TrendItem(String source, String title, String url, int score) {
    }
}
