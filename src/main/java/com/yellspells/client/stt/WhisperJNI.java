package com.yellspells.client.stt;

import com.yellspells.YellSpellsMod;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class WhisperJNI {

  private static boolean nativeLibraryLoaded = false;
  
  static {
    try {
      loadNativeLibrary();
      nativeLibraryLoaded = true;
      YellSpellsMod.LOGGER.info("Native Whisper library loaded successfully");
    } catch (Throwable t) {
      YellSpellsMod.LOGGER.warn("Native Whisper library not found: {}, using mock implementation for testing", t.getMessage());
    }
  }
  
  private static void loadNativeLibrary() {
    String osName = System.getProperty("os.name").toLowerCase();
    String libName;
    String libPath;
    
    if (osName.contains("win")) {
      libName = "yellspells_whisper.dll";
      libPath = "/natives/windows/" + libName;
    } else if (osName.contains("mac")) {
      libName = "yellspells_whisper.dylib";
      libPath = "/natives/macos/" + libName;
    } else {
      libName = "yellspells_whisper.so";
      libPath = "/natives/linux/" + libName;
    }
    
    try {
      // Try to load from bundled resources first
      java.io.InputStream is = WhisperJNI.class.getResourceAsStream(libPath);
      if (is != null) {
        // Extract to temp file and load
        java.nio.file.Path tempLib = java.nio.file.Files.createTempFile("yellspells_whisper", 
          osName.contains("win") ? ".dll" : (osName.contains("mac") ? ".dylib" : ".so"));
        java.nio.file.Files.copy(is, tempLib, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        is.close();
        
        System.load(tempLib.toAbsolutePath().toString());
        
        // Schedule cleanup
        tempLib.toFile().deleteOnExit();
        return;
      }
    } catch (Exception e) {
      YellSpellsMod.LOGGER.debug("Failed to load bundled library: {}", e.getMessage());
    }
    
    // Fall back to system library loading
    System.loadLibrary("yellspells_whisper");
  }

  private long ctx;

  public boolean available() { 
    return nativeLibraryLoaded && ctx != 0;
  }
  
  public boolean isUsingMock() {
    return !nativeLibraryLoaded;
  }

  public void init(String modelPath, int sampleRate, int threads) {
    if (nativeLibraryLoaded) {
      ctx = nativeInit(modelPath, sampleRate, threads);
      YellSpellsMod.LOGGER.info("Native Whisper: Initialized with model: {}", modelPath);
    } else {
      throw new RuntimeException("Native Whisper library not loaded");
    }
  }

  // push audio (float -1..1). buf must be a direct buffer; reuse it.
  public int push(FloatBuffer buf, int samples) {
    if (ctx == 0 || !nativeLibraryLoaded) return -1;
    return nativePush(ctx, buf, samples);
  }

  // poll one partial; out must be a direct UTF-8 buffer you reuse; returns bytes written or 0 if none
  public int poll(ByteBuffer outUtf8, float[] confidenceStable) {
    if (ctx == 0 || !nativeLibraryLoaded) return 0;
    return nativePoll(ctx, outUtf8, confidenceStable);
  }

  public void close() {
    if (nativeLibraryLoaded && ctx != 0) {
      nativeClose(ctx);
    }
    ctx = 0;
  }

  private static native long nativeInit(String modelPath, int sr, int threads);
  private static native int  nativePush(long ctx, FloatBuffer buf, int samples);
  private static native int  nativePoll(long ctx, ByteBuffer outUtf8, float[] confAndStable);
  private static native void nativeClose(long ctx);
}
