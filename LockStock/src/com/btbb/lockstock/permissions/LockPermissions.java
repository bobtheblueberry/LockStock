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
package com.btbb.lockstock.permissions;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.btbb.lockstock.ChestLock;
import com.btbb.lockstock.LockAPI;
import com.btbb.lockstock.ChestLock.LockType;
import com.btbb.lockstock.meta.MetaPerm;

public class LockPermissions {

	private LockPermissions() {
	}

	private boolean opeatorIsAdmin;

	private static LockPermissions perms;

	public static LockPermissions getPermissions() {
		if (perms == null)
			perms = new LockPermissions();
		return perms;
	}

	public boolean canPlayerInfo(Player player, ChestLock l) {
		if (getOpPerms(player) || l.getUUID().equals(player.getUniqueId()))
			return true;
		MetaPerm perm = l.getMetaPerm();
		if (perm != null && perm.canPlayerModify(player))
			return true;
		return player.hasPermission("lockstock.info.others");
	}

	public boolean canPlayerModify(Player player, ChestLock m) {
		boolean cond = getOpPerms(player) || m.getUUID().equals(player.getUniqueId());
		if (cond)
			return true;
		if (player.hasPermission("lockstock.admin.modify"))
			return true;
		MetaPerm perm = m.getMetaPerm();
		if (perm != null)
			return perm.canPlayerModify(player);
		return false;
	}

	public boolean canPlayerDelete(Player player, ChestLock m) {
		boolean cond = getOpPerms(player) || m.getUUID().equals(player.getUniqueId());
		if (cond)
			return true;
		if (player.hasPermission("lockstock.admin.delete"))
			return true;
		MetaPerm perm = m.getMetaPerm();
		if (perm != null)
			return perm.canPlayerDestory(player);
		return false;
	}

	public boolean canPlayerOpen(Player player, ChestLock m) {
		if (m == null || player == null)
			return false;
		if (getOpPerms(player))
			return true;
		if (!player.hasPermission("lockstock.use"))
			return false;
		if (player.hasPermission("lockstock.admin.open"))
			return true;
		if (m.getType() == LockType.PUBLIC)
			return true;
		if (m.getUUID().equals(player.getUniqueId()))
			return true;
		MetaPerm perm = m.getMetaPerm();
		if (perm != null && perm.canPlayerAccess(player))
			return true;
		if (m.getType() == LockType.PASSWORD && LockAPI.isChestPasswordUnlocked(m, player))
			return true;
		return false;
	}

	/**
	 * Returns true if the player isn't allowed to add any more of the given
	 * material type
	 * 
	 * @param player the {@link Player}
	 * @param type the {@link Material}
	 * @return false if it's to add more locks
	 */
	public boolean isAboveLimitFor(Player player, Material type) {
		// The two classes come together...
		int cur;
		int max = GroupPermissionsUtil.getUtils().getMaxBlocks(player, type);
		List<Material> special = GroupPermissionsUtil.getUtils().getBlocksWithSetting(player);
		if (!special.contains(type)) {
			cur = LockAPI.getLockCount(player).getTotal(special);
		} else {
			cur = LockAPI.getLockCount(player).get(type);
		}
		return cur >= max;
	}

	public boolean canPlayerUnlock(Player player) {
		return getOpPerms(player) || player.hasPermission("lockstock.use");
	}

	public boolean canPlayerLock(Player player) {
		return getOpPerms(player) || player.hasPermission("lockstock.lock");
	}

	public boolean canPlayerUsePlugin(Player player) {
		return getOpPerms(player) || player.hasPermission("lockstock.use");
	}

	public boolean canPlayerPasswordUnlock(Player player, ChestLock cl) {
		return getOpPerms(player) || player.hasPermission("lockstock.use");
	}

	public boolean canAdminClearplayer(Player player) {
		return getOpPerms(player) || player.hasPermission("lockstock.admin.clearplayer");
	}

	public boolean canChangeOwner(Player player) {
		return getOpPerms(player) || player.hasPermission("lockstock.admin.changeowner");
	}

	public boolean canAdminUpdatename(Player player) {
		return getOpPerms(player) || player.hasPermission("lockstock.admin.updatename");
	}

	public boolean canAdminImportFromSqlite(Player player) {
		return getOpPerms(player) || player.hasPermission("lockstock.admin.import");
	}

	public boolean canAdminListplayer(Player player) {
		return getOpPerms(player) || player.hasPermission("lockstock.admin.listplayer");
	}

	public boolean canPlayerPersistentMode(Player player) {
		return getOpPerms(player) || player.hasPermission("lockstock.use");
	}

	public boolean getOpPerms(Player p) {
		return opeatorIsAdmin && p.isOp();
	}

	public void setOpeatorIsAdmin(boolean opeatorIsAdmin) {
		this.opeatorIsAdmin = opeatorIsAdmin;
	}

}
