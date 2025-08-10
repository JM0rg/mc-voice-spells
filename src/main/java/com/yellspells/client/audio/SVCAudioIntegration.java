package com.yellspells.client.audio;

import com.yellspells.client.YellSpellsClientMod;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatClientApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audio.AudioPlayer;
import de.maxhenkel.voicechat.api.audio.ClientAudioPlayer;
import de.maxhenkel.voicechat.api.events.ClientVoicechatConnectionEvent;
import de.maxhenkel.voicechat.api.events.MicrophoneMuteEvent;
import de.maxhenkel.voicechat.api.events.SoundPacketEvent;
import de.maxhenkel.voicechat.api.mp3.Mp3Decoder;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoderMode;
import de.maxhenkel.voicechat.api.packet.Packet;
import de.maxhenkel.voicechat.api.packet.SoundPacket;
import de.maxhenkel.voicechat.api.volume.EntityVolumeManager;
import de.maxhenkel.voicechat.api.volume.LocationalVolumeManager;
import de.maxhenkel.voicechat.api.volume.VolumeManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;

public class SVCAudioIntegration implements VoicechatPlugin {
    private static final ResourceLocation ADDON_ID = new ResourceLocation("yellspells", "audio_integration");
    private AudioProcessor audioProcessor;
    
    public SVCAudioIntegration() {
        this.audioProcessor = YellSpellsClientMod.getAudioProcessor();
    }
    
    @Override
    public String getPluginId() {
        return "yellspells";
    }
    
    @Override
    public void initialize(VoicechatApi api) {
        // Register for client-side events
        if (api instanceof VoicechatClientApi clientApi) {
            clientApi.registerClientSoundPacketListener(this::onClientSoundPacket);
            clientApi.registerClientVoicechatConnectionListener(this::onClientVoicechatConnection);
            clientApi.registerMicrophoneMuteListener(this::onMicrophoneMute);
        }
    }
    
    private void onClientSoundPacket(SoundPacketEvent event) {
        try {
            SoundPacket packet = event.getPacket();
            if (packet == null) return;
            
            // Get raw PCM data from SVC
            short[] pcmData = packet.getData();
            if (pcmData == null || pcmData.length == 0) return;
            
            // Process through our audio pipeline
            if (audioProcessor != null && audioProcessor.isRunning()) {
                audioProcessor.processAudioFrame(pcmData, 48000); // SVC uses 48kHz
            }
            
        } catch (Exception e) {
            // Log but don't break SVC
            System.err.println("YellSpells: Error processing SVC audio packet: " + e.getMessage());
        }
    }
    
    private void onClientVoicechatConnection(ClientVoicechatConnectionEvent event) {
        VoicechatConnection connection = event.getConnection();
        if (connection != null) {
            // SVC connected, ensure our audio processor is running
            if (audioProcessor != null) {
                audioProcessor.start();
            }
        } else {
            // SVC disconnected, stop our audio processor
            if (audioProcessor != null) {
                audioProcessor.stop();
            }
        }
    }
    
    private void onMicrophoneMute(MicrophoneMuteEvent event) {
        boolean muted = event.isMuted();
        // Optionally handle mute state changes
        // Could pause audio processing when muted to save resources
    }
    
    // Required interface methods (unused for our addon)
    @Override
    public Map<String, String> getLanguageMap() { return null; }
    
    @Override
    public String getLanguagePath() { return null; }
    
    @Override
    public boolean isBuiltIn() { return false; }
    
    @Override
    public boolean hasClientSide() { return true; }
    
    @Override
    public boolean hasServerSide() { return false; }
}
