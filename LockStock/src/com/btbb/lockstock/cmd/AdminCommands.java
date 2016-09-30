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

import java.util.List;
import java.util.UUID;
import java.util.regex.PatternSyntaxException;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.btbb.bukkit.PlayerUtils;
import com.btbb.lockstock.ChatFormat;
import com.btbb.lockstock.ChatFormat.PageBuilder;
import com.btbb.lockstock.ChestLock;
import com.btbb.lockstock.LockAPI;
import com.btbb.lockstock.LockStockPlugin;
import com.btbb.lockstock.db.SQLiteImporter;
import com.btbb.lockstock.permissions.LockPermissions;

public class AdminCommands implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!command.getName().equalsIgnoreCase("lockstockadmin"))
			return false;
		boolean isConsole = !(sender instanceof Player);
		Player player = null;
		if (!isConsole)
			player = (Player) sender;

		boolean canClearplayer = isConsole || LockPermissions.getPermissions().canAdminClearplayer(player);
		boolean canImport = isConsole || LockPermissions.getPermissions().canAdminImportFromSqlite(player);
		boolean canListplayer = isConsole || LockPermissions.getPermissions().canAdminListplayer(player);
		boolean canUpdatename = isConsole || LockPermissions.getPermissions().canAdminUpdatename(player);

		if (!canClearplayer && !canImport && !canListplayer && !canUpdatename) {
			msgNoPerms(sender);
			return true;
		}
		if (args.length < 1) {
			msg(sender, "&6Use &f/%1 ?&6 for help", label);
			return true;
		}
		if (args[0].equals("?") || args[0].equalsIgnoreCase("help")) {
			if (args.length > 1 && args[1].equalsIgnoreCase("listplayer")) {
				msg(sender, "&e/%1 listplayer <player> [world] [|<regex>] [page]", label);
				msg(sender, "&f- lists locks by player");
				msg(sender, "&fuse |<keyword> to use a search keyword");
				msg(sender, "&fuse |$<regex> to use a regular expression");
				msg(sender, "&eExamples:");

				msg(sender, "&7/%1 ? listplayer Notch |Chest &f - seaches for 'Chest'", label);

				msg(sender, "&7/%1 ? listplayer Notch |$\\(([\\w]+:)?-16,-?[0-9]+,-?[0-9]+\\)\\WPrivate.+ \n"
						+ " &f- seaches for x = '-16' and type = 'Private'", label);

				return true;
			}
			msg(sender, "&6LockStock Admin Commands:");
			if (canClearplayer) {
				msg(sender, "&e/%1 clearplayer <player> [world]", label);
				msg(sender, "  &f - deletes all locks owned by player");
			}
			if (canListplayer) {
				msg(sender, "&e/%1 listplayer <player> [world] [|<regex>] [page]", label);
				msg(sender, "  &f - lists locks by player");
				msg(sender, "  &f - &7/%1 ? listplayer &ffor help on this command", label);
			}
			if (canUpdatename) {
				msg(sender, "&e/%1 updatename <new playername>", label);
				msg(sender, "  &f - updates player name to current one from Mojang");
			}
			if (canImport) {
				msg(sender, "&e/%1 importfromsqlite", label);
				msg(sender, "  &f - imports from sqlite database");
			}
			return true;
		}
		if (args[0].equalsIgnoreCase("clearplayer")) {
			if (!canClearplayer) {
				msgNoPerms(sender);
				return true;
			}
			if (args.length < 2) {
				msg(sender, "&7Usage: /%1 %2 <player> [world]", label, args[0]);
				return true;
			}
			String world = null;
			if (args.length > 2)
				world = args[2];
			UUID p = PlayerUtils.getUUID(args[1]);
			if (p == null) {
				msg(sender, "&cUnable to find player: %1", args[1]);
				return true;
			}
			int c = LockAPI.clearPlayer(p, world);
			msg(sender, "&6Cleared player %1's %2 locks" + ((world != null) ? " from %3" : ""), args[1], c, world);
			return true;
		} else if (args[0].equalsIgnoreCase("updatename")) {
			if (!canUpdatename) {
				msgNoPerms(sender);
				return true;
			}
			if (args.length < 2) {
				msg(sender, "&7Usage: /%1 %2 <new player name (case sensitive)>", label, args[0]);
				return true;
			}
			UUID uuid = PlayerUtils.getMojangUUID(args[1]);
			if (uuid == null) {
				msg(sender, "&7Unable to find player: &e%1", args[1]);
				msg(sender, "&7Is it spelled correctly? Minecraft names are case sensitve");
				return true;
			}
			int rows = LockAPI.updatePlayername(uuid, args[1]);
			msg(sender, "&6Updated player's name affecting %1 rows", rows);
			return true;
		} else if (args[0].equalsIgnoreCase("importfromsqlite")) {
			if (!canImport) {
				msgNoPerms(sender);
				return true;
			}
			if (!LockStockPlugin.lsp.useMysql()) {
				msg(sender, "&cMySQL is not enabled.");
				return true;
			}
			msg(sender, "&6Importing from SQLite....");
			int ind = SQLiteImporter.importFromSQLite();

			if (ind >= 0)
				msg(sender, "&6Imported %1 locks.", ind);
			else
				msg(sender, "&cImport failed.");
			return true;
		} else if (args[0].equalsIgnoreCase("listplayer")) {
			if (!canListplayer) {
				msgNoPerms(sender);
				return true;
			}
			if (args.length < 2) {
				msg(sender, "&7Usage: /%1 %2 <player name> [world] [|regex] [page]", label, args[0]);
				return true;
			}
			String world = null;
			String regex = null;
			String page = "1";
			// label ---- 0 ----- 1 ----- 2 ------- 3 ------ 4
			/// ls listplayer <player> [world] [|<regex>] [page]

			int arg2Type = 0, arg3Type = 0, arg4Type = 0;
			// 0 = nothing, 1 = world, 2 = page number, 3 = regex

			if (args.length > 2) {
				if (args[2].startsWith("|")) {
					arg2Type = 3;
				} else if (args[2].matches("[0-9]+")) {
					arg2Type = 2;
				} else {
					arg2Type = 1;
					world = args[2];
				}
				if (args.length > 3) {
					// regex, page
					if (arg2Type == 1) {
						// world... ?
						if (args[3].startsWith("|") && arg2Type != 3) {
							arg3Type = 3;
						} else if (arg2Type != 2 && args[3].matches("[0-9]+")) {
							arg3Type = 2;
						}
					} else if (arg2Type != 2 && arg2Type == 3) {
						if (args[3].matches("[0-9]+")) {
							arg3Type = 2;
						}
					}
					if (arg3Type == 3 && args.length > 4) {
						if (args[4].matches("[0-9]+")) {
							arg4Type = 2;
						}
					}
				}
			}
			if (arg2Type == 1) {
				world = args[2];
			} else if (arg2Type == 2) {
				page = args[2];
			} else if (arg2Type == 3) {
				regex = args[2].substring(1);
			}
			if (arg3Type == 1) {
				world = args[3];
			} else if (arg3Type == 2) {
				page = args[3];
			} else if (arg3Type == 3) {
				regex = args[3].substring(1);
			}
			if (arg4Type == 1) {
				world = args[4];
			} else if (arg4Type == 2) {
				page = args[4];
			} else if (arg4Type == 3) {
				regex = args[4].substring(1);
			}

			UUID uuid = PlayerUtils.getUUID(args[1]);
			if (uuid == null) {
				msg(sender, "&7Unable to find player: &e%1", args[1]);
				return true;
			}
			List<ChestLock> locks = LockStockPlugin.lsp.getLockDatabase().getLocksByPlayer(uuid, world);
			PageBuilder b = ChatFormat.getPageBuilder();
			for (ChestLock l : locks) {
				String location = (world == null) ? l.getLocation().getWorld().getName() + ":" : "";
				location = ChatFormat.format("%1%2,%3,%4", location, l.getLocation().getBlockX(), l.getLocation().getBlockY(),
						l.getLocation().getBlockZ());
				String msg = ChatFormat.format("&7(&e%3&7)&d %1 &e%2", l.getType(), ChatFormat.getFriendlyMaterialName(l.getMaterial()),
						location);
				boolean match = true;
				if (regex != null)
					if (regex.startsWith("$")) {
						try {
							match = ChatColor.stripColor(msg).matches(regex.substring(1));
						} catch (PatternSyntaxException e) {
							msg(sender, "&cInvalid regex: %1", e.getMessage().replaceAll("\r", "").replace("\\", "\\\\"));
							return true;
						}
					} else {
						match = ChatColor.stripColor(msg).contains(regex);
					}
				if (match)
					b.addPlainMessage(msg);

			}
			String title = ChatFormat.format("&6Found %1 locks by &7%2&6 " + ((world != null) ? "in world &7%3" : ""), b.getLines(),
					args[1], world);
			String cmd = ChatFormat.format("%1 %2 %3", label, args[0], args[1]);
			if (world != null)
				cmd = ChatFormat.format("%1 %2", cmd, world);
			if (regex != null)
				cmd = ChatFormat.format("%1 %2", cmd, regex);

			b.send(sender, title, cmd, page, true);
			return true;
		}
		msg(sender, "&cUnable to handle command %1", args[0]);

		return true;
	}

	private void msg(CommandSender s, String message, Object... objects) {
		s.sendMessage(ChatFormat.format(message, objects));
	}

	private void msgNoPerms(CommandSender s) {
		msg(s, "&cYou do not have permission to do this.");
	}
}
