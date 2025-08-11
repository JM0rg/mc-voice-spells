package com.yellspells.network.packets;

import com.yellspells.YellSpellsMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record SessionKeyPacket(byte[] sessionKey) implements CustomPayload {
  public static final CustomPayload.Id<SessionKeyPacket> ID = new CustomPayload.Id<>(YellSpellsMod.id("session_key"));
  public static final PacketCodec<PacketByteBuf, SessionKeyPacket> CODEC = PacketCodec.of(SessionKeyPacket::write, SessionKeyPacket::read);

  @Override
  public CustomPayload.Id<? extends CustomPayload> getId() {
    return ID;
  }

  public void write(PacketByteBuf buf) {
    buf.writeByteArray(sessionKey);
  }

  public static SessionKeyPacket read(PacketByteBuf buf) {
    byte[] key = buf.readByteArray();
    return new SessionKeyPacket(key);
  }
}
