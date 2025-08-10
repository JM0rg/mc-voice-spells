package com.yellspells.client.audio;

import java.util.ArrayList;
import java.util.List;

public class AudioBuffer {
    private final int targetSizeMs;
    private final List<AudioProcessor.AudioFrame> frames = new ArrayList<>();
    private int totalSamples = 0;
    
    public AudioBuffer(int targetSizeMs) {
        this.targetSizeMs = targetSizeMs;
    }
    
    public void addFrame(AudioProcessor.AudioFrame frame) {
        frames.add(frame);
        totalSamples += frame.audioData.length;
    }
    
    public boolean isFull() {
        if (frames.isEmpty()) return false;
        
        // Calculate total duration in milliseconds
        int sampleRate = frames.get(0).sampleRate;
        int totalDurationMs = (totalSamples * 1000) / sampleRate;
        
        return totalDurationMs >= targetSizeMs;
    }
    
    public float[] getAudioData() {
        if (frames.isEmpty()) return new float[0];
        
        // Concatenate all audio data
        float[] audioData = new float[totalSamples];
        int offset = 0;
        
        for (AudioProcessor.AudioFrame frame : frames) {
            System.arraycopy(frame.audioData, 0, audioData, offset, frame.audioData.length);
            offset += frame.audioData.length;
        }
        
        return audioData;
    }
    
    public void clear() {
        frames.clear();
        totalSamples = 0;
    }
    
    public int getSampleRate() {
        return frames.isEmpty() ? 16000 : frames.get(0).sampleRate;
    }
    
    public int getDurationMs() {
        if (frames.isEmpty()) return 0;
        int sampleRate = frames.get(0).sampleRate;
        return (totalSamples * 1000) / sampleRate;
    }
}
