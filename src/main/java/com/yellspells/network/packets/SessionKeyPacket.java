package com.yellspells.network.packets;

import net.minecraft.network.PacketByteBuf;

public record SessionKeyPacket(byte[] sessionKey) {

  public void write(PacketByteBuf buf) {
    buf.writeVarInt(sessionKey.length);
    buf.writeByteArray(sessionKey);
  }

  public static SessionKeyPacket read(PacketByteBuf buf) {
    int len = buf.readVarInt();
    byte[] key = new byte[len];
    buf.readBytes(key);
    return new SessionKeyPacket(key);
  }
}
