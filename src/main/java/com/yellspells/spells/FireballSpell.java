package com.yellspells.spells;

import com.yellspells.network.packets.CastIntentPacket;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class FireballSpell implements SpellManager.SpellExecutor {
    
    @Override
    public void execute(ServerPlayerEntity player, CastIntentPacket packet) {
        try {
            World world = player.getWorld();
            
            // Calculate direction from ray hint
            Vec3d direction = new Vec3d(packet.rayX, packet.rayY, packet.rayZ).normalize();
            
            // Create fireball entity
            FireballEntity fireball = new FireballEntity(EntityType.FIREBALL, world);
            fireball.setOwner(player);
            
            // Set position at player's eye level
            fireball.setPosition(player.getEyePos());
            
            // Set velocity
            fireball.setVelocity(direction.multiply(2.0)); // 2.0 speed multiplier
            
            // Spawn in world
            world.spawnEntity(fireball);
            
        } catch (Exception e) {
            com.yellspells.YellSpellsMod.LOGGER.error("Failed to execute fireball spell", e);
        }
    }
}
