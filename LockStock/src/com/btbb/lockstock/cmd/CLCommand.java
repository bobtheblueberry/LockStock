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

import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.btbb.bukkit.PlayerUtils;
import com.btbb.lockstock.ChatFormat;
import com.btbb.lockstock.ChatFormat.PageBuilder;
import com.btbb.lockstock.LockAPI;
import com.btbb.lockstock.LockStockPlugin;
import com.btbb.lockstock.PlayerSettings.AutoLockMode;
import com.btbb.lockstock.PlayerSettings.InteractMode;
import com.btbb.lockstock.meta.MetaPerm;
import com.btbb.lockstock.permissions.GroupPermissionsUtil;
import com.btbb.lockstock.permissions.LockPermissions;

public class CLCommand {

	Player player = null;
	CommandSender sender;
	String label;

	public CLCommand(String label, CommandSender s) {
		this.sender = s;
		this.label = label;
		if (s instanceof Player)
			this.player = (Player) s;

	}

	public void execute(String[] args) {
		if (args.length < 1) {
			return;
		}
		if (player == null) {
			msg("&cThis operation may only be completed by a player");
			return;
		}
		if (!LockPermissions.getPermissions().canPlayerUsePlugin(player)) {
			msg("&6You do not have permission to use LockStock");
			return;
		}

		if (args[0].equalsIgnoreCase("help") || args[0].equals("?")) {
			help(args);
			return;
		}

		if (args[0].equalsIgnoreCase("lock") || args[0].equalsIgnoreCase("private")) {
			lock(ClickOperation.Mode.CPRIVATE);
			return;
		}
		if (args[0].equalsIgnoreCase("public")) {
			lock(ClickOperation.Mode.CPUBLIC);
			return;
		}
		if (args[0].toLowerCase().startsWith("encrypt")) {
			cpassword(args);
			return;
		}

		if (args[0].equalsIgnoreCase("unlock")) {
			delete();
			return;
		}

		if (args[0].equalsIgnoreCase("info")) {
			info();
			return;
		}
		if (args[0].equalsIgnoreCase("add")) {
			add(args);
			return;
		}
		if (args[0].equalsIgnoreCase("remove")) {
			remove(args);
			return;
		}
		if (args[0].equalsIgnoreCase("decrypt")) {
			passwordUnlock(args);
			return;
		}
		if (args[0].equalsIgnoreCase("recrypt")) {
			chpass(args);
			return;
		}
		if (args[0].equalsIgnoreCase("autolock")) {
			autolock(args);
			return;
		}
		if (args[0].equalsIgnoreCase("limits")) {
			limits();
			return;
		}
		if (args[0].equalsIgnoreCase("mode")) {
			mode(args);
			return;
		}
		if (args[0].toLowerCase().startsWith("esc")) {
			escape();
			return;
		}
		if (args[0].equalsIgnoreCase("changeowner")) {
			changeowner(args);
			return;
		}
		msg("&cUnknown command: &f%1", args[0]);
		if (args[0].equalsIgnoreCase("lock"))
			msg("&7Did you mean &fcreate&7?");
	}

	public void execute(String command) {
		if (command == null)
			return;
		String[] args = command.split("\\u0020+");
		execute(args);
	}

	private void help(String[] args) {
		PageBuilder b = ChatFormat.getPageBuilder();
		LockPermissions p = LockPermissions.getPermissions();

		if (LockPermissions.getPermissions().canPlayerLock(player)) {
			b.addFormattedMessage("&2/%1 lock: &fCreates a private lock", label);
			b.addFormattedMessage("&2/%1 private: &fCreates a private lock", label);
		}
		if (LockPermissions.getPermissions().canPlayerUnlock(player))
			b.addFormattedMessage("&a/%1 unlock: &fUnlocks chest", label);
		if (LockPermissions.getPermissions().canPlayerLock(player)) {
			b.addFormattedMessage("&b/%1 public: &fCreates a public lock", label);
			b.addFormattedMessage("&2/%1 encrypt: &fEncrypts chest with password", label);
			b.addFormattedMessage("&a/%1 recrypt: &fRe-encrypts chest with lock", label);
		}
		if (LockPermissions.getPermissions().canPlayerUnlock(player))
			b.addFormattedMessage("&a/%1 decrypt <pass>: &fDecrypts a chest", label);
		b.addFormattedMessage("&e/%1 info: &fLooks up lock info", label);
		b.addFormattedMessage("&b/%1 add <player> <access|modify|full>: &fAdds players to lock", label);
		b.addFormattedMessage("&b/%1 remove: &fRemoves players from lock", label);
		b.addFormattedMessage("&e/%1 limits: &fDisplay your block limits", label);
		b.addFormattedMessage("&7/%1 autolock <on|off>: &fEnables/disabled auto-locking", label);
		b.addFormattedMessage("&7/%1 mode <single|persistent>: &fSets interact mode", label);
		b.addFormattedMessage("&7/%1 escape: &fEscapes persistent mode", label);
		if (p.canChangeOwner(player)) {
			b.addFormattedMessage("&9/%1 changeowner <player>: &fChanges protection owner", label);
		}
		b.send(sender, ChatFormat.format("&cLockStock help"), label + " " + args[0], ((args.length > 1) ? args[1] : null), true);
	}

	private void lock(ClickOperation.Mode mode) {
		if (!LockPermissions.getPermissions().canPlayerLock(player)) {
			msgNoPerms();
			return;
		}
		msg("&aTap the block you wish to lock");
		LockStockPlugin.lsp.getLockOperationListener().addOperation(player, new ClickOperation(mode, player));
	}

	private void cpassword(String[] args) {
		if (!LockPermissions.getPermissions().canPlayerLock(player)) {
			msgNoPerms();
			return;
		}
		if (args.length < 2) {
			msg("&c/%1 %2 <password>", label, args[0]);
			return;
		}

		msg("&aTap the block you wish to lock");
		LockStockPlugin.lsp.getLockOperationListener().addOperation(player,
				new ClickOperation(ClickOperation.Mode.CPASSWORD, player, args[1]));
	}

	private void delete() {
		if (!LockPermissions.getPermissions().canPlayerUnlock(player)) {
			msgNoPerms();
			return;
		}
		msg("&aTap the block you wish to unlock");
		LockStockPlugin.lsp.getLockOperationListener().addOperation(player, new ClickOperation(ClickOperation.Mode.DELETE, player));
	}

	private void info() {
		if (!LockPermissions.getPermissions().canPlayerUsePlugin(player)) {
			msgNoPerms();
			return;
		}
		msg("&aTap the block investigate");
		LockStockPlugin.lsp.getLockOperationListener().addOperation(player, new ClickOperation(ClickOperation.Mode.INFO, player));
	}

	/**
	 * add a player to the lock
	 * 
	 * @param args
	 */
	private void add(String[] args) {
		if (!LockPermissions.getPermissions().canPlayerUsePlugin(player)) {
			msgNoPerms();
			return;
		}
		if (args.length < 3) {
			msg("&c/%1 %2 <playername> <access|modify|full>", label, args[0]);
			return;
		}
		UUID other = PlayerUtils.getUUID(args[1]);
		if (other == null) {
			msg("&cNo such player: &e%1", args[1]);
			return;
		}
		MetaPerm.PermLevel level = null;
		if (args[2].equalsIgnoreCase("access"))
			level = MetaPerm.PermLevel.ACCESS_ONLY;
		else if (args[2].equalsIgnoreCase("modify"))
			level = MetaPerm.PermLevel.ALLOW_MODIFICATION;
		else if (args[2].equalsIgnoreCase("full"))
			level = MetaPerm.PermLevel.FULL_RIGHTS;
		else if (args[2].equalsIgnoreCase("none"))
			level = MetaPerm.PermLevel.NO_RIGHTS;
		else {
			msg("&cInvalid permission level: " + args[2]);
			return;
		}

		msg("&aTap the block you wish to modify");
		LockStockPlugin.lsp.getLockOperationListener().addOperation(player,
				new ClickOperation(ClickOperation.Mode.ADDP, player, other, level, args[1]));
	}

	/**
	 * remove a player for the lock
	 * 
	 * @param args
	 */
	private void remove(String[] args) {
		if (!LockPermissions.getPermissions().canPlayerUsePlugin(player)) {
			msgNoPerms();
			return;
		}
		if (args.length < 2) {
			msg("&c/%1 %2 <playername>", label, args[0]);
			return;
		}
		UUID other = PlayerUtils.getUUID(args[1]);
		if (other == null) {
			msg("&cNo such player: &e%1", args[1]);
			return;
		}

		msg("&aTap the block you wish to modify");
		LockStockPlugin.lsp.getLockOperationListener().addOperation(player,
				new ClickOperation(ClickOperation.Mode.DELP, player, other, args[1]));
	}

	private void passwordUnlock(String[] args) {
		if (args.length < 2) {
			msg("&c/%1 %2 <password>", label, args[0]);
			return;
		}

		msg("&aTap the block you wish unlock");
		LockStockPlugin.lsp.getLockOperationListener().addOperation(player,
				new ClickOperation(ClickOperation.Mode.PASSWORD_UNLOCK, player, args[1]));
	}

	private void chpass(String[] args) {
		if (!LockPermissions.getPermissions().canPlayerUsePlugin(player)) {
			msgNoPerms();
			return;
		}
		if (args.length < 2) {
			msg("&c/%1 %2 <password>", label, args[0]);
			return;
		}

		msg("&aTap the block modify");
		LockStockPlugin.lsp.getLockOperationListener().addOperation(player,
				new ClickOperation(ClickOperation.Mode.CHPASS, player, args[1]));
	}

	private void autolock(String[] args) {
		if (!LockPermissions.getPermissions().canPlayerUsePlugin(player)) {
			msgNoPerms();
			return;
		}
		if (args.length > 1 && args[1].equalsIgnoreCase("on")) {
			LockAPI.getPlayerSettings(player).setAutoLock(AutoLockMode.ALWAYS_LOCK);
			msg("&7Auto-locking enabled.");
			return;
		} else if (args.length > 1 && args[1].equalsIgnoreCase("off")) {
			LockAPI.getPlayerSettings(player).setAutoLock(AutoLockMode.NEVER);
			msg("&7Auto-locking disabled.");
			return;
		} else if (args.length > 1 && args[1].equalsIgnoreCase("default")) {
			LockAPI.getPlayerSettings(player).setAutoLock(AutoLockMode.DEFAULT);
			msg("&7Auto-locking enabled for server-enabled auto-lock blocks.");
			return;
		}
		msg("&c/%1 %2 <on|off|default>", label, args[0]);
		return;
	}

	private void mode(String[] args) {
		if (!LockPermissions.getPermissions().canPlayerUsePlugin(player)) {
			msgNoPerms();
			return;
		}
		if (args.length > 1 && args[1].equalsIgnoreCase("single")) {
			LockAPI.getPlayerSettings(player).setInteractMode(InteractMode.SINGLE);
			msg("&7Interact mode is single command mode.");
			return;
		} else if (args.length > 1 && args[1].equalsIgnoreCase("persistent")) {
			LockAPI.getPlayerSettings(player).setInteractMode(InteractMode.PERSISTENT);
			msg("&7Interact mode is persistent command mode.");
			msg("&6/%1 escape &7to escape persistent mode.", label);
			return;
		}
		msg("&c/%1 %2 <single|persistent>", label, args[0]);
		return;
	}

	private void escape() {
		LockStockPlugin.lsp.getLockOperationListener().remove(player);
		msg("&7Escaped persistent mode.");
		return;
	}

	private void limits() {
		if (!LockPermissions.getPermissions().canPlayerUsePlugin(player)) {
			msgNoPerms();
			return;
		}
		int total = LockAPI.getLockCount(player).getTotal();
		Set<Entry<Material, Integer>> set = GroupPermissionsUtil.getUtils().getMaxBlocks(player).entrySet();
		msg("&6Total Locked Blocks: &f%1", total);
		msg("&6Maximum Locks%1: &f%2",
				(set.isEmpty()) ? "" : " (Excluding those listed below)",
				getULstr(GroupPermissionsUtil.getUtils().getMaxDefaulteBlocks(player)));

		for (Entry<Material, Integer> entry : set)
			msg("&6Maximum %1: &f%2", ChatFormat.getFriendlyMaterialName(entry.getKey()), getULstr(entry.getValue()));

		return;
	}

	private String getULstr(Integer n) {
		if (n.intValue() == Integer.MAX_VALUE)
			return "Unlimited";
		return n.toString();
	}

	private void changeowner(String[] args) {
		if (!LockPermissions.getPermissions().canChangeOwner(player)) {
			msgNoPerms();
			return;
		}
		if (args.length < 2) {
			msg("&c/%1 %2 <playername>", label, args[0]);
			return;
		}
		UUID other = PlayerUtils.getUUID(args[1]);
		if (other == null) {
			msg("&cNo such player: &e%1", args[1]);
			return;
		}

		msg("&aTap the block you wish to modify");
		LockStockPlugin.lsp.getLockOperationListener().addOperation(player,
				new ClickOperation(ClickOperation.Mode.CHOWN, player, other, args[1]));
	}

	private void msg(String message, Object... objects) {
		sender.sendMessage(ChatFormat.format(message, objects));
	}

	private void msgNoPerms() {
		msg("&cYou do not have permission to do this.");
	}
}
