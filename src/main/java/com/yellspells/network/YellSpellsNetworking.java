package com.yellspells.network;

import com.yellspells.YellSpellsMod;
import com.yellspells.network.packets.CastIntentPacket;
import com.yellspells.network.packets.SessionKeyPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class YellSpellsNetworking {

  // Legacy compatibility
  public static final Identifier CAST_INTENT_CHANNEL = YellSpellsMod.id("cast_intent");
  public static final Identifier SESSION_KEY_CHANNEL = YellSpellsMod.id("session_key");
  public static final Identifier INTENT_ID = CAST_INTENT_CHANNEL;

  private static final Map<UUID, byte[]> SESSION_KEYS = new ConcurrentHashMap<>();
  private static final SecureRandom RNG = new SecureRandom();

  // client-cache of session key
  private static volatile byte[] clientSessionKey;

  // ===== Server wiring =====
  public static void registerServer() {
    init();
  }
  
  public static void init() {
    // Register payload types
    PayloadTypeRegistry.playS2C().register(SessionKeyPacket.ID, SessionKeyPacket.CODEC);
    PayloadTypeRegistry.playC2S().register(CastIntentPacket.ID, CastIntentPacket.CODEC);
    
    // server receiver for intents
    ServerPlayNetworking.registerGlobalReceiver(CastIntentPacket.ID, (payload, context) -> {
      YellSpellsMod.LOGGER.info("Server: Received cast intent for spell '{}' from player {}", 
        payload.spellId, context.player().getName().getString());
      
      byte[] key = SESSION_KEYS.get(context.player().getUuid());
      if (key == null) {
        YellSpellsMod.LOGGER.warn("Server: No session key found for player {}", context.player().getName().getString());
        return;
      }
      
      if (!verifyHmac(payload, key)) {
        YellSpellsMod.LOGGER.warn("Server: HMAC verification failed for player {}", context.player().getName().getString());
        return;
      }

      // TODO: add skew/nonce/rate-limits here or in pkt.applyServer
      YellSpellsMod.LOGGER.info("Server: Executing spell '{}' for player {}", payload.spellId, context.player().getName().getString());
      context.server().execute(() -> payload.applyServer(context.player()));
    });

    // send per-session key on join, clear on disconnect
    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
      byte[] key = new byte[32];
      RNG.nextBytes(key);
      SESSION_KEYS.put(handler.player.getUuid(), key);
      sendSessionKey(handler.player, key);
    });
    ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
      SESSION_KEYS.remove(handler.player.getUuid())
    );
  }

  private static void sendSessionKey(ServerPlayerEntity player, byte[] key) {
    YellSpellsMod.LOGGER.info("Server: Sending session key to player {} (length: {})", 
      player.getName().getString(), key.length);
    ServerPlayNetworking.send(player, new SessionKeyPacket(key));
  }

  private static boolean verifyHmac(CastIntentPacket pkt, byte[] key) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));
      mac.update(pkt.spellId.getBytes(StandardCharsets.UTF_8));
      mac.update((byte) 0);
      mac.update(Float.toString(pkt.confidence).getBytes(StandardCharsets.UTF_8));
      mac.update((byte) 0);
      mac.update(Long.toString(pkt.clientTick).getBytes(StandardCharsets.UTF_8));
      mac.update((byte) 0);
      mac.update(Long.toString(pkt.timestamp).getBytes(StandardCharsets.UTF_8));
      mac.update((byte) 0);
      mac.update(doubleToBytes(pkt.rayX));
      mac.update(doubleToBytes(pkt.rayY));
      mac.update(doubleToBytes(pkt.rayZ));
      mac.update(intToBytes(pkt.nonce));
      byte[] expected = mac.doFinal();
      return java.util.Arrays.equals(expected, pkt.hmac);
    } catch (Exception e) {
      return false;
    }
  }

  private static byte[] intToBytes(int v) {
    return new byte[]{ (byte)(v>>>24), (byte)(v>>>16), (byte)(v>>>8), (byte)v };
  }
  private static byte[] doubleToBytes(double d) {
    long v = Double.doubleToLongBits(d);
    return new byte[]{ (byte)(v>>>56),(byte)(v>>>48),(byte)(v>>>40),(byte)(v>>>32),(byte)(v>>>24),(byte)(v>>>16),(byte)(v>>>8),(byte)v };
  }

  // ===== Client wiring =====
  public static void registerClient() {
    initClient();
  }
  
  public static void initClient() {
    ClientPlayNetworking.registerGlobalReceiver(SessionKeyPacket.ID, (payload, context) -> {
      context.client().execute(() -> {
        clientSessionKey = payload.sessionKey();
        YellSpellsMod.LOGGER.info("Received session key from server (length: {})", clientSessionKey.length);
      });
    });
  }

  public static byte[] getClientSessionKey() {
    return clientSessionKey;
  }

  public static void sendCastIntent(String spellId, float confidence, int clientTick, long timestamp,
                                    double rayX, double rayY, double rayZ) {
    if (!ClientPlayNetworking.canSend(CAST_INTENT_CHANNEL)) return;
    byte[] key = clientSessionKey;
    if (key == null) return;

    int nonce = RNG.nextInt();
    CastIntentPacket pkt = new CastIntentPacket(spellId, confidence, clientTick, timestamp, rayX, rayY, rayZ, nonce, null);
    pkt.hmac = hmac(pkt, key);

    ClientPlayNetworking.send(pkt);
  }

  private static byte[] hmac(CastIntentPacket pkt, byte[] key) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));
      mac.update(pkt.spellId.getBytes(StandardCharsets.UTF_8));
      mac.update((byte) 0);
      mac.update(Float.toString(pkt.confidence).getBytes(StandardCharsets.UTF_8));
      mac.update((byte) 0);
      mac.update(Long.toString(pkt.clientTick).getBytes(StandardCharsets.UTF_8));
      mac.update((byte) 0);
      mac.update(Long.toString(pkt.timestamp).getBytes(StandardCharsets.UTF_8));
      mac.update((byte) 0);
      mac.update(doubleToBytes(pkt.rayX));
      mac.update(doubleToBytes(pkt.rayY));
      mac.update(doubleToBytes(pkt.rayZ));
      mac.update(intToBytes(pkt.nonce));
      return mac.doFinal();
    } catch (Exception e) {
      return new byte[32];
    }
  }
}
