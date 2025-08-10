package io.github.freshsupasulley.censorcraft.network;

import java.util.List;
import java.util.stream.Collectors;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public class Participant {
	
	private String name;
	private long lastHeartbeat = System.currentTimeMillis(), lastPunishment; // intentionally setting lastPunishment to nothing, so the cooldown doesn't apply on start
	
	// Hold 200 characters
	private static final int BUFFER_SIZE = 200;
	private StringBuilder buffer = new StringBuilder(BUFFER_SIZE);
	
	public Participant(String name)
	{
		this.name = name;
	}
	
	public String appendWord(String word)
	{
		// Separate words with spaces
		String string = word + " ";
		
		if(string.length() >= BUFFER_SIZE)
		{
			buffer.setLength(0);
			buffer.append(string.substring(string.length() - BUFFER_SIZE));
		}
		else
		{
			int overflow = buffer.length() + string.length() - BUFFER_SIZE;
			
			if(overflow > 0)
			{
				buffer.delete(0, overflow);
			}
			
			buffer.append(string);
		}
		
		return buffer.toString();
	}
	
	public void punish(List<Punishment> punishments, ServerPlayer player)
	{
		CensorCraft.LOGGER.debug("Sending punishment packet");
		CensorCraft.channel.send(new PunishedPacket(punishments.stream().map(Punishment::getName).collect(Collectors.toList()).toArray(String[]::new)), PacketDistributor.PLAYER.with(player));
		buffer.setLength(0);
		lastPunishment = System.currentTimeMillis();
		heartbeat();
	}
	
	public void heartbeat()
	{
		this.lastHeartbeat = System.currentTimeMillis();
	}
	
	public String getName()
	{
		return name;
	}
	
	public long getLastHeartbeat()
	{
		return lastHeartbeat;
	}
	
	public long getLastPunishment()
	{
		return lastPunishment;
	}
}
