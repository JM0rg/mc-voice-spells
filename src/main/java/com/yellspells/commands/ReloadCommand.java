package com.yellspells.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.yellspells.YellSpellsMod;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public final class ReloadCommand {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    dispatcher.register(CommandManager.literal("yellspells")
      .then(CommandManager.literal("reload")
        .requires(src -> src.hasPermissionLevel(2))
        .executes(ctx -> {
          try {
            YellSpellsMod.getConfig().load();
            ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("Reloaded yellspells.json"), true);
            return 1;
          } catch (Exception e) {
            ctx.getSource().sendError(net.minecraft.text.Text.literal("Failed to reload yellspells.json"));
            return 0;
          }
        })
      )
    );
  }
}


