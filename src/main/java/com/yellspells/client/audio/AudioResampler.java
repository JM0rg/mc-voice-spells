package com.yellspells.client.audio;

import java.util.Arrays;

/**
 * 48 kHz mono 16-bit short[] -> 16 kHz float[] (normalized -1..1)
 * 3:1 decimation with low-pass FIR (Hamming window, ~7.2 kHz cutoff).
 * Maintains state across calls; emits exactly inLen/3 samples.
 */
public final class AudioResampler {

  // Precomputed symmetric FIR taps (length is multiple of 3 preferred;  ninety-ish taps is fine)
  private static final float[] TAPS = buildLowpassTaps(81, 48000, 7200); // length, fs, fc
  private final float[] state = new float[TAPS.length - 1];

  public int outLengthFor(int inSamples48) { return inSamples48 / 3; }

  public int process(short[] inPcm48, float[] outPcm16k) {
    // Convert input to float and prepend state
    int n = inPcm48.length;
    float[] x = new float[state.length + n];
    System.arraycopy(state, 0, x, 0, state.length);
    for (int i = 0; i < n; i++) x[state.length + i] = inPcm48[i] / 32768f;

    // Convolution + decimate by 3
    int outLen = (n) / 3;
    int tapLen = TAPS.length;
    int write = 0;
    for (int i = 0; i + tapLen <= x.length; i += 3) {
      float acc = 0f;
      for (int k = 0; k < tapLen; k++) {
        acc += x[i + k] * TAPS[k];
      }
      outPcm16k[write++] = acc;
      if (write >= outLen) break;
    }

    // Save new state (last taps-1 samples of current x)
    int keep = state.length;
    System.arraycopy(x, x.length - keep, state, 0, keep);
    return write;
  }

  private static float[] buildLowpassTaps(int length, int fs, int fc) {
    // windowed-sinc (Hamming), normalized to unity gain at DC
    float[] h = new float[length];
    int M = length - 1;
    double sum = 0;
    for (int n = 0; n < length; n++) {
      double m = n - M / 2.0;
      double sinc = (m == 0) ? 2.0 * Math.PI * fc / fs
                              : Math.sin(2.0 * Math.PI * fc * m / fs) / m;
      double w = 0.54 - 0.46 * Math.cos(2.0 * Math.PI * n / M);
      h[n] = (float) (sinc * w);
      sum += h[n];
    }
    // normalize
    for (int i = 0; i < length; i++) h[i] /= (float) sum;
    return h;
  }
}
