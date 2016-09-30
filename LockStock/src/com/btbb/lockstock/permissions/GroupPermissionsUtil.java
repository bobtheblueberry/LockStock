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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.btbb.lockstock.LockStockPlugin;

public class GroupPermissionsUtil {

	ArrayList<Group> groups;

	private GroupPermissionsUtil() {
		groups = new ArrayList<Group>();
	}

	private static GroupPermissionsUtil instance;

	public static GroupPermissionsUtil getUtils() {
		return (instance == null) ? instance = new GroupPermissionsUtil() : instance;
	}

	/**
	 * Returns a list of materials that have a setting
	 * 
	 * @param p
	 * @param m
	 * @return
	 */
	public List<Material> getBlocksWithSetting(Player p) {
		ArrayList<Material> list = new ArrayList<Material>();
		for (String playerGroup : LockStockPlugin.lsp.getPermissions().getGroups(p))
			for (Group g : groups) {
				if (!g.getName().equalsIgnoreCase(playerGroup))
					continue;
				for (Entry<Material, Integer> entry : g.mappings.entrySet()) {
					if (!list.contains(entry.getKey()))
						list.add(entry.getKey());
				}
			}
		return list;
	}

	public int getMaxBlocks(Player p, Material m) {
		boolean set = false;
		int max = -1;
		int default_num = 0;
		for (String playerGroup : LockStockPlugin.lsp.getPermissions().getGroups(p))
			for (Group g : groups) {
				if (!g.getName().equalsIgnoreCase(playerGroup))
					continue;

				if (g.hasDefaultValue()) {
					default_num = Math.max(default_num, g.getDefaultValue());
					set = true;
				}

				Integer i = g.getFor(m);
				if (i != null) {
					max = Math.max(max, i.intValue());
					set = true;
				}

			}
		if (max >= 0 && set)
			return max;
		else if (set)
			return default_num;
		return Integer.MAX_VALUE;
	}

	public int getMaxDefaulteBlocks(Player p) {
		int default_num = 0;
		boolean set = false;
		for (String playerGroup : LockStockPlugin.lsp.getPermissions().getGroups(p))
			for (Group g : groups) {
				if (!g.getName().equalsIgnoreCase(playerGroup))
					continue;
				if (g.hasDefaultValue()) {
					default_num = Math.max(default_num, g.getDefaultValue());
					set = true;
				}

			}
		if (set)
			return default_num;
		return Integer.MAX_VALUE;
	}

	public Map<Material, Integer> getMaxBlocks(Player p) {
		Map<Material, Integer> map = new HashMap<Material, Integer>();
		for (String playerGroup : LockStockPlugin.lsp.getPermissions().getGroups(p))
			for (Group g : groups) {
				if (!g.getName().equalsIgnoreCase(playerGroup))
					continue;
				for (Entry<Material, Integer> entry : g.mappings.entrySet()) {
					Integer i = map.get(entry.getKey());
					if (i != null)
						map.put(entry.getKey(), Math.max(i, entry.getValue()));
					else
						map.put(entry.getKey(), entry.getValue());
				}

			}
		return map;
	}

	public void addGroup(Group g) {
		groups.add(g);
	}

}
