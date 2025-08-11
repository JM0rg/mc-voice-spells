package com.yellspells.client.audio;

import com.yellspells.YellSpellsMod;
import com.yellspells.client.stt.SpeechToTextManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class AudioProcessor {

  private final AudioResampler resampler = new AudioResampler();
  private final VoiceActivityDetector vad = new VoiceActivityDetector(0.005f, 0.003f, 6);
  private final SpeechToTextManager stt = new SpeechToTextManager();

  // One batch ~320 ms @16k -> 5120 samples (multiple of 320)
  private final float[] batchBuf = new float[5120];
  private int batchWrite = 0;
  private int frameCount = 0;

  // Called from SVC event (20ms @48k, 960 samples short[])
  public void onPcm48Frame(short[] pcm48) {
    frameCount++;
    if (frameCount % 50 == 0) { // Log every second (50 * 20ms = 1s)
      YellSpellsMod.LOGGER.debug("AudioProcessor: Processing audio frame {} (length: {})", frameCount, pcm48.length);
    }
    float[] out = new float[resampler.outLengthFor(pcm48.length)];
    int wrote = resampler.process(pcm48, out);

    // chunk into 20ms@16k blocks (320 samples) and feed VAD/STT
    int i = 0;
    while (i + 320 <= wrote) {
      boolean speaking = vad.update(out, 320);

      // accumulate regardless; STT will do internal VAD too if desired
      System.arraycopy(out, i, batchBuf, batchWrite, 320);
      batchWrite += 320;
      i += 320;

      if (batchWrite >= batchBuf.length) {
        YellSpellsMod.LOGGER.debug("AudioProcessor: Sending {} samples to STT (speaking: {})", batchWrite, speaking);
        stt.pushBlock(batchBuf, batchWrite, speaking);
        batchWrite = 0;
      }
    }
  }
  
  /**
   * Initialize STT with downloaded model (called after user consents to download)
   */
  public void initializeSTT(String modelPath) {
    YellSpellsMod.LOGGER.info("Initializing STT with user-downloaded model: {}", modelPath);
    // The SpeechToTextManager will be created with the model when first used
  }
}
