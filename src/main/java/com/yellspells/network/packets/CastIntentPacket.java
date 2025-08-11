package com.yellspells.network.packets;

import com.yellspells.YellSpellsMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public final class CastIntentPacket implements CustomPayload {
  public static final CustomPayload.Id<CastIntentPacket> ID = new CustomPayload.Id<>(YellSpellsMod.id("cast_intent"));
  public static final PacketCodec<PacketByteBuf, CastIntentPacket> CODEC = PacketCodec.of(CastIntentPacket::write, CastIntentPacket::read);
  public final String spellId;
  public final float confidence;
  public final long clientTick;
  public final long timestamp;     // renamed for consistency
  public final double rayX;        // ray direction components
  public final double rayY;
  public final double rayZ;
  public final int nonce;          // simplistic; you can switch to 128-bit later
  public byte[] hmac;              // 32 bytes

  public CastIntentPacket(String spellId, float confidence, long clientTick, long timestamp, double rayX, double rayY, double rayZ, int nonce, byte[] hmac) {
    this.spellId = spellId;
    this.confidence = confidence;
    this.clientTick = clientTick;
    this.timestamp = timestamp;
    this.rayX = rayX;
    this.rayY = rayY;
    this.rayZ = rayZ;
    this.nonce = nonce;
    this.hmac = hmac;
  }

  @Override
  public CustomPayload.Id<? extends CustomPayload> getId() {
    return ID;
  }

  public void write(PacketByteBuf buf) {
    buf.writeString(spellId);
    buf.writeFloat(confidence);
    buf.writeVarLong(clientTick);
    buf.writeVarLong(timestamp);
    buf.writeDouble(rayX);
    buf.writeDouble(rayY);
    buf.writeDouble(rayZ);
    buf.writeInt(nonce);
    buf.writeByteArray(hmac == null ? new byte[32] : hmac);
  }

  public static CastIntentPacket read(PacketByteBuf buf) {
    String spell = buf.readString();
    float conf = buf.readFloat();
    long tick = buf.readVarLong();
    long ts = buf.readVarLong();
    double rayX = buf.readDouble();
    double rayY = buf.readDouble();
    double rayZ = buf.readDouble();
    int nonce = buf.readInt();
    byte[] hmac = buf.readByteArray();
    return new CastIntentPacket(spell, conf, tick, ts, rayX, rayY, rayZ, nonce, hmac);
  }

  // CLIENT: compute HMAC (over all fields except HMAC)
  public byte[] generateHmac(byte[] key) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));
      mac.update(spellId.getBytes(StandardCharsets.UTF_8));
      mac.update((byte)0);
      mac.update(Float.toString(confidence).getBytes(StandardCharsets.UTF_8));
      mac.update((byte)0);
      mac.update(Long.toString(clientTick).getBytes(StandardCharsets.UTF_8));
      mac.update((byte)0);
      mac.update(Long.toString(timestamp).getBytes(StandardCharsets.UTF_8));
      mac.update((byte)0);
      mac.update(doubleToBytes(rayX));
      mac.update(doubleToBytes(rayY));
      mac.update(doubleToBytes(rayZ));
      mac.update(intToBytes(nonce));
      return mac.doFinal();
    } catch (GeneralSecurityException e) {
      return new byte[32];
    }
  }

  public boolean verifyHmac(byte[] key) {
    byte[] expected = generateHmac(key);
    return Arrays.equals(expected, this.hmac);
  }

  private static byte[] intToBytes(int v) {
    return new byte[] {
      (byte)(v >>> 24), (byte)(v >>> 16), (byte)(v >>> 8), (byte) v
    };
  }

  private static byte[] doubleToBytes(double v) {
    long bits = Double.doubleToLongBits(v);
    return new byte[] {
      (byte)(bits >>> 56), (byte)(bits >>> 48), (byte)(bits >>> 40), (byte)(bits >>> 32),
      (byte)(bits >>> 24), (byte)(bits >>> 16), (byte)(bits >>> 8), (byte) bits
    };
  }

  // SERVER-side application (re-raycast, cooldowns, etc.)
  public void applyServer(PlayerEntity player) {
    // Process the spell through SpellManager
    YellSpellsMod.getSpellManager().processCastIntent((net.minecraft.server.network.ServerPlayerEntity) player, this);
  }
}
