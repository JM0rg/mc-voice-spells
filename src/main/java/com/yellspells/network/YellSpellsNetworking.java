package com.yellspells.network;

import com.yellspells.YellSpellsMod;
import com.yellspells.network.packets.CastIntentPacket;
import com.yellspells.network.packets.SessionKeyPacket;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class YellSpellsNetworking {

  public static final Identifier HANDSHAKE_ID = new Identifier(YellSpellsMod.MODID, "handshake");
  public static final Identifier INTENT_ID    = new Identifier(YellSpellsMod.MODID, "intent");

  // SERVER: per-session keys, keyed by player UUID
  private static final Map<UUID, byte[]> SESSION_KEYS = new ConcurrentHashMap<>();
  private static final SecureRandom RNG = new SecureRandom();

  // CLIENT: latest session key
  private static volatile byte[] clientSessionKey;

  // ======== SERVER ========
  public static void registerServer() {
    // Send session key on join
    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
      byte[] key = new byte[32];
      RNG.nextBytes(key);
      SESSION_KEYS.put(handler.player.getUuid(), key);
      sendSessionKey(handler.player, key);
    });
    // Remove on quit
    ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
      SESSION_KEYS.remove(handler.player.getUuid())
    );

    // Receive intent
    ServerPlayNetworking.registerGlobalReceiver(INTENT_ID, (server, player, handler, buf, responseSender) -> {
      CastIntentPacket pkt = CastIntentPacket.read(buf);
      byte[] key = SESSION_KEYS.get(player.getUuid());
      if (key == null || !pkt.verifyHmac(key)) {
        // silently drop or log
        return;
      }
      // Rate limits, skew, nonce checks (implement inside pkt or here)
      server.execute(() -> pkt.applyServer(player)); // raycast, cooldowns, execute spell
    });
  }

  private static void sendSessionKey(ServerPlayerEntity player, byte[] key) {
    PacketByteBuf out = PacketByteBufs.create();
    new SessionKeyPacket(key).write(out);
    ServerPlayNetworking.send(player, HANDSHAKE_ID, out);
  }

  // ======== CLIENT ========
  public static void registerClient() {
    ClientPlayNetworking.registerGlobalReceiver(HANDSHAKE_ID, (client, handler, buf, responseSender) -> {
      SessionKeyPacket pkt = SessionKeyPacket.read(buf);
      client.execute(() -> clientSessionKey = pkt.sessionKey());
    });
  }

  public static byte[] getClientSessionKey() { return clientSessionKey; }
}
