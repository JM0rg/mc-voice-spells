package com.yellspells.client.stt;

import com.yellspells.YellSpellsMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
public class ModelManager {
    
    // Whisper tiny.en model from Hugging Face
    private static final String MODEL_URL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin";
    private static final String MODEL_SHA256 = "921e4cf2e068bcb2fb4ee2f85d8c5d1e33a0995b0c8f28a6a1e8e2c5c0f2e6a8"; // Example hash
    private static final String MODEL_FILENAME = "ggml-tiny.en.bin";
    
    private final Path modelDir;
    private final Path modelPath;
    
    public ModelManager() {
        // Store models in .minecraft/yellspells/models/
        this.modelDir = Paths.get(MinecraftClient.getInstance().runDirectory.getPath(), "yellspells", "models");
        this.modelPath = modelDir.resolve(MODEL_FILENAME);
        
        try {
            Files.createDirectories(modelDir);
        } catch (IOException e) {
            YellSpellsMod.LOGGER.error("Failed to create model directory", e);
        }
    }
    
    /**
     * Gets the path to the Whisper model, downloading it if necessary
     */
    public CompletableFuture<String> getModelPath() {
        if (Files.exists(modelPath)) {
            YellSpellsMod.LOGGER.info("Whisper model found at: {}", modelPath);
            return CompletableFuture.completedFuture(modelPath.toString());
        }
        
        return promptAndDownloadModel();
    }
    
    private CompletableFuture<String> promptAndDownloadModel() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Show chat message to user
                MinecraftClient.getInstance().execute(() -> {
                    if (MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.sendMessage(
                            Text.literal("[YellSpells] ").formatted(Formatting.GREEN)
                                .append(Text.literal("Downloading Whisper model for voice spells...").formatted(Formatting.YELLOW)),
                            false
                        );
                    }
                });
                
                YellSpellsMod.LOGGER.info("Downloading Whisper model from: {}", MODEL_URL);
                
                // Download the model
                URL url = new URL(MODEL_URL);
                try (InputStream in = url.openStream()) {
                    Files.copy(in, modelPath, StandardCopyOption.REPLACE_EXISTING);
                }
                
                // Verify file exists and has reasonable size
                if (Files.exists(modelPath) && Files.size(modelPath) > 1024 * 1024) { // At least 1MB
                    YellSpellsMod.LOGGER.info("Whisper model downloaded successfully: {}", modelPath);
                    
                    // Notify user of success
                    MinecraftClient.getInstance().execute(() -> {
                        if (MinecraftClient.getInstance().player != null) {
                            MinecraftClient.getInstance().player.sendMessage(
                                Text.literal("[YellSpells] ").formatted(Formatting.GREEN)
                                    .append(Text.literal("Model downloaded! You can now use voice spells. Say 'fireball' while looking at a target!").formatted(Formatting.GREEN)),
                                false
                            );
                        }
                    });
                    
                    return modelPath.toString();
                } else {
                    throw new IOException("Downloaded model file is invalid or too small");
                }
                
            } catch (Exception e) {
                YellSpellsMod.LOGGER.error("Failed to download Whisper model", e);
                
                // Notify user of failure
                MinecraftClient.getInstance().execute(() -> {
                    if (MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.sendMessage(
                            Text.literal("[YellSpells] ").formatted(Formatting.RED)
                                .append(Text.literal("Failed to download Whisper model. Voice spells will not work.").formatted(Formatting.RED)),
                            false
                        );
                    }
                });
                
                return null;
            }
        });
    }
    
    /**
     * Check if the model is available without triggering download
     */
    public boolean isModelAvailable() {
        return Files.exists(modelPath);
    }
    
    /**
     * Get the model path if it exists, null otherwise
     */
    public String getModelPathIfExists() {
        return Files.exists(modelPath) ? modelPath.toString() : null;
    }
}
