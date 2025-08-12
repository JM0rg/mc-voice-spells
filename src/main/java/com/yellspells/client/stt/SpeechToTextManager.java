package com.yellspells.client.stt;

import com.yellspells.YellSpellsMod;
import com.yellspells.network.YellSpellsNetworking;
import com.yellspells.network.packets.CastIntentPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Environment(EnvType.CLIENT)
public final class SpeechToTextManager {

  private final WhisperJNI whisper = new WhisperJNI();
  private final ModelManager modelManager = new ModelManager();
  private final ExecutorService exec = Executors.newSingleThreadExecutor(r -> new Thread(r, "YellSpells-STT"));

  // Reused direct buffers
  private final FloatBuffer audioBuf = ByteBuffer
      .allocateDirect(4 * 5120) // 5120 float samples
      .order(ByteOrder.nativeOrder())
      .asFloatBuffer();

  private final ByteBuffer textBuf = ByteBuffer.allocateDirect(2048).order(ByteOrder.nativeOrder());

  private final AtomicInteger nonce = new AtomicInteger(1);

  public SpeechToTextManager() {
    // Check if model exists but DO NOT auto-download
    exec.submit(() -> {
      String existingModelPath = modelManager.getModelPathIfExists();
      if (existingModelPath != null) {
        try {
          whisper.init(existingModelPath, 16000, Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
          if (whisper.available()) {
            YellSpellsMod.LOGGER.info("Whisper STT initialized (using {} implementation) with model: {}", 
              whisper.isUsingMock() ? "MOCK" : "NATIVE", existingModelPath);
          } else {
            YellSpellsMod.LOGGER.error("Failed to initialize Whisper STT - not available after init");
          }
        } catch (Exception e) {
          YellSpellsMod.LOGGER.error("Failed to initialize Whisper STT", e);
        }
      } else {
        YellSpellsMod.LOGGER.info("No Whisper model found. Voice spells will not work until model is downloaded.");
      }
    });
  }

  public void pushBlock(float[] block, int samples, boolean speaking) {
    exec.submit(() -> {
      if (!whisper.available()) {
        // silent when not initialized
        return;
      }
      // Only log when speaking
      if (speaking) {
        YellSpellsMod.LOGGER.info("STT: Processing {} samples (speaking: true)", samples);
      }
      
      audioBuf.clear();
      audioBuf.put(block, 0, samples);
      audioBuf.flip();
      whisper.push(audioBuf, samples);

      // Poll once (or loop) for a partial
      textBuf.clear();
      float[] confStable = new float[2]; // [0]=confidence, [1]=isStable(0/1)
      int wrote = whisper.poll(textBuf, confStable);
      
      if (speaking) {
        YellSpellsMod.LOGGER.info("STT: Poll result - wrote: {}, confidence: {}", wrote, confStable[0]);
      }
      
      if (wrote > 0 && confStable[0] >= 0.65f) {
        textBuf.limit(wrote);
        byte[] utf8 = new byte[wrote];
        textBuf.get(utf8);
        String partial = new String(utf8);

        YellSpellsMod.LOGGER.info("STT: Recognized text: '{}' (confidence: {})", partial, confStable[0]);
        
        String spell = SpellDetector.detectKeyword(partial); // your keyword matching
        if (spell != null) {
          YellSpellsMod.LOGGER.info("STT: SPELL DETECTED: '{}' from text: '{}'", spell, partial);
          long tick = MinecraftClient.getInstance().world == null ? 0 : MinecraftClient.getInstance().world.getTime();
          long now = System.currentTimeMillis();

          // Use player's looking direction for ray hint
          double rayX = 0.0, rayY = 0.0, rayZ = 1.0; // Default forward direction
          if (MinecraftClient.getInstance().player != null) {
            var player = MinecraftClient.getInstance().player;
            rayX = -Math.sin(Math.toRadians(player.getYaw())) * Math.cos(Math.toRadians(player.getPitch()));
            rayY = -Math.sin(Math.toRadians(player.getPitch()));
            rayZ = Math.cos(Math.toRadians(player.getYaw())) * Math.cos(Math.toRadians(player.getPitch()));
          }
          
          CastIntentPacket pkt = new CastIntentPacket(spell, confStable[0], tick, now, rayX, rayY, rayZ, nonce.getAndIncrement(), new byte[32]);
          byte[] key = YellSpellsNetworking.getClientSessionKey();
          if (key != null) {
            pkt.hmac = pkt.generateHmac(key);
            YellSpellsMod.LOGGER.info("STT: Sending cast intent for spell '{}' with confidence {}", spell, confStable[0]);
            sendIntent(pkt);
          } else {
            YellSpellsMod.LOGGER.warn("STT: Cannot send cast intent - no session key available!");
          }
        }
      }
    });
  }

  /**
   * Prompt user to download model (only call when user explicitly wants to use voice spells)
   */
  public void promptModelDownload() {
    if (!modelManager.isModelAvailable()) {
      exec.submit(() -> {
        modelManager.getModelPath().thenAccept(modelPath -> {
          if (modelPath != null) {
            try {
              whisper.init(modelPath, 16000, Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
              YellSpellsMod.LOGGER.info("Whisper STT initialized with downloaded model: {}", modelPath);
            } catch (Exception e) {
              YellSpellsMod.LOGGER.error("Failed to initialize Whisper STT after download", e);
            }
          }
        });
      });
    }
  }

  private void sendIntent(CastIntentPacket pkt) {
    MinecraftClient.getInstance().execute(() -> com.yellspells.client.net.ClientSender.sendIntent(pkt));
  }
}
