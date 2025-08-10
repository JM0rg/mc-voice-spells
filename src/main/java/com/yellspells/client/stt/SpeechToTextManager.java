package com.yellspells.client.stt;

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
  private final ExecutorService exec = Executors.newSingleThreadExecutor(r -> new Thread(r, "YellSpells-STT"));

  // Reused direct buffers
  private final FloatBuffer audioBuf = ByteBuffer
      .allocateDirect(4 * 5120) // 5120 float samples
      .order(ByteOrder.nativeOrder())
      .asFloatBuffer();

  private final ByteBuffer textBuf = ByteBuffer.allocateDirect(2048).order(ByteOrder.nativeOrder());

  private final AtomicInteger nonce = new AtomicInteger(1);

  public SpeechToTextManager() {
    // TODO: model manager; for now assume a model path is present and init off-thread
    exec.submit(() -> {
      try {
        whisper.init(getDefaultModelPath(), 16000, Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
      } catch (Throwable ignored) {}
    });
  }

  public void pushBlock(float[] block, int samples, boolean speaking) {
    exec.submit(() -> {
      if (!whisper.available()) return;
      audioBuf.clear();
      audioBuf.put(block, 0, samples);
      audioBuf.flip();
      whisper.push(audioBuf, samples);

      // Poll once (or loop) for a partial
      textBuf.clear();
      float[] confStable = new float[2]; // [0]=confidence, [1]=isStable(0/1)
      int wrote = whisper.poll(textBuf, confStable);
      if (wrote > 0 && confStable[0] >= 0.65f) {
        textBuf.limit(wrote);
        byte[] utf8 = new byte[wrote];
        textBuf.get(utf8);
        String partial = new String(utf8);

        String spell = SpellDetector.detectKeyword(partial); // your keyword matching
        if (spell != null) {
          long tick = MinecraftClient.getInstance().world == null ? 0 : MinecraftClient.getInstance().world.getTime();
          long now = System.currentTimeMillis();

          CastIntentPacket pkt = new CastIntentPacket(spell, confStable[0], tick, now, nonce.getAndIncrement(), new byte[32]);
          byte[] key = YellSpellsNetworking.getClientSessionKey();
          if (key != null) {
            pkt.hmac = pkt.generateHmac(key);
            sendIntent(pkt);
          }
        }
      }
    });
  }

  private void sendIntent(CastIntentPacket pkt) {
    MinecraftClient.getInstance().execute(() -> com.yellspells.client.net.ClientSender.sendIntent(pkt));
  }

  private String getDefaultModelPath() {
    // TODO: resolve from your model manager/cache dir
    return System.getProperty("user.home") + "/.yellspells/models/ggml-tiny.en.bin";
  }
}
