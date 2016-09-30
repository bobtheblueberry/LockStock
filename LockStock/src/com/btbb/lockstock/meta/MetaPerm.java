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
package com.btbb.lockstock.meta;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.btbb.lockstock.ChestLock;

public class MetaPerm {
	public static enum PermLevel {
		NO_RIGHTS("None"), ACCESS_ONLY("Access"), ALLOW_MODIFICATION("Modify"), FULL_RIGHTS("Full");
		private PermLevel(String str) {
			this.name = str;
		}

		String name;

		public String toString() {
			return name;
		}
	};

	public MetaPerm(ChestLock lock) {
		this.lock = lock;
		perms = new HashMap<UUID, PermLevel>();
	}

	public Set<Entry<UUID, PermLevel>> getPerms() {
		return perms.entrySet();
	}

	private ChestLock lock;
	private HashMap<UUID, PermLevel> perms;

	public boolean canPlayerAccess(Player p) {
		if (p.getUniqueId() == lock.getUUID())
			return true;
		PermLevel level = perms.get(p.getUniqueId());
		return level != null && level == PermLevel.ACCESS_ONLY || level == PermLevel.ALLOW_MODIFICATION || level == PermLevel.FULL_RIGHTS;
	}

	public boolean canPlayerModify(Player p) {
		if (p.getUniqueId() == lock.getUUID())
			return true;
		PermLevel level = perms.get(p.getUniqueId());
		return level != null && level == PermLevel.ALLOW_MODIFICATION || level == PermLevel.FULL_RIGHTS;

	}

	public boolean canPlayerDestory(Player p) {
		if (p.getUniqueId() == lock.getUUID())
			return true;
		PermLevel level = perms.get(p.getUniqueId());
		return level != null && level == PermLevel.FULL_RIGHTS;
	}

	public void setPerm(UUID uuid, PermLevel level) {
		perms.put(uuid, level);
	}
	
	public void remove(UUID uuid) {
		perms.remove(uuid);
	}
	
}