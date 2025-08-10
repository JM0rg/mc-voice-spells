package com.yellspells.spells;

import com.yellspells.network.packets.CastIntentPacket;
import net.minecraft.server.network.ServerPlayerEntity;

public class HealSpell implements SpellManager.SpellExecutor {
    
    @Override
    public void execute(ServerPlayerEntity player, CastIntentPacket packet) {
        try {
            // Heal the player by 4 hearts (8 health points)
            float currentHealth = player.getHealth();
            float maxHealth = player.getMaxHealth();
            float healAmount = Math.min(8.0f, maxHealth - currentHealth);
            
            if (healAmount > 0) {
                player.heal(healAmount);
            }
            
        } catch (Exception e) {
            com.yellspells.YellSpellsMod.LOGGER.error("Failed to execute heal spell", e);
        }
    }
}
