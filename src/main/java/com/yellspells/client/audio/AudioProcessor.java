package com.yellspells.client.audio;

import com.yellspells.client.stt.SpeechToTextManager;

public final class AudioProcessor {

  private final AudioResampler resampler = new AudioResampler();
  private final VoiceActivityDetector vad = new VoiceActivityDetector(0.005f, 0.003f, 6);
  private final SpeechToTextManager stt = new SpeechToTextManager();

  // One batch ~320 ms @16k -> 5120 samples (multiple of 320)
  private final float[] batchBuf = new float[5120];
  private int batchWrite = 0;

  // Called from SVC event (20ms @48k, 960 samples short[])
  public void onPcm48Frame(short[] pcm48) {
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
        stt.pushBlock(batchBuf, batchWrite, speaking);
        batchWrite = 0;
      }
    }
  }
}
