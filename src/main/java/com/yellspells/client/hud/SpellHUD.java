package com.yellspells.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.concurrent.atomic.AtomicLong;

public class SpellHUD {
    private String currentTranscript = "";
    private float currentConfidence = 0.0f;
    private String lastSpellCast = "";
    private final AtomicLong lastSpellTime = new AtomicLong(0);
    private static final int HUD_DURATION_MS = 3000;
    
    public void updateTranscript(String transcript, float confidence) {
        this.currentTranscript = transcript;
        this.currentConfidence = confidence;
    }
    
    public void onSpellCast(String spellId, float confidence) {
        this.lastSpellCast = spellId;
        this.lastSpellTime.set(System.currentTimeMillis());
    }
    
    public void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        TextRenderer textRenderer = client.textRenderer;
        
        int y = screenHeight - 100;
        int x = 10;
        
        // Render current transcript
        if (!currentTranscript.isEmpty()) {
            String transcriptText = "Transcript: " + currentTranscript;
            context.drawText(textRenderer, Text.literal(transcriptText), x, y, 0xFFFFFF, true);
            y += 12;
            
            String confidenceText = String.format("Confidence: %.1f%%", currentConfidence * 100);
            int confidenceColor = getConfidenceColor(currentConfidence);
            context.drawText(textRenderer, Text.literal(confidenceText), x, y, confidenceColor, true);
            y += 20;
        }
        
        // Render last spell cast
        long timeSinceLastSpell = System.currentTimeMillis() - lastSpellTime.get();
        if (!lastSpellCast.isEmpty() && timeSinceLastSpell < HUD_DURATION_MS) {
            String spellText = "Last cast: " + lastSpellCast;
            context.drawText(textRenderer, Text.literal(spellText), x, y, 0x00FF00, true);
            y += 12;
            
            // Show cooldown progress
            YellSpellsConfig.SpellConfig spellConfig = YellSpellsMod.getConfig().getSpell(lastSpellCast);
            if (spellConfig != null) {
                float cooldownProgress = Math.min(1.0f, (float) timeSinceLastSpell / spellConfig.cooldown);
                String cooldownText = String.format("Cooldown: %.1fs", (spellConfig.cooldown - timeSinceLastSpell) / 1000.0f);
                int cooldownColor = cooldownProgress >= 1.0f ? 0x00FF00 : 0xFF0000;
                context.drawText(textRenderer, Text.literal(cooldownText), x, y, cooldownColor, true);
            }
        }
    }
    
    private int getConfidenceColor(float confidence) {
        if (confidence >= 0.8f) return 0x00FF00; // Green
        if (confidence >= 0.6f) return 0xFFFF00; // Yellow
        return 0xFF0000; // Red
    }
}
