package com.yellspells.client.gui;

import com.yellspells.YellSpellsMod;
import com.yellspells.client.YellSpellsClientMod;
import com.yellspells.client.stt.ModelManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Environment(EnvType.CLIENT)
public class ModelDownloadScreen extends Screen {
    
    private final Screen parent;
    private final ModelManager modelManager;
    private boolean downloading = false;
    
    public ModelDownloadScreen(Screen parent) {
        super(Text.literal("YellSpells Voice Model"));
        this.parent = parent;
        this.modelManager = new ModelManager();
    }
    
    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // Download button
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Download Model (Required for Voice Spells)").formatted(Formatting.GREEN),
            button -> downloadModel()
        ).dimensions(centerX - 150, centerY + 20, 300, 20).build());
        
        // Skip button
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Skip (Voice Spells Disabled)").formatted(Formatting.GRAY),
            button -> this.close()
        ).dimensions(centerX - 150, centerY + 50, 300, 20).build());
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Fill with semi-transparent dark background
        context.fill(0, 0, this.width, this.height, 0x80000000);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, 
            Text.literal("YellSpells Voice Commands").formatted(Formatting.BOLD, Formatting.YELLOW),
            centerX, centerY - 60, 0xFFFFFF);
        
        // Description
        context.drawCenteredTextWithShadow(this.textRenderer,
            Text.literal("Voice spells allow you to cast spells by speaking!"),
            centerX, centerY - 40, 0xFFFFFF);
            
        context.drawCenteredTextWithShadow(this.textRenderer,
            Text.literal("Say 'fireball' while looking at a target to cast a fireball."),
            centerX, centerY - 30, 0xFFFFFF);
        
        context.drawCenteredTextWithShadow(this.textRenderer,
            Text.literal("This requires downloading a ~40MB speech recognition model.").formatted(Formatting.AQUA),
            centerX, centerY - 10, 0xFFFFFF);
        
        if (downloading) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Downloading model... Please wait.").formatted(Formatting.YELLOW),
                centerX, centerY + 80, 0xFFFFFF);
        }
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void downloadModel() {
        if (downloading) return;
        
        downloading = true;
        YellSpellsMod.LOGGER.info("User requested Whisper model download");
        
        // Start download in background
        modelManager.getModelPath().thenAccept(modelPath -> {
            MinecraftClient.getInstance().execute(() -> {
                if (modelPath != null) {
                    // Initialize STT with downloaded model
                    YellSpellsClientMod.getAudioProcessor().initializeSTT(modelPath);
                    
                    // Show success message
                    if (client != null && client.player != null) {
                        client.player.sendMessage(
                            Text.literal("[YellSpells] ").formatted(Formatting.GREEN)
                                .append(Text.literal("Voice spells enabled! Say 'fireball' while looking at a target.").formatted(Formatting.GREEN)),
                            false
                        );
                    }
                } else {
                    // Show failure message
                    if (client != null && client.player != null) {
                        client.player.sendMessage(
                            Text.literal("[YellSpells] ").formatted(Formatting.RED)
                                .append(Text.literal("Failed to download model. Voice spells will not work.").formatted(Formatting.RED)),
                            false
                        );
                    }
                }
                this.close();
            });
        });
    }
    
    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
    
    @Override
    public boolean shouldPause() {
        return false; // Don't pause the game
    }
}
