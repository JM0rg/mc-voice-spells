package com.yellspells.client.audio;

import com.yellspells.YellSpellsMod;
import com.yellspells.client.stt.SpeechToTextManager;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioProcessor {
    private final BlockingQueue<AudioFrame> audioQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AudioBuffer buffer;
    private final VoiceActivityDetector vad;
    private final AudioResampler resampler;
    private Thread processingThread;
    
    public AudioProcessor() {
        this.buffer = new AudioBuffer(YellSpellsMod.getConfig().audioBufferSize);
        this.vad = new VoiceActivityDetector(YellSpellsMod.getConfig().vadThreshold);
        this.resampler = new AudioResampler(8192); // Handle up to 8K samples at once
    }
    
    public void start() {
        if (running.compareAndSet(false, true)) {
            processingThread = new Thread(this::processAudio, "YellSpells-AudioProcessor");
            processingThread.setDaemon(true);
            processingThread.start();
            YellSpellsMod.LOGGER.info("Audio processor started");
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
            audioQueue.clear();
            YellSpellsMod.LOGGER.info("Audio processor stopped");
        }
    }
    
    public void processAudioFrame(short[] pcmData, int sampleRate) {
        if (!running.get()) return;
        
        try {
            // Resample 48kHz to 16kHz for whisper.cpp
            float[] resampledData = resampler.resample(pcmData);
            if (resampledData != null) {
                AudioFrame frame = new AudioFrame(resampledData, 16000);
                audioQueue.offer(frame);
            }
        } catch (Exception e) {
            YellSpellsMod.LOGGER.error("Failed to queue audio frame", e);
        }
    }
    
    private void processAudio() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                AudioFrame frame = audioQueue.take();
                
                // Add frame to buffer
                buffer.addFrame(frame);
                
                // Check if buffer is full
                if (buffer.isFull()) {
                    // Apply VAD
                    if (vad.hasVoiceActivity(buffer.getAudioData())) {
                        // Send to STT manager
                        SpeechToTextManager sttManager = com.yellspells.client.YellSpellsClientMod.getSttManager();
                        if (sttManager != null) {
                            sttManager.processAudioChunk(buffer.getAudioData());
                        }
                    }
                    
                    // Clear buffer for next chunk
                    buffer.clear();
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                YellSpellsMod.LOGGER.error("Error processing audio", e);
            }
        }
    }
    
    public boolean isRunning() {
        return running.get();
    }
    
    public static class AudioFrame {
        public final float[] audioData;
        public final int sampleRate;
        public final long timestamp;
        
        public AudioFrame(float[] audioData, int sampleRate) {
            this.audioData = audioData.clone();
            this.sampleRate = sampleRate;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
