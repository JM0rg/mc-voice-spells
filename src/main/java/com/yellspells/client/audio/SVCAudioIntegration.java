package com.yellspells.client.audio;

import com.yellspells.YellSpellsMod;
import com.yellspells.client.YellSpellsClientMod;
import com.yellspells.client.gui.ModelDownloadScreen;
import com.yellspells.client.stt.ModelManager;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import de.maxhenkel.voicechat.api.events.ClientVoicechatConnectionEvent;
import de.maxhenkel.voicechat.api.events.MicrophoneMuteEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)

public final class SVCAudioIntegration implements VoicechatPlugin {

  private volatile boolean connected = false;
  private volatile boolean muted = false;
  private boolean hasShownPrompt = false;

  @Override
  public String getPluginId() {
    return "yellspells";
  }

  @Override
  public void registerEvents(EventRegistration reg) {
    reg.registerEvent(ClientVoicechatConnectionEvent.class, e -> {
      connected = e.isConnected();
      YellSpellsMod.LOGGER.info("SVC Connection changed: connected={}", connected);
      
      // Show model download prompt when first connected (if model not available)
      if (connected && !hasShownPrompt) {
        hasShownPrompt = true;
        
        ModelManager modelManager = new ModelManager();
        if (!modelManager.isModelAvailable()) {
          YellSpellsMod.LOGGER.info("Model not available, showing download prompt");
          // Show popup after a short delay to ensure client is ready
          new Thread(() -> {
            try {
              Thread.sleep(1500); // 1.5 second delay
              MinecraftClient.getInstance().execute(() -> {
                MinecraftClient.getInstance().setScreen(new ModelDownloadScreen(MinecraftClient.getInstance().currentScreen));
              });
            } catch (InterruptedException ex) {
              Thread.currentThread().interrupt();
            }
          }).start();
        } else {
          YellSpellsMod.LOGGER.info("Model already available, no download prompt needed");
        }
      }
    });
    
    reg.registerEvent(MicrophoneMuteEvent.class, e -> {
      muted = e.isDisabled();
      YellSpellsMod.LOGGER.info("SVC Microphone changed: muted={}", muted);
    });
    
    // Listen for client sound events - this should capture audio
    reg.registerEvent(ClientSoundEvent.class, e -> {
      YellSpellsMod.LOGGER.debug("SVC ClientSoundEvent triggered - connected: {}, muted: {}", connected, muted);
      
      if (!connected || muted) {
        return;
      }
      
      short[] pcm48 = e.getRawAudio();
      if (pcm48 != null && pcm48.length > 0) {
        // Removed spam logging - audio is being processed
        YellSpellsClientMod.getAudioProcessor().onPcm48Frame(pcm48);
      } else {
        YellSpellsMod.LOGGER.debug("SVC Audio was null or empty");
      }
    });
    
    YellSpellsMod.LOGGER.info("YellSpells SVC events registered successfully");
  }
}
