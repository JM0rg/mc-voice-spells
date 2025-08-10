package com.yellspells;

import com.yellspells.network.YellSpellsNetworking;
import com.yellspells.spells.SpellManager;
import net.fabricmc.api.ModInitializer;

public final class YellSpellsMod implements ModInitializer {
  public static final String MODID = "yellspells";

  @Override
  public void onInitialize() {
    SpellManager.init();
    YellSpellsNetworking.registerServer(); // receivers + join/quit session key handling
  }
}
