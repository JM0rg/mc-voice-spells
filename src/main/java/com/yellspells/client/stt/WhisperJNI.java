package com.yellspells.client.stt;

import com.yellspells.YellSpellsMod;
import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * JNI interface to whisper.cpp for low-latency speech recognition
 * Uses direct buffers to avoid GC overhead and enable efficient native calls
 */
public class WhisperJNI {
    private long contextPtr;
    private float lastConfidence;
    private final FloatBuffer audioBuffer;
    private final ByteBuffer whisperBuffer;
    
    static {
        try {
            System.loadLibrary("whisper_jni");
            YellSpellsMod.LOGGER.info("Loaded whisper JNI library");
        } catch (UnsatisfiedLinkError e) {
            YellSpellsMod.LOGGER.error("Failed to load whisper JNI library", e);
        }
    }
    
    public WhisperJNI() {
        // Pre-allocate buffers to avoid GC churn
        this.audioBuffer = ByteBuffer.allocateDirect(8192 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.whisperBuffer = ByteBuffer.allocateDirect(4096);
    }
    
    public void initialize(String modelName) throws Exception {
        String modelPath = getModelPath(modelName);
        contextPtr = nativeInit(modelPath);
        
        if (contextPtr == 0) {
            throw new Exception("Failed to initialize whisper with model: " + modelPath);
        }
        
        YellSpellsMod.LOGGER.info("Whisper initialized with model: {}", modelPath);
    }
    
    public String transcribe(float[] audioData) {
        if (contextPtr == 0) return null;
        
        try {
            // Copy audio data to direct buffer
            audioBuffer.clear();
            audioBuffer.put(audioData);
            audioBuffer.flip();
            
            // Call native whisper processing
            int result = nativeTranscribe(contextPtr, audioBuffer, audioData.length, whisperBuffer);
            
            if (result == 0) {
                // Extract text and confidence from buffer
                whisperBuffer.flip();
                String text = extractText(whisperBuffer);
                lastConfidence = extractConfidence(whisperBuffer);
                return text;
            }
            
        } catch (Exception e) {
            YellSpellsMod.LOGGER.error("Failed to transcribe audio", e);
        }
        
        return null;
    }
    
    public float getConfidence() {
        return lastConfidence;
    }
    
    public void cleanup() {
        if (contextPtr != 0) {
            nativeCleanup(contextPtr);
            contextPtr = 0;
        }
    }
    
    private String getModelPath(String modelName) {
        // This would implement model downloading and caching
        // For now, return a placeholder path
        return "/path/to/models/" + modelName + ".bin";
    }
    
    private String extractText(ByteBuffer buffer) {
        // Extract text from the buffer returned by native code
        // This is a simplified version - actual implementation would depend on native format
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return new String(bytes).trim();
    }
    
    private float extractConfidence(ByteBuffer buffer) {
        // Extract confidence from the buffer
        // This is a simplified version
        return 0.8f; // Placeholder
    }
    
    // Native method declarations
    private native long nativeInit(String modelPath);
    private native int nativeTranscribe(long context, FloatBuffer audioBuffer, int sampleCount, ByteBuffer resultBuffer);
    private native void nativeCleanup(long context);
}
