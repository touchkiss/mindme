package com.touchkiss.mindme.controller;

import com.touchkiss.mindme.domain.UserConfig;
import com.touchkiss.mindme.repository.UserConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ConfigController {

    private final UserConfigRepository repository;

    @GetMapping("/{key}")
    public ResponseEntity<Map<String, String>> getConfig(@PathVariable String key) {
        UserConfig config = repository.findByConfigKey(key).orElse(null);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, String> response = new HashMap<>();
        response.put("key", config.getConfigKey());
        response.put("value", config.getConfigValue());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{key}")
    public ResponseEntity<UserConfig> saveConfig(@PathVariable String key, @RequestBody Map<String, String> body) {
        String value = body.get("value");
        if (value == null) {
            return ResponseEntity.badRequest().build();
        }

        UserConfig config = repository.findByConfigKey(key).orElse(new UserConfig());
        config.setConfigKey(key);
        // Clean JSON formatting if double encoded
        config.setConfigValue(value);

        return ResponseEntity.ok(repository.save(config));
    }
}
