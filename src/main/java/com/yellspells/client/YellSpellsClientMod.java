package com.yellspells.client;

import com.yellspells.client.audio.AudioProcessor;
import com.yellspells.network.YellSpellsNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class YellSpellsClientMod implements ClientModInitializer {

  private static AudioProcessor AUDIO;

  @Override
  public void onInitializeClient() {
    AUDIO = new AudioProcessor();           // resampler + VAD + STT worker inside
    YellSpellsNetworking.registerClient();  // session key receiver, intent sender
  }

  public static AudioProcessor audioProcessor() { return AUDIO; }
  
  public static AudioProcessor getAudioProcessor() { return AUDIO; }
}
