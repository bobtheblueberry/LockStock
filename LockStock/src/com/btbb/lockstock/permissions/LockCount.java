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

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;

import org.bukkit.Material;

/**
 * Stores info on how many locks a player has
 * 
 * @author Serge
 *
 */
public class LockCount {

	HashMap<Material, IntWrapper> map;
	UUID player;

	public LockCount(UUID player) {
		this.player = player;
		map = new HashMap<Material, IntWrapper>();
	}

	public int get(Material m) {
		IntWrapper i = map.get(m);
		return (i != null) ? i.get() : 0;
	}

	public int getTotal() {
		int i = 0;
		for (IntWrapper iw : map.values())
			i += iw.get();
		return i;
	}

	/**
	 * Gets total blocks excluding certain materials
	 * 
	 * @param exclude list of materials to exclude
	 * @return
	 */
	public int getTotal(List<Material> exclude) {
		int i = 0;
		for (Entry<Material, IntWrapper> entry : map.entrySet())
			if (!exclude.contains(entry.getKey()))
				i += entry.getValue().get();
		return i;
	}

	public void set(Material m, int numb) {
		if (numb == 0)
			map.remove(m);
		else
			map.put(m, new IntWrapper(numb));
	}

	public void add(Material m) {
		if (map.containsKey(m))
			map.get(m).inc();
		else
			map.put(m, new IntWrapper(1));
	}

	public void remove(Material m) {
		IntWrapper i = map.get(m);
		if (i == null)
			return;
		if (i.get() == 1)
			map.remove(i);
		else
			i.dec();
	}

	private static class IntWrapper {
		int i;

		public IntWrapper(int i) {
			this.i = i;
		}

		public int get() {
			return i;
		}

		public void inc() {
			i++;
		}

		public void dec() {
			i--;
		}
	}
}
