package com.touchkiss.mindme.service;

import com.touchkiss.mindme.domain.ActivityRecord;
import com.touchkiss.mindme.domain.KnowledgeEdge;
import com.touchkiss.mindme.domain.KnowledgeNode;
import com.touchkiss.mindme.repository.ActivityRecordRepository;
import com.touchkiss.mindme.repository.KnowledgeEdgeRepository;
import com.touchkiss.mindme.repository.KnowledgeNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeGraphService {

    private final KnowledgeNodeRepository nodeRepository;
    private final KnowledgeEdgeRepository edgeRepository;
    private final ActivityRecordRepository activityRepository;

    @Transactional
    public void rebuildGraph() {
        log.info("Rebuilding knowledge graph...");
        // Clear existing graph
        edgeRepository.deleteAll();
        nodeRepository.deleteAll();

        List<ActivityRecord> records = activityRepository.findAll();
        Map<String, KnowledgeNode> urlToNode = new HashMap<>();

        // 1. Create Nodes (Pages & Searches)
        for (ActivityRecord record : records) {
            // Only include meaningful records
            if (record.getInterestScore() < 20 && !"generated".equals(record.getTransitionType())) {
                continue;
            }

            // Deduplicate by URL
            KnowledgeNode node = urlToNode.computeIfAbsent(record.getUrl(), k -> {
                KnowledgeNode n = new KnowledgeNode();
                n.setName(record.getTitle().length() > 50 ? record.getTitle().substring(0, 47) + "..."
                        : record.getTitle());
                n.setDescription(record.getUrl());
                n.setType(isSearch(record) ? KnowledgeNode.NodeType.TOPIC : KnowledgeNode.NodeType.PAGE);
                n.setWeight(record.getInterestScore());
                return nodeRepository.save(n);
            });

            // Accumulate weight for multiple visits
            if (node.getId() != null) { // if already existed
                // simple increment, but could be more complex
                node.setWeight(Math.max(node.getWeight(), record.getInterestScore()));
                nodeRepository.save(node);
            }
        }

        // 2. Create Edges
        for (ActivityRecord record : records) {
            KnowledgeNode source = urlToNode.get(record.getUrl());
            if (source == null)
                continue;

            // Link by Referrer
            if (record.getReferrer() != null) {
                KnowledgeNode target = urlToNode.get(record.getReferrer());
                if (target != null && !target.equals(source)) {
                    createEdge(target, source, KnowledgeEdge.EdgeType.RELATED);
                }
            }

            // Link by Context (Search -> Page or Page -> Search)
            if (record.getRelatedRecordUrl() != null) {
                KnowledgeNode target = urlToNode.get(record.getRelatedRecordUrl());
                if (target != null && !target.equals(source)) {
                    createEdge(target, source, KnowledgeEdge.EdgeType.RELATED);
                    log.info("Created edge: {} -> {}", target.getName(), source.getName());
                }
            }
        }

        log.info("Graph rebuild complete. Nodes: {}", urlToNode.size());
    }

    private void createEdge(KnowledgeNode source, KnowledgeNode target, KnowledgeEdge.EdgeType type) {
        // Avoid duplicate edges
        if (edgeRepository.findBySourceAndTarget(source, target).isPresent())
            return;

        KnowledgeEdge edge = new KnowledgeEdge();
        edge.setSource(source);
        edge.setTarget(target);
        edge.setType(type);
        edge.setWeight(1.0);
        edgeRepository.save(edge);
    }

    private boolean isSearch(ActivityRecord record) {
        return record.getSearchQuery() != null || "generated".equals(record.getTransitionType());
    }

    public Map<String, Object> getGraphData() {
        List<KnowledgeNode> nodes = nodeRepository.findAll();
        List<KnowledgeEdge> edges = edgeRepository.findAll();

        List<Map<String, Object>> nodesData = nodes.stream().map(n -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", n.getId());
            map.put("name", n.getName());
            map.put("val", n.getWeight()); // visualization logic often uses 'val'
            map.put("type", n.getType().toString());
            return map;
        }).collect(Collectors.toList());

        List<Map<String, Object>> edgesData = edges.stream().map(e -> {
            Map<String, Object> map = new HashMap<>();
            map.put("source", e.getSource().getId());
            map.put("target", e.getTarget().getId());
            map.put("type", e.getType().toString());
            return map;
        }).collect(Collectors.toList());

        return Map.of("nodes", nodesData, "links", edgesData);
    }

    private String getDomain(String url) {
        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            return domain != null ? domain.startsWith("www.") ? domain.substring(4) : domain : null;
        } catch (Exception e) {
            return null;
        }
    }
}
