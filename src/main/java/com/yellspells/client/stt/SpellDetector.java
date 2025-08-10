package com.yellspells.client.stt;

import com.yellspells.YellSpellsMod;
import com.yellspells.config.YellSpellsConfig;

import java.util.HashMap;
import java.util.Map;

public class SpellDetector {
    private final Map<String, SpellDetectionHistory> detectionHistory = new HashMap<>();
    private final int stabilityThreshold;
    private final float confidenceThreshold;
    
    public SpellDetector() {
        this.stabilityThreshold = YellSpellsMod.getConfig().stabilityThreshold;
        this.confidenceThreshold = YellSpellsMod.getConfig().confidenceThreshold;
    }
    
    public SpellDetectionResult detectSpell(String transcript, float confidence) {
        String lowerTranscript = transcript.toLowerCase().trim();
        
        // Check each configured spell
        for (Map.Entry<String, YellSpellsConfig.SpellConfig> entry : 
             YellSpellsMod.getConfig().spells.entrySet()) {
            
            String spellId = entry.getKey();
            YellSpellsConfig.SpellConfig spellConfig = entry.getValue();
            
            if (lowerTranscript.contains(spellConfig.keyword.toLowerCase())) {
                // Check confidence threshold
                if (confidence >= spellConfig.confidenceThreshold) {
                    // Update detection history
                    SpellDetectionHistory history = detectionHistory.computeIfAbsent(
                        spellId, k -> new SpellDetectionHistory());
                    
                    history.addDetection(confidence);
                    
                    // Check if stable
                    if (history.isStable(stabilityThreshold)) {
                        return new SpellDetectionResult(spellId, confidence, true);
                    } else {
                        return new SpellDetectionResult(spellId, confidence, false);
                    }
                }
            }
        }
        
        return null;
    }
    
    private static class SpellDetectionHistory {
        private final float[] recentConfidences = new float[10];
        private int index = 0;
        private int count = 0;
        
        public void addDetection(float confidence) {
            recentConfidences[index] = confidence;
            index = (index + 1) % recentConfidences.length;
            count = Math.min(count + 1, recentConfidences.length);
        }
        
        public boolean isStable(int threshold) {
            if (count < threshold) return false;
            
            // Check if we have enough consecutive detections
            int consecutive = 0;
            for (int i = 0; i < count; i++) {
                int idx = (index - 1 - i + recentConfidences.length) % recentConfidences.length;
                if (recentConfidences[idx] > 0) {
                    consecutive++;
                } else {
                    break;
                }
            }
            
            return consecutive >= threshold;
        }
    }
    
    public static class SpellDetectionResult {
        private final String spellId;
        private final float confidence;
        private final boolean stable;
        
        public SpellDetectionResult(String spellId, float confidence, boolean stable) {
            this.spellId = spellId;
            this.confidence = confidence;
            this.stable = stable;
        }
        
        public String getSpellId() { return spellId; }
        public float getConfidence() { return confidence; }
        public boolean isStable() { return stable; }
    }
}
