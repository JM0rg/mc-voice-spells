package com.yellspells.network.packets;

import com.yellspells.YellSpellsMod;
import net.minecraft.network.PacketByteBuf;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.UUID;

public class CastIntentPacket {
    public final String spellId;
    public final float confidence;
    public final int clientTick;
    public final long timestamp;
    public final double rayX, rayY, rayZ;
    public final String nonce;
    public final byte[] hmac;
    
    private static final SecureRandom random = new SecureRandom();
    
    public CastIntentPacket(String spellId, float confidence, int clientTick, long timestamp, 
                           double rayX, double rayY, double rayZ) {
        this.spellId = spellId;
        this.confidence = confidence;
        this.clientTick = clientTick;
        this.timestamp = timestamp;
        this.rayX = rayX;
        this.rayY = rayY;
        this.rayZ = rayZ;
        this.nonce = generateNonce();
        this.hmac = new byte[32]; // Will be set by client-side HMAC generation
    }
    
    public CastIntentPacket(String spellId, float confidence, int clientTick, long timestamp,
                           double rayX, double rayY, double rayZ, String nonce, byte[] hmac) {
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
    
    public void write(PacketByteBuf buf) {
        buf.writeString(spellId);
        buf.writeFloat(confidence);
        buf.writeInt(clientTick);
        buf.writeLong(timestamp);
        buf.writeDouble(rayX);
        buf.writeDouble(rayY);
        buf.writeDouble(rayZ);
        buf.writeString(nonce);
        buf.writeByteArray(hmac);
    }
    
    public static CastIntentPacket read(PacketByteBuf buf) {
        String spellId = buf.readString();
        float confidence = buf.readFloat();
        int clientTick = buf.readInt();
        long timestamp = buf.readLong();
        double rayX = buf.readDouble();
        double rayY = buf.readDouble();
        double rayZ = buf.readDouble();
        String nonce = buf.readString();
        byte[] hmac = buf.readByteArray();
        
        return new CastIntentPacket(spellId, confidence, clientTick, timestamp, rayX, rayY, rayZ, nonce, hmac);
    }
    
    private static String generateNonce() {
        return UUID.randomUUID().toString();
    }
    
    public byte[] generateHMAC(byte[] sessionKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(sessionKey, "HmacSHA256");
            mac.init(keySpec);
            
            // Create payload for HMAC
            String payload = String.format("%s:%.3f:%d:%d:%.3f:%.3f:%.3f:%s",
                spellId, confidence, clientTick, timestamp, rayX, rayY, rayZ, nonce);
            
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            YellSpellsMod.LOGGER.error("Failed to generate HMAC", e);
            return new byte[32];
        }
    }
}
