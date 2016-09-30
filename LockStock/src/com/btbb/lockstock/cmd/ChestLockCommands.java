/*
 *    Copyright (C) 2016 Serge Humphrey <sergehumphrey@gmail.com>
 * 
 *    This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *   
 */
package com.btbb.lockstock.cmd;

import java.util.LinkedHashMap;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import com.btbb.lockstock.ChatFormat;
import com.btbb.lockstock.LockAPI;
import com.btbb.lockstock.LockStockPlugin;
import com.btbb.lockstock.permissions.LockPermissions;

public class ChestLockCommands implements CommandExecutor, Listener {

	LockStockPlugin plugin;
	LinkedHashMap<UUID, CLCommand> cmdPlayers;
	static final UUID CONSOLE_ID = new UUID(9001, 1009);

	public ChestLockCommands(LockStockPlugin plugin) {
		this.plugin = plugin;
		cmdPlayers = new LinkedHashMap<UUID, CLCommand>();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] params) {
		if (sender instanceof Player && LockAPI.isWorldDisabled(((Player) sender).getWorld())) {
			sender.sendMessage(ChatFormat.format("&cLockStock has been disabled for this world."));
			return true;
		}
		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (!LockPermissions.getPermissions().canPlayerUsePlugin(player)) {
				sender.sendMessage(ChatFormat.format("&cYou do not have permission to use LockStock"));
				return true;
			}
		}

		String cmdName = cmd.getName().toLowerCase();
		if (cmdName.equals("lock") || cmdName.equals("unlock")) {
			String[] args = new String[params.length + 1];
			int i = 0;
			args[i++] = cmdName;
			for (String s : params)
				args[i++] = s;
			new CLCommand(label, sender).execute(args);
		}

		if (!cmdName.equals("lockstock"))
			return false;
		if (params.length > 0) {
			new CLCommand(label, sender).execute(params);
			return true;
		}
		sender.sendMessage(ChatColor.GOLD + "Enter LockStock operation or type ? for help");
		cmdPlayers.put((sender instanceof Player) ? ((Player) sender).getUniqueId() : CONSOLE_ID, new CLCommand(label, sender));
		return true;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		UUID id = event.getPlayer().getUniqueId();
		if (cmdPlayers.containsKey(id)) {
			CLCommand cc = cmdPlayers.get(id);
			cmdPlayers.remove(id);
			cc.execute(event.getMessage());
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPrePlayerCommand(PlayerCommandPreprocessEvent event) {
		UUID id = event.getPlayer().getUniqueId();
		if (cmdPlayers.containsKey(id)) {
			event.getPlayer().sendMessage(ChatColor.GOLD + "LockStock operation cancelled.");
			cmdPlayers.remove(id);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onServerCommand(ServerCommandEvent event) {
		if (cmdPlayers.containsKey(CONSOLE_ID)) {
			CLCommand cc = cmdPlayers.get(CONSOLE_ID);
			cmdPlayers.remove(CONSOLE_ID);
			cc.execute(event.getCommand());
			event.setCancelled(true);
		}
	}

}
