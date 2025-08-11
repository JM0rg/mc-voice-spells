package com.yellspells.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.yellspells.YellSpellsMod;
import com.yellspells.network.packets.CastIntentPacket;
import com.yellspells.spells.SpellManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class CastCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("cast")
            .then(CommandManager.argument("spell", StringArgumentType.string())
                .executes(CastCommand::executeSpell)));
    }
    
    private static int executeSpell(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();
            String spellName = StringArgumentType.getString(context, "spell");
            
            YellSpellsMod.LOGGER.info("Debug command: Player {} casting spell '{}'", player.getName().getString(), spellName);
            
            // Perform raycast to get target location
            Vec3d start = player.getEyePos();
            Vec3d direction = player.getRotationVector();
            Vec3d end = start.add(direction.multiply(64.0)); // 64 block range
            
            RaycastContext raycastContext = new RaycastContext(
                start, 
                end, 
                RaycastContext.ShapeType.OUTLINE, 
                RaycastContext.FluidHandling.NONE, 
                player
            );
            
            BlockHitResult hitResult = player.getWorld().raycast(raycastContext);
            
            if (hitResult.getType() == HitResult.Type.MISS) {
                player.sendMessage(Text.literal("§c[YellSpells] No target found! Look at a block."), false);
                return 0;
            }
            
            Vec3d targetPos = hitResult.getPos();
            
            // Calculate direction vector (same as voice command)
            Vec3d playerPos = player.getEyePos();
            Vec3d rayDirection = targetPos.subtract(playerPos).normalize();
            
            // Create the same packet as voice command uses
            CastIntentPacket packet = new CastIntentPacket(
                spellName,
                1.0f, // max confidence for debug
                player.getWorld().getTime(),
                System.currentTimeMillis(),
                rayDirection.x, rayDirection.y, rayDirection.z,
                0, // nonce
                new byte[32] // hmac (empty for debug)
            );
            
            // Use the same spell execution path as voice commands
            SpellManager spellManager = YellSpellsMod.getSpellManager();
            try {
                spellManager.processCastIntent(player, packet);
                player.sendMessage(Text.literal("§a[YellSpells] Cast " + spellName + "!"), false);
                YellSpellsMod.LOGGER.info("Debug command: Successfully cast {} at {}", spellName, targetPos);
                return 1;
            } catch (Exception e) {
                player.sendMessage(Text.literal("§c[YellSpells] Failed to cast " + spellName + " (error: " + e.getMessage() + ")"), false);
                YellSpellsMod.LOGGER.error("Debug command: Failed to cast {}", spellName, e);
                return 0;
            }
            
        } catch (Exception e) {
            YellSpellsMod.LOGGER.error("Error in cast command", e);
            return 0;
        }
    }
}
