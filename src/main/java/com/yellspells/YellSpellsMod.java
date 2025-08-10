package com.yellspells;

import com.yellspells.network.YellSpellsNetworking;
import com.yellspells.spells.SpellManager;
import com.yellspells.config.YellSpellsConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YellSpellsMod implements ModInitializer {
    public static final String MOD_ID = "yellspells";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static YellSpellsConfig config;
    private static SpellManager spellManager;
    
    @Override
    public void onInitialize() {
        LOGGER.info("Initializing YellSpells mod");
        
        // Initialize configuration
        config = new YellSpellsConfig();
        config.load();
        
        // Initialize spell manager
        spellManager = new SpellManager(config);
        
        // Initialize networking
        YellSpellsNetworking.init();
        
        // Register server lifecycle events
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("YellSpells server started");
            spellManager.onServerStarted(server);
        });
        
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("YellSpells server stopping");
            spellManager.onServerStopping(server);
        });
        
        LOGGER.info("YellSpells mod initialized successfully");
    }
    
    public static YellSpellsConfig getConfig() {
        return config;
    }
    
    public static SpellManager getSpellManager() {
        return spellManager;
    }
    
    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}
