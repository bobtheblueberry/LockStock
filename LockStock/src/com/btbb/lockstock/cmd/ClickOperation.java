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
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.btbb.bukkit.PlayerUtils;
import com.btbb.lockstock.ChatFormat;
import com.btbb.lockstock.ChestLock;
import com.btbb.lockstock.ChestLock.LockType;
import com.btbb.lockstock.meta.MetaPerm.PermLevel;
import com.btbb.lockstock.permissions.LockPermissions;
import com.btbb.lockstock.LockAPI;

public class ClickOperation {
	public enum Mode {
		CPRIVATE, CPUBLIC, CPASSWORD, DELETE, CHPASS, INFO, ADDP, DELP, MODP, PASSWORD_UNLOCK, CHOWN
	}

	protected Mode mode;
	Player player;
	UUID uuid;
	PermLevel level;
	String nameforUUID, password;

	public ClickOperation(Mode m, Player player) {
		this.mode = m;
		this.player = player;
	}

	public ClickOperation(Mode m, Player player, UUID uuid, PermLevel level, String name) {
		this.mode = m;
		this.player = player;
		this.uuid = uuid;
		this.level = level;
		this.nameforUUID = name;
	}

	public ClickOperation(Mode m, Player player, UUID uuid, String name) {
		this.mode = m;
		this.player = player;
		this.uuid = uuid;
		this.nameforUUID = name;
	}

	public ClickOperation(Mode m, Player player, String password) {
		this.mode = m;
		this.player = player;
		this.password = password;
	}

	public void execute(Block block) {
		// Do an info lookup for any block in case it the protection is
		// corrupted
		if (mode == Mode.INFO) {
			info(block);
			return;
		}
		if (!LockAPI.isBlockLockable(block.getType())) {
			msg("&cThat type of block is not enabled");
			return;
		}
		if (mode == Mode.CPRIVATE) {
			lock(block, LockType.PRIVATE);
			return;
		}
		if (mode == Mode.CPUBLIC) {
			lock(block, LockType.PUBLIC);
			return;
		}
		if (mode == Mode.CPASSWORD) {
			lock(block, LockType.PASSWORD);
			return;
		}

		if (mode == Mode.DELETE) {
			delete(block);
			return;
		}
		if (mode == Mode.ADDP) {
			addp(block);
			return;
		}
		if (mode == Mode.DELP) {
			delp(block);
			return;
		}
		if (mode == Mode.PASSWORD_UNLOCK) {
			password(block);
			return;
		}
		if (mode == Mode.CHPASS) {
			chpass(block);
			return;
		}
		if (mode == Mode.CHOWN) {
			chown(block);
			return;
		}
		msg("&aNot implemented");
	}

	private void lock(Block b, LockType type) {
		if (lockExists(b.getLocation())) {
			msg("&cThat %1 is already locked!", ChatFormat.getFriendlyMaterialName(b.getType()));
			return;
		}
		if (LockPermissions.getPermissions().isAboveLimitFor(player, b.getType())) {
			msg("&cYou have reached your limit for locked blocks.");
			return;
		}
		ChestLock cl = new ChestLock(player, type, b, (type.equals(LockType.PASSWORD)) ? LockAPI.getSHA(password) : null);
		LockAPI.addLock(cl, player);
		if (type.equals(LockType.PASSWORD)) {
			msg("&6Encryption on %1 (password=&6%2&6)", ChatFormat.getFriendlyMaterialName(cl.getMaterial()), password);
		} else if (type.equals(LockType.PUBLIC))
			msg("&6Public lock created on %1", ChatFormat.getFriendlyMaterialName(cl.getMaterial()));
		else
			msg("&6Private lock created on %1", ChatFormat.getFriendlyMaterialName(cl.getMaterial()));
	}

	private void info(Block b) {
		ChestLock cl = getLock(b.getLocation());
		if (cl == null) {
			if (!LockAPI.isBlockLockable(b.getType())) {
				msg("&cThat type of block is not enabled");
				return;
			}
			msg("&cThat %1 is not locked.", ChatFormat.getFriendlyMaterialName(b.getType()));
			return;
		}
		if (!LockPermissions.getPermissions().canPlayerInfo(player, cl)) {
			msgNoPerms();
			return;
		}
		msg("&e%3 lock on %2 by %1", cl.getName(), ChatFormat.getFriendlyMaterialName(cl.getMaterial()), cl.getType());
		if (cl.getMetaPerm() != null) {
			msg("&eAccess permissions:");
			for (Entry<UUID, PermLevel> entry : cl.getMetaPerm().getPerms()) {
				String name = PlayerUtils.getPlayerName(entry.getKey());
				msg("&6%1 (Level=%2)", name, entry.getValue());
			}
		}
	}

	private void delete(Block b) {
		ChestLock cl = getLock(b.getLocation());
		if (cl == null) {
			msg("&cThat %1 is not locked.", ChatFormat.getFriendlyMaterialName(b.getType()));
			return;
		}
		if (!LockPermissions.getPermissions().canPlayerDelete(player, cl)) {
			msgNoPerms();
			return;
		}
		LockAPI.removeLock(cl);
		if (cl.getUUID().equals(player.getUniqueId()))
			msg("&cUnlocked %1", ChatFormat.getFriendlyMaterialName(cl.getMaterial()));
		else
			msg("&cUnlocked %2 by %1", cl.getName(), ChatFormat.getFriendlyMaterialName(cl.getMaterial()));

	}

	private void addp(Block b) {
		ChestLock cl = getLock(b.getLocation());
		if (cl == null) {
			msg("&cThat %1 is not locked.", ChatFormat.getFriendlyMaterialName(b.getType()));
			return;
		}
		if (!LockPermissions.getPermissions().canPlayerModify(player, cl)) {
			msgNoPerms();
			return;
		}
		if (uuid.equals(cl.getUUID())) {
			msg("&cYou cannot add yourself");
			return;
		}
		if (cl.getMetaPerm() == null)
			cl.addMetaPerm();
		cl.getMetaPerm().setPerm(uuid, level);
		LockAPI.updateLock(cl);
		msg("&cAdded %1 to lock (Level=%2)", nameforUUID, level);
	}

	private void delp(Block b) {
		ChestLock cl = getLock(b.getLocation());
		if (cl == null) {
			msg("&cThat %1 is not locked.", ChatFormat.getFriendlyMaterialName(b.getType()));
			return;
		}
		if (!LockPermissions.getPermissions().canPlayerModify(player, cl)) {
			msgNoPerms();
			return;
		}
		if (cl.getMetaPerm() == null)
			return;
		cl.getMetaPerm().remove(uuid);
		LockAPI.updateLock(cl);
		msg("&cRemoved %1 from lock", nameforUUID);
	}
	
	private void chown(Block b) {
		ChestLock cl = getLock(b.getLocation());
		if (cl == null) {
			msg("&cThat %1 is not locked.", ChatFormat.getFriendlyMaterialName(b.getType()));
			return;
		}
		if (!LockPermissions.getPermissions().canPlayerModify(player, cl)) {
			msgNoPerms();
			return;
		}
		cl.setOwner(uuid);
		cl.setName(nameforUUID);
		LockAPI.updateLock(cl);
		msg("&cChanged owner to &7%1", nameforUUID);
	}
	/**
	 * unlock with password
	 * 
	 * @param b
	 */
	private void password(Block b) {
		ChestLock cl = getLock(b.getLocation());
		if (cl == null) {
			msg("&cThat %1 is not locked.", ChatFormat.getFriendlyMaterialName(b.getType()));
			return;
		}
		if (!LockPermissions.getPermissions().canPlayerPasswordUnlock(player, cl)) {
			msgNoPerms();
			return;
		}
		if (!cl.getType().equals(LockType.PASSWORD)) {
			msg("&cIncorrect lock type");
			return;
		}
		if (LockAPI.passwordUnlock(cl, player, password))
			msg("&eUnlocked successfully");
		else
			msg("&cIncorrect password: %1", password);
	}

	private void chpass(Block b) {
		ChestLock cl = getLock(b.getLocation());
		if (cl == null) {
			msg("&cThat %1 is not locked.", ChatFormat.getFriendlyMaterialName(b.getType()));
			return;
		}
		if (!LockPermissions.getPermissions().canPlayerModify(player, cl)) {
			msgNoPerms();
			return;
		}
		if (!cl.getType().equals(LockType.PASSWORD)) {
			msg("&cIncorrect lock type");
			return;
		}
		cl.setPassword(LockAPI.getSHA(password));
		LockAPI.updateLock(cl);
		msg("&ePassword updated to &7%1", password);
	}

	private boolean lockExists(Location l) {
		return getLock(l) != null;
	}

	private ChestLock getLock(Location l) {
		return LockAPI.getLock(l);
	}

	private void msg(String msg, Object... objects) {
		player.sendMessage(ChatFormat.format(msg, objects));
	}

	private void msgNoPerms() {
		msg("&cYou do not have permission to do this.");
	}
}
