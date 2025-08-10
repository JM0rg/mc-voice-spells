package com.yellspells.network.packets;

import net.minecraft.network.PacketByteBuf;

public class SessionKeyPacket {
    public final byte[] sessionKey;
    
    public SessionKeyPacket(byte[] sessionKey) {
        this.sessionKey = sessionKey;
    }
    
    public void write(PacketByteBuf buf) {
        buf.writeByteArray(sessionKey);
    }
    
    public static SessionKeyPacket read(PacketByteBuf buf) {
        byte[] sessionKey = buf.readByteArray();
        return new SessionKeyPacket(sessionKey);
    }
}
