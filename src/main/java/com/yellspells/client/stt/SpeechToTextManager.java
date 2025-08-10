package com.yellspells.client.stt;

import com.yellspells.YellSpellsMod;
import com.yellspells.client.hud.SpellHUD;
import com.yellspells.network.YellSpellsNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class SpeechToTextManager {
    private final BlockingQueue<float[]> audioQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final WhisperJNI whisper;
    private final SpellDetector spellDetector;
    private Thread processingThread;
    
    public SpeechToTextManager() {
        this.whisper = new WhisperJNI();
        this.spellDetector = new SpellDetector();
    }
    
    public void start() {
        if (running.compareAndSet(false, true)) {
            try {
                // Initialize whisper.cpp
                whisper.initialize(YellSpellsMod.getConfig().modelName);
                
                processingThread = new Thread(this::processAudio, "YellSpells-STT");
                processingThread.setDaemon(true);
                processingThread.start();
                
                YellSpellsMod.LOGGER.info("Speech-to-text manager started");
            } catch (Exception e) {
                running.set(false);
                YellSpellsMod.LOGGER.error("Failed to start STT manager", e);
            }
        }
    }
    
    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (processingThread != null) {
                processingThread.interrupt();
                try {
                    processingThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            try {
                whisper.cleanup();
            } catch (Exception e) {
                YellSpellsMod.LOGGER.error("Failed to cleanup whisper", e);
            }
            
            audioQueue.clear();
            YellSpellsMod.LOGGER.info("Speech-to-text manager stopped");
        }
    }
    
    public void processAudioChunk(float[] audioData) {
        if (!running.get()) return;
        
        try {
            audioQueue.offer(audioData);
        } catch (Exception e) {
            YellSpellsMod.LOGGER.error("Failed to queue audio chunk", e);
        }
    }
    
    private void processAudio() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                float[] audioData = audioQueue.take();
                
                // Process with whisper.cpp
                String transcript = whisper.transcribe(audioData);
                float confidence = whisper.getConfidence();
                
                if (transcript != null && !transcript.trim().isEmpty()) {
                    // Update HUD
                    SpellHUD hud = com.yellspells.client.YellSpellsClientMod.getSpellHUD();
                    if (hud != null) {
                        hud.updateTranscript(transcript, confidence);
                    }
                    
                    // Check for spell detection
                    SpellDetectionResult result = spellDetector.detectSpell(transcript, confidence);
                    if (result != null && result.isStable()) {
                        // Cast spell
                        castSpell(result);
                    }
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                YellSpellsMod.LOGGER.error("Error processing audio", e);
            }
        }
    }
    
    private void castSpell(SpellDetectionResult result) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;
            
            // Get player's look direction for ray hint
            Vec3d lookVec = client.player.getRotationVec(1.0f);
            
            // Send cast intent to server
            YellSpellsNetworking.sendCastIntent(
                result.getSpellId(),
                result.getConfidence(),
                client.player.age, // client tick
                System.currentTimeMillis(),
                lookVec.x, lookVec.y, lookVec.z
            );
            
            // Update HUD
            SpellHUD hud = com.yellspells.client.YellSpellsClientMod.getSpellHUD();
            if (hud != null) {
                hud.onSpellCast(result.getSpellId(), result.getConfidence());
            }
            
            YellSpellsMod.LOGGER.info("Cast spell: {} with confidence: {}", 
                result.getSpellId(), result.getConfidence());
                
        } catch (Exception e) {
            YellSpellsMod.LOGGER.error("Failed to cast spell", e);
        }
    }
    
    public boolean isRunning() {
        return running.get();
    }
}
