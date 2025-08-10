package com.yellspells.client;

import com.yellspells.client.audio.AudioProcessor;
import com.yellspells.client.audio.SVCAudioIntegration;
import com.yellspells.client.stt.SpeechToTextManager;
import com.yellspells.client.hud.SpellHUD;
import com.yellspells.network.YellSpellsNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YellSpellsClientMod implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("YellSpellsClient");
    
    private static AudioProcessor audioProcessor;
    private static SpeechToTextManager sttManager;
    private static SpellHUD spellHUD;
    private static SVCAudioIntegration svcIntegration;
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing YellSpells client mod");
        
        // Initialize networking
        YellSpellsNetworking.initClient();
        
        // Initialize audio processor
        audioProcessor = new AudioProcessor();
        
        // Initialize STT manager
        sttManager = new SpeechToTextManager();
        
        // Initialize HUD
        spellHUD = new SpellHUD();
        HudRenderCallback.EVENT.register(spellHUD);
        
        // Initialize SVC integration
        svcIntegration = new SVCAudioIntegration();
        
        // Register client lifecycle events
        ClientLifecycleEvents.CLIENT_LEVEL_LOAD.register(world -> {
            LOGGER.info("Client level loaded, starting audio processing");
            audioProcessor.start();
            sttManager.start();
        });
        
        ClientLifecycleEvents.CLIENT_LEVEL_UNLOAD.register(world -> {
            LOGGER.info("Client level unloading, stopping audio processing");
            audioProcessor.stop();
            sttManager.stop();
        });
        
        LOGGER.info("YellSpells client mod initialized successfully");
    }
    
    public static AudioProcessor getAudioProcessor() {
        return audioProcessor;
    }
    
    public static SpeechToTextManager getSttManager() {
        return sttManager;
    }
    
    public static SpellHUD getSpellHUD() {
        return spellHUD;
    }
}
