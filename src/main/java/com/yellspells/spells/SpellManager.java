package com.yellspells.spells;

import com.yellspells.YellSpellsMod;
import com.yellspells.config.YellSpellsConfig;
import com.yellspells.network.packets.CastIntentPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpellManager {
    private final YellSpellsConfig config;
    private final Map<UUID, Map<String, Long>> playerCooldowns = new HashMap<>();
    private final Map<String, SpellExecutor> spellExecutors = new HashMap<>();
    
    public SpellManager(YellSpellsConfig config) {
        this.config = config;
        registerSpellExecutors();
    }
    
    private void registerSpellExecutors() {
        // Only fireball for testing
        spellExecutors.put("fireball", new FireballSpell());
    }
    
    public void processCastIntent(ServerPlayerEntity player, CastIntentPacket packet) {
        try {
            // Get spell configuration
            YellSpellsConfig.SpellConfig spellConfig = config.getSpell(packet.spellId);
            if (spellConfig == null) {
                YellSpellsMod.LOGGER.warn("Unknown spell {} from player {}", packet.spellId, player.getName().getString());
                return;
            }
            
            // Check confidence threshold
            if (packet.confidence < spellConfig.confidenceThreshold) {
                YellSpellsMod.LOGGER.debug("Confidence too low for spell {} from {}: {} < {}", 
                    packet.spellId, player.getName().getString(), packet.confidence, spellConfig.confidenceThreshold);
                return;
            }
            
            // Check cooldown
            if (isOnCooldown(player.getUuid(), packet.spellId, spellConfig.cooldown)) {
                YellSpellsMod.LOGGER.debug("Spell {} on cooldown for player {}", packet.spellId, player.getName().getString());
                return;
            }
            
            // Check global cooldown
            if (isOnCooldown(player.getUuid(), "global", config.globalCooldown)) {
                YellSpellsMod.LOGGER.debug("Global cooldown active for player {}", player.getName().getString());
                return;
            }
            
            // Validate target if required
            if (spellConfig.requiresTarget && config.enableRaycastValidation) {
                if (!validateTarget(player, packet)) {
                    YellSpellsMod.LOGGER.warn("Invalid target for spell {} from player {}", 
                        packet.spellId, player.getName().getString());
                    return;
                }
            }
            
            // Execute spell
            SpellExecutor executor = spellExecutors.get(packet.spellId);
            if (executor != null) {
                executor.execute(player, packet);
                
                // Set cooldowns
                setCooldown(player.getUuid(), packet.spellId, spellConfig.cooldown);
                setCooldown(player.getUuid(), "global", config.globalCooldown);
                
                YellSpellsMod.LOGGER.info("Spell {} cast by {} with confidence {}", 
                    packet.spellId, player.getName().getString(), packet.confidence);
            } else {
                YellSpellsMod.LOGGER.warn("No executor found for spell {}", packet.spellId);
            }
            
        } catch (Exception e) {
            YellSpellsMod.LOGGER.error("Failed to process cast intent for player {}", player.getName().getString(), e);
        }
    }
    
    private boolean validateTarget(ServerPlayerEntity player, CastIntentPacket packet) {
        try {
            // Perform server-side raycast
            Vec3d start = player.getEyePos();
            Vec3d direction = new Vec3d(packet.rayX, packet.rayY, packet.rayZ).normalize();
            Vec3d end = start.add(direction.multiply(64.0)); // 64 block range
            
            RaycastContext context = new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player);
            HitResult hitResult = player.getWorld().raycast(context);
            
            if (hitResult != null && hitResult.getType() != HitResult.Type.MISS) {
                // Target is valid
                return true;
            }
            
            return false;
        } catch (Exception e) {
            YellSpellsMod.LOGGER.error("Failed to validate target", e);
            return false;
        }
    }
    
    private boolean isOnCooldown(UUID playerId, String spellId, int cooldownMs) {
        Map<String, Long> playerSpells = playerCooldowns.get(playerId);
        if (playerSpells == null) {
            return false;
        }
        
        Long lastCast = playerSpells.get(spellId);
        if (lastCast == null) {
            return false;
        }
        
        return System.currentTimeMillis() - lastCast < cooldownMs;
    }
    
    private void setCooldown(UUID playerId, String spellId, int cooldownMs) {
        playerCooldowns.computeIfAbsent(playerId, k -> new HashMap<>())
            .put(spellId, System.currentTimeMillis());
    }
    
    public static void init() {
        // Legacy compatibility method - actual initialization happens in constructor
        YellSpellsMod.LOGGER.info("SpellManager static init called");
    }
    
    public void onServerStarted(MinecraftServer server) {
        YellSpellsMod.LOGGER.info("SpellManager initialized for server");
    }
    
    public void onServerStopping(MinecraftServer server) {
        playerCooldowns.clear();
        YellSpellsMod.LOGGER.info("SpellManager cleaned up");
    }
    

    
    public interface SpellExecutor {
        void execute(ServerPlayerEntity player, CastIntentPacket packet);
    }
}
