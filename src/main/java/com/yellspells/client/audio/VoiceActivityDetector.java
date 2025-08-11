package com.yellspells.client.audio;

import com.yellspells.YellSpellsMod;

public final class VoiceActivityDetector {

  private final float attack;      // e.g., 0.005f
  private final float release;     // e.g., 0.003f
  private final int   minFrames;   // e.g., 6 frames @16k (≈120ms) to confirm speech

  private float noise = 0.02f;
  private boolean speaking = false;
  private int voicedCount = 0;
  private int frameCount = 0;

  public VoiceActivityDetector(float attack, float release, int minFrames) {
    this.attack = attack;
    this.release = release;
    this.minFrames = minFrames;
  }

  public boolean update(float[] block, int len) {
    // simple RMS
    float rms = 0f;
    for (int i = 0; i < len; i++) {
      float s = block[i];
      rms += s * s;
    }
    rms = (float)Math.sqrt(rms / Math.max(1, len));

    // adapt noise floor
    noise = (speaking ? noise * (1 - release) + rms * release
                      : noise * (1 - attack) + rms * attack);

    float hi = noise * 3.0f;
    float lo = noise * 1.5f;

    // Debug logging every 100 frames (about every 2 seconds)
    frameCount++;
    if (frameCount % 100 == 0) {
      YellSpellsMod.LOGGER.info("VAD: rms={}, noise={}, hi={}, lo={}, speaking={}, voicedCount={}", 
        String.format("%.4f", rms), String.format("%.4f", noise), String.format("%.4f", hi), String.format("%.4f", lo), speaking, voicedCount);
    }

    if (speaking) {
      if (rms < lo) {
        speaking = false;
        voicedCount = 0;
      }
    } else {
      if (rms > hi) {
        if (++voicedCount >= minFrames) {
          speaking = true;
          voicedCount = 0;
          YellSpellsMod.LOGGER.info("VAD: Voice activity detected! rms={} > hi={}", String.format("%.4f", rms), String.format("%.4f", hi));
        }
      } else {
        voicedCount = 0;
      }
    }
    return speaking;
  }
}
