package com.yellspells.spells;

import com.yellspells.network.packets.CastIntentPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class LightningSpell implements SpellManager.SpellExecutor {
    
    @Override
    public void execute(ServerPlayerEntity player, CastIntentPacket packet) {
        try {
            World world = player.getWorld();
            
            // Calculate target position from ray hint
            Vec3d direction = new Vec3d(packet.rayX, packet.rayY, packet.rayZ).normalize();
            Vec3d targetPos = player.getEyePos().add(direction.multiply(32.0)); // 32 block range
            
            // Find the highest block at target position
            BlockPos targetBlock = new BlockPos((int) targetPos.x, (int) targetPos.y, (int) targetPos.z);
            while (targetBlock.getY() > world.getBottomY() && world.getBlockState(targetBlock).isAir()) {
                targetBlock = targetBlock.down();
            }
            
            // Strike lightning at the target
            if (world.getBlockState(targetBlock).isSolidBlock(world, targetBlock)) {
                world.createLightning(targetBlock.up());
            }
            
        } catch (Exception e) {
            com.yellspells.YellSpellsMod.LOGGER.error("Failed to execute lightning spell", e);
        }
    }
}
