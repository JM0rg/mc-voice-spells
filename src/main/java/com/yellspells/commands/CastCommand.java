package com.yellspells.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.yellspells.YellSpellsMod;
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
            
            // Try to execute the spell
            SpellManager spellManager = YellSpellsMod.getSpellManager();
            boolean success = spellManager.executeSpell(player, spellName, targetPos);
            
            if (success) {
                player.sendMessage(Text.literal("§a[YellSpells] Cast " + spellName + "!"), false);
                YellSpellsMod.LOGGER.info("Debug command: Successfully cast {} at {}", spellName, targetPos);
            } else {
                player.sendMessage(Text.literal("§c[YellSpells] Failed to cast " + spellName + " (cooldown/permission?)"), false);
                YellSpellsMod.LOGGER.info("Debug command: Failed to cast {} (cooldown/permission)", spellName);
            }
            
            return success ? 1 : 0;
            
        } catch (Exception e) {
            YellSpellsMod.LOGGER.error("Error in cast command", e);
            return 0;
        }
    }
}
