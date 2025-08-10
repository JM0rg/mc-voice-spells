package com.yellspells.network;

import com.yellspells.YellSpellsMod;
import com.yellspells.network.packets.CastIntentPacket;
import com.yellspells.network.packets.SessionKeyPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class YellSpellsNetworking {
    public static final Identifier CAST_INTENT_CHANNEL = YellSpellsMod.id("cast_intent");
    public static final Identifier SESSION_KEY_CHANNEL = YellSpellsMod.id("session_key");
    
    private static final Map<UUID, byte[]> sessionKeys = new HashMap<>();
    private static final SecureRandom random = new SecureRandom();
    
    public static void init() {
        // Server-side packet handlers
        ServerPlayNetworking.registerGlobalReceiver(CAST_INTENT_CHANNEL, (server, player, handler, buf, responseSender) -> {
            try {
                CastIntentPacket packet = CastIntentPacket.read(buf);
                handleCastIntent(server, player, packet);
            } catch (Exception e) {
                YellSpellsMod.LOGGER.error("Failed to handle cast intent packet", e);
            }
        });
    }
    
    public static void initClient() {
        // Client-side packet handlers
        ClientPlayNetworking.registerGlobalReceiver(SESSION_KEY_CHANNEL, (client, handler, buf, responseSender) -> {
            try {
                SessionKeyPacket packet = SessionKeyPacket.read(buf);
                handleSessionKey(client, packet);
            } catch (Exception e) {
                YellSpellsMod.LOGGER.error("Failed to handle session key packet", e);
            }
        });
    }
    
    public static void sendCastIntent(String spellId, float confidence, int clientTick, long timestamp, 
                                    double rayX, double rayY, double rayZ) {
        if (!ClientPlayNetworking.canSend(CAST_INTENT_CHANNEL)) {
            YellSpellsMod.LOGGER.warn("Cannot send cast intent - not connected to server");
            return;
        }
        
        try {
            CastIntentPacket packet = new CastIntentPacket(
                spellId, confidence, clientTick, timestamp, rayX, rayY, rayZ
            );
            
            PacketByteBuf buf = PacketByteBufs.create();
            packet.write(buf);
            
            ClientPlayNetworking.send(CAST_INTENT_CHANNEL, buf);
        } catch (Exception e) {
            YellSpellsMod.LOGGER.error("Failed to send cast intent", e);
        }
    }
    
    public static void sendSessionKey(ServerPlayerEntity player, byte[] sessionKey) {
        try {
            SessionKeyPacket packet = new SessionKeyPacket(sessionKey);
            PacketByteBuf buf = PacketByteBufs.create();
            packet.write(buf);
            
            ServerPlayNetworking.send(player, SESSION_KEY_CHANNEL, buf);
            sessionKeys.put(player.getUuid(), sessionKey);
        } catch (Exception e) {
            YellSpellsMod.LOGGER.error("Failed to send session key to {}", player.getName().getString(), e);
        }
    }
    
    private static void handleCastIntent(net.minecraft.server.MinecraftServer server, ServerPlayerEntity player, 
                                       CastIntentPacket packet) {
        server.execute(() -> {
            try {
                // Verify HMAC
                byte[] sessionKey = sessionKeys.get(player.getUuid());
                if (sessionKey == null) {
                    YellSpellsMod.LOGGER.warn("No session key for player {}", player.getName().getString());
                    return;
                }
                
                if (!verifyHMAC(packet, sessionKey)) {
                    YellSpellsMod.LOGGER.warn("Invalid HMAC for cast intent from {}", player.getName().getString());
                    return;
                }
                
                // Check time skew
                long timeDiff = Math.abs(System.currentTimeMillis() - packet.timestamp);
                if (timeDiff > YellSpellsMod.getConfig().maxTimeSkew) {
                    YellSpellsMod.LOGGER.warn("Time skew too large for player {}: {}ms", 
                        player.getName().getString(), timeDiff);
                    return;
                }
                
                // Process spell
                YellSpellsMod.getSpellManager().processCastIntent(player, packet);
                
            } catch (Exception e) {
                YellSpellsMod.LOGGER.error("Failed to process cast intent from {}", player.getName().getString(), e);
            }
        });
    }
    
    private static void handleSessionKey(net.minecraft.client.MinecraftClient client, SessionKeyPacket packet) {
        // Store session key for HMAC verification
        // This would be stored in a client-side manager
        YellSpellsMod.LOGGER.info("Received session key from server");
    }
    
    private static boolean verifyHMAC(CastIntentPacket packet, byte[] sessionKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(sessionKey, "HmacSHA256");
            mac.init(keySpec);
            
            // Create payload for HMAC verification
            String payload = String.format("%s:%.3f:%d:%d:%.3f:%.3f:%.3f:%s",
                packet.spellId, packet.confidence, packet.clientTick, packet.timestamp,
                packet.rayX, packet.rayY, packet.rayZ, packet.nonce);
            
            byte[] expectedHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return java.util.Arrays.equals(expectedHmac, packet.hmac);
        } catch (Exception e) {
            YellSpellsMod.LOGGER.error("Failed to verify HMAC", e);
            return false;
        }
    }
    
    public static byte[] generateSessionKey() {
        byte[] key = new byte[32];
        random.nextBytes(key);
        return key;
    }
    
    public static void removeSessionKey(UUID playerId) {
        sessionKeys.remove(playerId);
    }
}
