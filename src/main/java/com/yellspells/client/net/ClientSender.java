package com.yellspells.client.net;

import com.yellspells.network.YellSpellsNetworking;
import com.yellspells.network.packets.CastIntentPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

public final class ClientSender {
  private ClientSender() {}

  public static void sendIntent(CastIntentPacket pkt) {
    var buf = PacketByteBufs.create();
    pkt.write(buf);
    ClientPlayNetworking.send(YellSpellsNetworking.INTENT_ID, buf);
  }
}
