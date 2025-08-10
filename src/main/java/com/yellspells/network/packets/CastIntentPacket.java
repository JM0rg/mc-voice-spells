package com.yellspells.network.packets;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public final class CastIntentPacket {
  public final String spellId;
  public final float confidence;
  public final long clientTick;
  public final long tsMillis;
  public final int nonce;           // simplistic; you can switch to 128-bit later
  public byte[] hmac;               // 32 bytes

  public CastIntentPacket(String spellId, float confidence, long clientTick, long tsMillis, int nonce, byte[] hmac) {
    this.spellId = spellId;
    this.confidence = confidence;
    this.clientTick = clientTick;
    this.tsMillis = tsMillis;
    this.nonce = nonce;
    this.hmac = hmac;
  }

  public void write(PacketByteBuf buf) {
    buf.writeString(spellId);
    buf.writeFloat(confidence);
    buf.writeVarLong(clientTick);
    buf.writeVarLong(tsMillis);
    buf.writeInt(nonce);
    buf.writeByteArray(hmac == null ? new byte[32] : hmac);
  }

  public static CastIntentPacket read(PacketByteBuf buf) {
    String spell = buf.readString();
    float conf = buf.readFloat();
    long tick = buf.readVarLong();
    long ts   = buf.readVarLong();
    int nonce = buf.readInt();
    byte[] hmac = new byte[32];
    buf.readBytes(hmac);
    return new CastIntentPacket(spell, conf, tick, ts, nonce, hmac);
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
      mac.update(Long.toString(tsMillis).getBytes(StandardCharsets.UTF_8));
      mac.update((byte)0);
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

  // SERVER-side application (re-raycast, cooldowns, etc.)
  public void applyServer(PlayerEntity player) {
    // TODO: rate limits, skew check, nonce replay check (keep a small LRU per player)
    // TODO: validate spell, server-side raycast, cooldowns/permissions
    // Example placeholder:
    // SpellManager.cast(spellId, player, confidence);
  }
}
