package com.yellspells;

import com.yellspells.config.YellSpellsConfig;
import com.yellspells.commands.ReloadCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import com.yellspells.network.YellSpellsNetworking;
import com.yellspells.spells.SpellManager;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class YellSpellsMod implements ModInitializer {
  public static final String MODID = "yellspells";
  public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
  
  private static YellSpellsConfig config;
  private static SpellManager spellManager;

  @Override
  public void onInitialize() {
    LOGGER.info("Initializing YellSpells mod");
    
    // Load configuration
    config = new YellSpellsConfig();
    config.load();
    
    // Initialize spell manager
    spellManager = new SpellManager(config);
    
    // Register networking
    YellSpellsNetworking.registerServer();
    // Register commands
    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> ReloadCommand.register(dispatcher));
    
    LOGGER.info("YellSpells mod initialized");
  }
  
  public static Identifier id(String path) {
    return Identifier.of(MODID, path);
  }
  
  public static YellSpellsConfig getConfig() {
    return config;
  }
  
  public static SpellManager getSpellManager() {
    return spellManager;
  }
}
