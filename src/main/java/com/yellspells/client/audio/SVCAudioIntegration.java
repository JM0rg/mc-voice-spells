package com.yellspells.client.audio;

import com.yellspells.client.YellSpellsClientMod;
import de.maxhenkel.voicechat.api.EventRegistration;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import de.maxhenkel.voicechat.api.events.ClientVoicechatConnectionEvent;
import de.maxhenkel.voicechat.api.events.MicrophoneMuteEvent;

public final class SVCAudioIntegration implements VoicechatPlugin {

  private volatile boolean connected = false;
  private volatile boolean muted = false;

  @Override
  public String getPluginId() {
    return "yellspells";
  }

  @Override
  public void registerEvents(EventRegistration reg) {
    reg.registerEvent(ClientVoicechatConnectionEvent.class, e -> connected = e.isConnected());
    reg.registerEvent(MicrophoneMuteEvent.class, e -> muted = e.isDisabled());
    reg.registerEvent(ClientSoundEvent.class, e -> {
      if (!connected || muted) return;
      short[] pcm48 = e.getRawAudio();              // 20ms @48kHz mono 16-bit
      YellSpellsClientMod.getAudioProcessor().onPcm48Frame(pcm48); // hand off quickly (non-blocking)
    });
  }
}
