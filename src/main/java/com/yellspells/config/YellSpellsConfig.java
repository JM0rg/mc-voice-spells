package com.yellspells.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yellspells.YellSpellsMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class YellSpellsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("yellspells.json");
    
    // Audio processing settings
    public int audioBufferSize = 400; // ms
    public float vadThreshold = 0.3f;
    public int sampleRate = 16000;
    
    // STT settings
    public String modelName = "tiny.en";
    public float confidenceThreshold = 0.7f;
    public int stabilityThreshold = 2; // consecutive partials
    public int maxPartialLength = 50; // characters
    
    // Spell settings
    public Map<String, SpellConfig> spells = new HashMap<>();
    public int globalCooldown = 1000; // ms
    public boolean requirePermission = true;
    
    // Anti-cheat settings
    public int maxTimeSkew = 5000; // ms
    public int nonceWindow = 1000; // ms
    public boolean enableRaycastValidation = true;
    
    public YellSpellsConfig() {
        // Initialize default spells - spells are now handled by Magic System mod via /cast commands
        spells.put("fireball", SpellConfig.ofSingle("fireball", 2000, 0.6f, true));
        spells.put("safedescent", SpellConfig.ofSingle("safe descent", 40000, 0.6f, false));
        spells.put("greatwall", SpellConfig.ofSingle("great wall", 10000, 0.6f, true));
    }
    
    public void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String content = Files.readString(CONFIG_PATH);
                JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                
                // Load basic settings
                if (json.has("audioBufferSize")) audioBufferSize = json.get("audioBufferSize").getAsInt();
                if (json.has("vadThreshold")) vadThreshold = json.get("vadThreshold").getAsFloat();
                if (json.has("sampleRate")) sampleRate = json.get("sampleRate").getAsInt();
                if (json.has("modelName")) modelName = json.get("modelName").getAsString();
                if (json.has("confidenceThreshold")) confidenceThreshold = json.get("confidenceThreshold").getAsFloat();
                if (json.has("stabilityThreshold")) stabilityThreshold = json.get("stabilityThreshold").getAsInt();
                if (json.has("maxPartialLength")) maxPartialLength = json.get("maxPartialLength").getAsInt();
                if (json.has("globalCooldown")) globalCooldown = json.get("globalCooldown").getAsInt();
                if (json.has("requirePermission")) requirePermission = json.get("requirePermission").getAsBoolean();
                if (json.has("maxTimeSkew")) maxTimeSkew = json.get("maxTimeSkew").getAsInt();
                if (json.has("nonceWindow")) nonceWindow = json.get("nonceWindow").getAsInt();
                if (json.has("enableRaycastValidation")) enableRaycastValidation = json.get("enableRaycastValidation").getAsBoolean();
                
                // Load spells (robust against missing fields)
                if (json.has("spells")) {
                    JsonObject spellsJson = json.getAsJsonObject("spells");
                    spells.clear();
                    for (String key : spellsJson.keySet()) {
                        try {
                            JsonObject spellJson = spellsJson.getAsJsonObject(key);
                            // Back-compat: accept single keyword or keywords array
                            java.util.List<String> keywords;
                            if (spellJson.has("keywords") && spellJson.get("keywords").isJsonArray()) {
                                keywords = new java.util.ArrayList<>();
                                for (var el : spellJson.getAsJsonArray("keywords")) {
                                    keywords.add(el.getAsString());
                                }
                            } else {
                                String keywordVal = spellJson.has("keyword") ? spellJson.get("keyword").getAsString() : key;
                                keywords = java.util.List.of(keywordVal);
                            }
                            int cooldownVal = spellJson.has("cooldown") ? spellJson.get("cooldown").getAsInt() : 2000;
                            float confidenceVal = spellJson.has("confidenceThreshold") ? spellJson.get("confidenceThreshold").getAsFloat() : this.confidenceThreshold;
                            boolean requiresTargetVal = spellJson.has("requiresTarget") && spellJson.get("requiresTarget").getAsBoolean();
                            boolean enabled = !spellJson.has("enabled") || spellJson.get("enabled").getAsBoolean();
                            String command = spellJson.has("command") ? spellJson.get("command").getAsString() : null;
                            SpellConfig spell = new SpellConfig(keywords, cooldownVal, confidenceVal, requiresTargetVal, enabled, command);
                            spells.put(key, spell);
                        } catch (Exception e) {
                            YellSpellsMod.LOGGER.warn("Failed to parse spell config for key {}. Using defaults.", key, e);
                            spells.put(key, SpellConfig.ofSingle(key, 2000, this.confidenceThreshold, false));
                        }
                    }
                    // Ensure defaults exist if not present
                    spells.putIfAbsent("fireball", SpellConfig.ofSingle("fireball", 2000, 0.6f, true));
                    spells.putIfAbsent("safedescent", SpellConfig.ofSingle("safe descent", 40000, 0.6f, false));
                    spells.putIfAbsent("greatwall", SpellConfig.ofSingle("great wall", 10000, 0.6f, true));
                }
                
                YellSpellsMod.LOGGER.info("Configuration loaded from {}", CONFIG_PATH);
            } else {
                save();
                YellSpellsMod.LOGGER.info("Created default configuration at {}", CONFIG_PATH);
            }
        } catch (IOException e) {
            YellSpellsMod.LOGGER.error("Failed to load configuration", e);
        }
    }
    
    public void save() {
        try {
            JsonObject json = new JsonObject();
            
            // Save basic settings
            json.addProperty("audioBufferSize", audioBufferSize);
            json.addProperty("vadThreshold", vadThreshold);
            json.addProperty("sampleRate", sampleRate);
            json.addProperty("modelName", modelName);
            json.addProperty("confidenceThreshold", confidenceThreshold);
            json.addProperty("stabilityThreshold", stabilityThreshold);
            json.addProperty("maxPartialLength", maxPartialLength);
            json.addProperty("globalCooldown", globalCooldown);
            json.addProperty("requirePermission", requirePermission);
            json.addProperty("maxTimeSkew", maxTimeSkew);
            json.addProperty("nonceWindow", nonceWindow);
            json.addProperty("enableRaycastValidation", enableRaycastValidation);
            
            // Save spells
            JsonObject spellsJson = new JsonObject();
            for (Map.Entry<String, SpellConfig> entry : spells.entrySet()) {
                JsonObject spellJson = new JsonObject();
                SpellConfig spell = entry.getValue();
                // prefer array form
                var kwArr = new com.google.gson.JsonArray();
                for (String k : spell.keywords) kwArr.add(k);
                spellJson.add("keywords", kwArr);
                spellJson.addProperty("cooldown", spell.cooldown);
                spellJson.addProperty("confidenceThreshold", spell.confidenceThreshold);
                spellJson.addProperty("requiresTarget", spell.requiresTarget);
                spellJson.addProperty("enabled", spell.enabled);
                if (spell.command != null) spellJson.addProperty("command", spell.command);
                spellsJson.add(entry.getKey(), spellJson);
            }
            json.add("spells", spellsJson);
            
            Files.writeString(CONFIG_PATH, GSON.toJson(json));
            YellSpellsMod.LOGGER.info("Configuration saved to {}", CONFIG_PATH);
        } catch (IOException e) {
            YellSpellsMod.LOGGER.error("Failed to save configuration", e);
        }
    }
    
    public SpellConfig getSpell(String spellId) {
        return spells.get(spellId);
    }
    
    public static class SpellConfig {
        public java.util.List<String> keywords;
        public int cooldown; // ms
        public float confidenceThreshold;
        public boolean requiresTarget;
        public boolean enabled;
        public String command; // optional override (e.g., "cast fireball power=2")
        
        public SpellConfig(java.util.List<String> keywords, int cooldown, float confidenceThreshold, boolean requiresTarget, boolean enabled, String command) {
            this.keywords = keywords;
            this.cooldown = cooldown;
            this.confidenceThreshold = confidenceThreshold;
            this.requiresTarget = requiresTarget;
            this.enabled = enabled;
            this.command = command;
        }
        public static SpellConfig ofSingle(String keyword, int cooldown, float confidenceThreshold, boolean requiresTarget) {
            return new SpellConfig(java.util.List.of(keyword), cooldown, confidenceThreshold, requiresTarget, true, null);
        }
    }
}
