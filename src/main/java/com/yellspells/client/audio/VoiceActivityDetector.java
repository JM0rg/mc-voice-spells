package com.yellspells.client.audio;

public class VoiceActivityDetector {
    private final float threshold;
    private static final int MIN_ACTIVITY_SAMPLES = 1600; // 100ms at 16kHz
    
    public VoiceActivityDetector(float threshold) {
        this.threshold = threshold;
    }
    
    public boolean hasVoiceActivity(float[] audioData) {
        if (audioData.length < MIN_ACTIVITY_SAMPLES) {
            return false;
        }
        
        // Calculate RMS (Root Mean Square) energy
        double rms = calculateRMS(audioData);
        
        // Audio is already normalized to -1.0 to 1.0
        return rms > threshold;
    }
    
    private double calculateRMS(float[] audioData) {
        double sum = 0.0;
        
        for (float sample : audioData) {
            sum += (double) sample * sample;
        }
        
        return Math.sqrt(sum / audioData.length);
    }
    
    public float getThreshold() {
        return threshold;
    }
}
