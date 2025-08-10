package com.yellspells.client.stt;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class WhisperJNI {

  static {
    try {
      System.loadLibrary("yellspells_whisper"); // your native lib name
    } catch (Throwable t) {
      // log & allow running without STT
    }
  }

  private long ctx;

  public boolean available() { return ctx != 0; }

  public void init(String modelPath, int sampleRate, int threads) {
    ctx = nativeInit(modelPath, sampleRate, threads);
  }

  // push audio (float -1..1). buf must be a direct buffer; reuse it.
  public int push(FloatBuffer buf, int samples) {
    if (ctx == 0) return -1;
    return nativePush(ctx, buf, samples);
  }

  // poll one partial; out must be a direct UTF-8 buffer you reuse; returns bytes written or 0 if none
  public int poll(ByteBuffer outUtf8, float[] confidenceStable) {
    if (ctx == 0) return 0;
    return nativePoll(ctx, outUtf8, confidenceStable);
  }

  public void close() {
    if (ctx != 0) nativeClose(ctx);
    ctx = 0;
  }

  private static native long nativeInit(String modelPath, int sr, int threads);
  private static native int  nativePush(long ctx, FloatBuffer buf, int samples);
  private static native int  nativePoll(long ctx, ByteBuffer outUtf8, float[] confAndStable);
  private static native void nativeClose(long ctx);
}
