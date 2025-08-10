package io.github.freshsupasulley.censorcraft;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import io.github.freshsupasulley.censorcraft.api.CensorCraftPlugin;
import io.github.freshsupasulley.censorcraft.api.ForgeCensorCraftPlugin;
import io.github.freshsupasulley.censorcraft.api.events.EventRegistration;
import io.github.freshsupasulley.censorcraft.network.PunishedPacket;
import io.github.freshsupasulley.censorcraft.network.SetupPacket;
import io.github.freshsupasulley.censorcraft.network.WordPacket;
import io.github.freshsupasulley.plugins.EventHandler;
import io.github.freshsupasulley.plugins.EventHandler.EventHandlerBuilder;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

@Mod(CensorCraft.MODID)
public class CensorCraft {
	
	public static final String MODID = "censorcraft";
	public static final Logger LOGGER = LogUtils.getLogger();
	
	// Packets
	public static final long HEARTBEAT_TIME = 30000, HEARTBEAT_SAFETY_NET = 5000;
	public static SimpleChannel channel;
	
	/** Used for the plugins! */
	public static EventHandler events;
	
	public CensorCraft(FMLJavaModLoadingContext context)
	{
		var modBusGroup = context.getModBusGroup();
		FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);
		// FMLClientSetupEvent.getBus(modBusGroup).addListener(this::clientSetup);
	}
	
	// Packet communication setup
	private void commonSetup(FMLCommonSetupEvent event)
	{
		// ig just bump this with each new major (and thus incompatible with the last) update?
		final int protocolVersion = 2;
		
		event.enqueueWork(() ->
		{
			channel = ChannelBuilder.named(CensorCraft.MODID).networkProtocolVersion(protocolVersion).simpleChannel();
			
			channel.configuration().clientbound().addMain(SetupPacket.class, SetupPacket.CODEC, SetupPacket::consume);
			channel.play().serverbound().addMain(WordPacket.class, WordPacket.CODEC, WordPacket::consume);
			channel.play().clientbound().addMain(PunishedPacket.class, PunishedPacket.CODEC, PunishedPacket::consume);
			
			channel.build();
		});
		
		// Plugin schenanigans
		CensorCraft.LOGGER.info("Loading CensorCraft plugins");
		var plugins = loadPlugins();
		CensorCraft.LOGGER.info("Found {} plugins", plugins.size());
		
		EventHandlerBuilder eventBuilder = new EventHandlerBuilder(CensorCraft.LOGGER);
		EventRegistration registration = eventBuilder::addEvent;
		
		for(CensorCraftPlugin plugin : plugins)
		{
			LOGGER.info("Registering events for CensorCraft plugin '{}'", plugin.getPluginId());
			
			try
			{
				plugin.registerEvents(registration);
			} catch(Exception e)
			{
				LOGGER.warn("Failed to register events for CensorCraft plugin '{}'", plugin.getPluginId(), e);
			}
		}
		
		events = eventBuilder.build();
	}
	
	public List<CensorCraftPlugin> loadPlugins()
	{
		List<CensorCraftPlugin> plugins = new ArrayList<CensorCraftPlugin>();
		
		ModList.get().getAllScanData().forEach(scan ->
		{
			scan.getAnnotations().forEach(annotationData ->
			{
				if(annotationData.annotationType().getClassName().equals(ForgeCensorCraftPlugin.class.getName()))
				{
					try
					{
						Class<?> clazz = Class.forName(annotationData.memberName());
						
						if(CensorCraftPlugin.class.isAssignableFrom(clazz))
						{
							CensorCraftPlugin plugin = (CensorCraftPlugin) clazz.getDeclaredConstructor().newInstance();
							plugins.add(plugin);
						}
					} catch(Throwable e)
					{
						CensorCraft.LOGGER.warn("Failed to load plugin '{}'", annotationData.memberName(), e);
					}
				}
			});
		});
		
		return plugins;
	}
}
