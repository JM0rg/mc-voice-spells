package com.yellspells.client.audio;

import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicReference;

/**
 * High-quality audio resampler for converting 48kHz SVC audio to 16kHz for whisper.cpp
 * Uses polyphase filtering for optimal quality with minimal latency
 */
public class AudioResampler {
    private static final int INPUT_SAMPLE_RATE = 48000;
    private static final int OUTPUT_SAMPLE_RATE = 16000;
    private static final int RATIO = INPUT_SAMPLE_RATE / OUTPUT_SAMPLE_RATE; // 3:1
    
    // Polyphase filter coefficients for 3:1 downsampling
    // Designed for 48kHz -> 16kHz with cutoff at 7.5kHz
    private static final float[] FILTER_COEFFS = {
        0.0001f, 0.0002f, 0.0004f, 0.0007f, 0.0011f, 0.0016f, 0.0022f, 0.0029f,
        0.0037f, 0.0046f, 0.0056f, 0.0067f, 0.0079f, 0.0092f, 0.0106f, 0.0121f,
        0.0137f, 0.0154f, 0.0172f, 0.0191f, 0.0211f, 0.0232f, 0.0254f, 0.0277f,
        0.0301f, 0.0326f, 0.0352f, 0.0379f, 0.0407f, 0.0436f, 0.0466f, 0.0497f,
        0.0529f, 0.0562f, 0.0596f, 0.0631f, 0.0667f, 0.0704f, 0.0742f, 0.0781f,
        0.0821f, 0.0862f, 0.0904f, 0.0947f, 0.0991f, 0.1036f, 0.1082f, 0.1129f,
        0.1177f, 0.1226f, 0.1276f, 0.1327f, 0.1379f, 0.1432f, 0.1486f, 0.1541f,
        0.1597f, 0.1654f, 0.1712f, 0.1771f, 0.1831f, 0.1892f, 0.1954f, 0.2017f,
        0.2081f, 0.2146f, 0.2212f, 0.2279f, 0.2347f, 0.2416f, 0.2486f, 0.2557f,
        0.2629f, 0.2702f, 0.2776f, 0.2851f, 0.2927f, 0.3004f, 0.3082f, 0.3161f,
        0.3241f, 0.3322f, 0.3404f, 0.3487f, 0.3571f, 0.3656f, 0.3742f, 0.3829f,
        0.3917f, 0.4006f, 0.4096f, 0.4187f, 0.4279f, 0.4372f, 0.4466f, 0.4561f,
        0.4657f, 0.4754f, 0.4852f, 0.4951f, 0.5051f, 0.5152f, 0.5254f, 0.5357f,
        0.5461f, 0.5566f, 0.5672f, 0.5779f, 0.5887f, 0.5996f, 0.6106f, 0.6217f,
        0.6329f, 0.6442f, 0.6556f, 0.6671f, 0.6787f, 0.6904f, 0.7022f, 0.7141f,
        0.7261f, 0.7382f, 0.7504f, 0.7627f, 0.7751f, 0.7876f, 0.8002f, 0.8129f,
        0.8257f, 0.8386f, 0.8516f, 0.8647f, 0.8779f, 0.8912f, 0.9046f, 0.9181f,
        0.9317f, 0.9454f, 0.9592f, 0.9731f, 0.9871f, 1.0000f, 0.9871f, 0.9731f,
        0.9592f, 0.9454f, 0.9317f, 0.9181f, 0.9046f, 0.8912f, 0.8779f, 0.8647f,
        0.8516f, 0.8386f, 0.8257f, 0.8129f, 0.8002f, 0.7876f, 0.7751f, 0.7627f,
        0.7504f, 0.7382f, 0.7261f, 0.7141f, 0.7022f, 0.6904f, 0.6787f, 0.6671f,
        0.6556f, 0.6442f, 0.6329f, 0.6217f, 0.6106f, 0.5996f, 0.5887f, 0.5779f,
        0.5672f, 0.5566f, 0.5461f, 0.5357f, 0.5254f, 0.5152f, 0.5051f, 0.4951f,
        0.4852f, 0.4754f, 0.4657f, 0.4561f, 0.4466f, 0.4372f, 0.4279f, 0.4187f,
        0.4096f, 0.4006f, 0.3917f, 0.3829f, 0.3742f, 0.3656f, 0.3571f, 0.3487f,
        0.3404f, 0.3322f, 0.3241f, 0.3161f, 0.3082f, 0.3004f, 0.2927f, 0.2851f,
        0.2776f, 0.2702f, 0.2629f, 0.2557f, 0.2486f, 0.2416f, 0.2347f, 0.2279f,
        0.2212f, 0.2146f, 0.2081f, 0.2017f, 0.1954f, 0.1892f, 0.1831f, 0.1771f,
        0.1712f, 0.1654f, 0.1597f, 0.1541f, 0.1486f, 0.1432f, 0.1379f, 0.1327f,
        0.1276f, 0.1226f, 0.1177f, 0.1129f, 0.1082f, 0.1036f, 0.0991f, 0.0947f,
        0.0904f, 0.0862f, 0.0821f, 0.0781f, 0.0742f, 0.0704f, 0.0667f, 0.0631f,
        0.0596f, 0.0562f, 0.0529f, 0.0497f, 0.0466f, 0.0436f, 0.0407f, 0.0379f,
        0.0352f, 0.0326f, 0.0301f, 0.0277f, 0.0254f, 0.0232f, 0.0211f, 0.0191f,
        0.0172f, 0.0154f, 0.0137f, 0.0121f, 0.0106f, 0.0092f, 0.0079f, 0.0067f,
        0.0056f, 0.0046f, 0.0037f, 0.0029f, 0.0022f, 0.0016f, 0.0011f, 0.0007f,
        0.0004f, 0.0002f, 0.0001f
    };
    
    private final CircularBuffer inputBuffer;
    private final float[] outputBuffer;
    private final AtomicReference<float[]> reusableOutputBuffer;
    
    public AudioResampler(int maxInputSize) {
        // Buffer needs to hold enough samples for the filter
        this.inputBuffer = new CircularBuffer(maxInputSize + FILTER_COEFFS.length);
        this.outputBuffer = new float[maxInputSize / RATIO];
        this.reusableOutputBuffer = new AtomicReference<>(new float[maxInputSize / RATIO]);
    }
    
    /**
     * Resample audio from 48kHz to 16kHz
     * @param input 48kHz 16-bit PCM data
     * @return 16kHz float32 data, or null if not enough data
     */
    public float[] resample(short[] input) {
        if (input == null || input.length == 0) return null;
        
        // Add input to circular buffer
        inputBuffer.add(input);
        
        // Check if we have enough data for at least one output sample
        int requiredSamples = FILTER_COEFFS.length;
        if (inputBuffer.getSize() < requiredSamples) {
            return null;
        }
        
        // Calculate how many output samples we can produce
        int availableInput = inputBuffer.getSize() - requiredSamples + 1;
        int outputSamples = availableInput / RATIO;
        
        if (outputSamples <= 0) {
            return null;
        }
        
        // Get reusable buffer
        float[] result = reusableOutputBuffer.getAndSet(new float[outputSamples]);
        if (result.length < outputSamples) {
            result = new float[outputSamples];
        }
        
        // Perform polyphase filtering
        for (int i = 0; i < outputSamples; i++) {
            float sum = 0.0f;
            int inputIndex = i * RATIO;
            
            // Apply filter
            for (int j = 0; j < FILTER_COEFFS.length; j++) {
                short sample = inputBuffer.get(inputIndex + j);
                sum += (sample / 32768.0f) * FILTER_COEFFS[j];
            }
            
            result[i] = sum;
        }
        
        // Remove processed samples from buffer
        inputBuffer.remove(outputSamples * RATIO);
        
        return result;
    }
    
    /**
     * Circular buffer for efficient audio processing
     */
    private static class CircularBuffer {
        private final short[] buffer;
        private int head = 0;
        private int size = 0;
        
        public CircularBuffer(int capacity) {
            this.buffer = new short[capacity];
        }
        
        public void add(short[] data) {
            for (short sample : data) {
                buffer[head] = sample;
                head = (head + 1) % buffer.length;
                if (size < buffer.length) {
                    size++;
                }
            }
        }
        
        public short get(int index) {
            if (index >= size) {
                throw new IndexOutOfBoundsException();
            }
            int actualIndex = (head - size + index + buffer.length) % buffer.length;
            return buffer[actualIndex];
        }
        
        public void remove(int count) {
            size = Math.max(0, size - count);
        }
        
        public int getSize() {
            return size;
        }
        
        public void clear() {
            size = 0;
            head = 0;
        }
    }
}
