package com.touchkiss.mindme.service;

import com.touchkiss.mindme.controller.CognitionController.HorizonCard;
import com.touchkiss.mindme.domain.UserInterest;
import com.touchkiss.mindme.repository.UserInterestRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CognitiveHorizonService {

    private final UserInterestRepository interestRepository;
    private final ChatLanguageModel chatModel;

    // Cache the result for 4 hours to avoid spamming the AI and keeps the "Daily
    // Spark" feel
    @Cacheable(value = "cognitiveHorizon", key = "'daily'")
    public List<HorizonCard> generateHorizon() {
        try {
            List<UserInterest> interests = interestRepository.findTop10ByOrderByWeightDesc();

            String interestStr = (interests == null || interests.isEmpty())
                    ? "通用知识, 个人成长"
                    : interests.stream().map(UserInterest::getCategory).collect(Collectors.joining(", "));

            log.info("Generating cognitive horizon for interests: {}", interestStr);

            String template = """
                    你是一个“认知扩张”助手。你的目标是打破用户的信息茧房。

                    用户当前深度的兴趣领域包括：[{{interests}}]。

                    你的任务：
                    建议 3 个来自这些领域**之外**的多元化概念、思维模型或理论。
                    它们应该来自生物学、历史、哲学、物理学、艺术或心理学等领域，但能提供一个令人惊讶的类比或经验，适用于用户的核心兴趣。

                    输出格式要求（严格遵守，不要包含 Markdown 格式，不要包含代码块）：
                    ---CARD---
                    Title: [概念名称]
                    Type: [领域/类别，例如：进化生物学]
                    Description: [一句话定义]
                    Relevance: [为什么这对对 {{interests}} 感兴趣的人有意义]
                    ---END---

                    重要事项：
                    1. 确保严格遵守上述纯文本格式。
                    2. 使用简体中文回答。
                    3. 确保内容具有高质量、学术性和启发性。
                    """;

            PromptTemplate promptTemplate = PromptTemplate.from(template);
            Map<String, Object> variables = new HashMap<>();
            variables.put("interests", interestStr);
            Prompt prompt = promptTemplate.apply(variables);

            int maxRetries = 2;
            int retryCount = 0;

            while (retryCount <= maxRetries) {
                try {
                    String response = chatModel.generate(prompt.text());
                    if (response != null && !response.isBlank()) {
                        List<HorizonCard> cards = parseCards(response);
                        if (!cards.isEmpty()) {
                            return cards;
                        }
                    }
                    log.warn("AI returned no valid cards or empty response, retrying... (attempt {}/{})", retryCount + 1, maxRetries + 1);
                } catch (Exception e) {
                    log.warn("AI call failed in attempt {}: {}", retryCount + 1, e.getMessage());
                }
                retryCount++;
            }
        } catch (Exception e) {
            log.error("Fatal error in generateHorizon: {}", e.getMessage(), e);
        }

        log.error("Failed to generate cognitive horizon, returning fallback cards.");
        return getFallbackCards();
    }

    private List<HorizonCard> parseCards(String response) {
        List<HorizonCard> cards = new ArrayList<>();
        // 增强正则，支持可能的 Markdown 加粗符号或多余空格
        Pattern pattern = Pattern.compile(
                "---CARD---[\\s\\n]*" +
                        "(?:Title|标题)[:：]\\s*(.+?)[\\s\\n]*" +
                        "(?:Type|类型)[:：]\\s*(.+?)[\\s\\n]*" +
                        "(?:Description|描述)[:：]\\s*(.+?)[\\s\\n]*" +
                        "(?:Relevance|相关性)[:：]\\s*(.+?)[\\s\\n]*" +
                        "---END---",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        Matcher matcher = pattern.matcher(response);
        while (matcher.find()) {
            String title = matcher.group(1).replaceAll("[\\*#\\[\\]]", "").trim();
            String type = matcher.group(2).replaceAll("[\\*#\\[\\]]", "").trim();
            String desc = matcher.group(3).replaceAll("[\\*#\\[\\]]", "").trim();
            String rel = matcher.group(4).replaceAll("[\\*#\\[\\]]", "").trim();

            if (!title.isEmpty() && !desc.isEmpty()) {
                cards.add(new HorizonCard(title, desc, type, rel));
            }
        }
        return cards;
    }

    private List<HorizonCard> getFallbackCards() {
        // Hardcoded fallback mental models when AI is unavailable
        List<HorizonCard> fallback = new ArrayList<>();
        fallback.add(new HorizonCard(
                "第一性原理",
                "将复杂问题分解为最基本的元素，然后从头开始重建。",
                "思维模型",
                "帮助你跳出现有框架，找到问题的本质解决方案。"));
        fallback.add(new HorizonCard(
                "帕累托法则",
                "80%的结果来自20%的原因。专注于真正重要的少数事物。",
                "经济学",
                "优化你的时间和精力分配，事半功倍。"));
        fallback.add(new HorizonCard(
                "逆向思维",
                "与其追求成功，不如先避免愚蠢。将问题倒过来思考。",
                "心理学",
                "通过排除法找到更可靠的决策路径。"));
        Collections.shuffle(fallback);
        return fallback;
    }
}
