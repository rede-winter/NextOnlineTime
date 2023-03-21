package com.nextplugins.onlinetime.command;

import com.nextplugins.onlinetime.NextOnlineTime;
import com.nextplugins.onlinetime.api.player.TimedPlayer;
import com.nextplugins.onlinetime.configuration.ConfigurationManager;
import com.nextplugins.onlinetime.configuration.values.MessageValue;
import com.nextplugins.onlinetime.manager.TimedPlayerManager;
import com.nextplugins.onlinetime.npc.manager.NPCManager;
import com.nextplugins.onlinetime.npc.runnable.NPCRunnable;
import com.nextplugins.onlinetime.registry.InventoryRegistry;
import com.nextplugins.onlinetime.utils.ColorUtil;
import com.nextplugins.onlinetime.utils.LocationUtils;
import com.nextplugins.onlinetime.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Yuhtin
 * Github: https://github.com/Yuhtin
 */
public final class OnlineTimeCommand implements CommandExecutor {

    private final TimedPlayerManager timedPlayerManager =
            NextOnlineTime.getInstance().getTimedPlayerManager();
    private final InventoryRegistry inventoryRegistry =
            NextOnlineTime.getInstance().getInventoryRegistry();
    private final NPCManager npcManager = NextOnlineTime.getInstance().getNpcManager();

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando apenas pode ser executado por jogadores.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            inventoryRegistry.getMainInventory().openInventory(player);
            return true;
        }

        String subCommand = args[0];

        // see

        if (subCommand.equalsIgnoreCase("ver")) {
            Player target = null;

            try {
                target = Bukkit.getPlayer(args[1]);
            } catch (Throwable ignored) {
            }

            String name = target == null ? player.getName() : target.getName();

            TimedPlayer timedPlayer = timedPlayerManager.getPlayers().getOrDefault(name, null);
            if (timedPlayer == null) {
                player.sendMessage(MessageValue.get(MessageValue::offlinePlayer));
                return true;
            }

            player.sendMessage(MessageValue.get(MessageValue::timeOfTarget)
                    .replace("%target%", name)
                    .replace("%time%", TimeUtils.format(timedPlayer.getTimeInServer())));

            return true;
        }

        // help

        if (subCommand.equalsIgnoreCase("help")) {
            List<String> messages = sender.hasPermission("nextonlinetime.admin")
                    ? MessageValue.get(MessageValue::helpMessageAdmin)
                    : MessageValue.get(MessageValue::helpMessage);

            for (String message : messages) {
                player.sendMessage(message.replace("%label%", "tempo"));
            }

            return true;
        }

        // set npc

        if (subCommand.equalsIgnoreCase("setnpc")) {
            if (!player.hasPermission("nextonlinetime.admin")) {
                player.sendMessage(ChatColor.RED + "Você não tem permissão para utilizar este comando");
                return true;
            }

            Location location = player.getLocation();
            ConfigurationManager configManager = ConfigurationManager.of("npc.yml");

            FileConfiguration config = configManager.load();
            config.set("position", LocationUtils.serialize(location));

            try {

                config.save(configManager.getFile());

                NPCRunnable runnable = (NPCRunnable) npcManager.getRunnable();
                runnable.spawnDefault(location);

                player.sendMessage(ColorUtil.colored("&aNPC setado com sucesso."));

            } catch (Exception exception) {
                player.sendMessage(ColorUtil.colored(
                        "&cNão foi possível setar o npc, o sistema está desabilitado por falta de dependência."));
            }

            return true;
        }

        // delete npc

        if (subCommand.equalsIgnoreCase("delnpc")) {
            if (!player.hasPermission("nextonlinetime.admin")) {
                player.sendMessage(ChatColor.RED + "Você não tem permissão para utilizar este comando");
                return true;
            }

            ConfigurationManager configManager = ConfigurationManager.of("npc.yml");

            FileConfiguration config = configManager.load();
            config.set("position", "");

            try {

                config.save(configManager.getFile());

                NPCRunnable runnable = (NPCRunnable) npcManager.getRunnable();
                runnable.clear();

                player.sendMessage(ColorUtil.colored("&aNPC deletado com sucesso."));

            } catch (Exception exception) {
                player.sendMessage(ColorUtil.colored("&cNão foi possível deletar o npc."));
            }

            return true;
        }

        return true;
    }
}
