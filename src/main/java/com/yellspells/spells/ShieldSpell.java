package com.yellspells.spells;

import com.yellspells.network.packets.CastIntentPacket;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;

public class ShieldSpell implements SpellManager.SpellExecutor {
    
    @Override
    public void execute(ServerPlayerEntity player, CastIntentPacket packet) {
        try {
            // Apply resistance effect for 10 seconds (200 ticks)
            StatusEffectInstance resistance = new StatusEffectInstance(
                StatusEffects.RESISTANCE, 
                200, // duration in ticks
                1,   // amplifier (level 2 resistance)
                false, // ambient
                true   // show particles
            );
            
            player.addStatusEffect(resistance);
            
        } catch (Exception e) {
            com.yellspells.YellSpellsMod.LOGGER.error("Failed to execute shield spell", e);
        }
    }
}
