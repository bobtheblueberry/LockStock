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
package com.btbb.lockstock.cache;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;

import com.btbb.lockstock.ChestLock;

/**
 * Stores cached chest lock data by chunk These are not the same size as
 * minecraft chunks.
 * 
 * @author Serge
 *
 */
public class ChunkCache {

	private long lastAccessed;
	private int x, z;
	/**
	 * since these aren't real chunks, we can put in whatever range we want to
	 */
	private static final int range = 32;
	private World world;

	public World getWorld() {
		return world;
	}

	public int getX() {
		return x;
	}

	public int getZ() {
		return z;
	}

	protected ChunkCache(World world, int x, int z) {
		lastAccessed = System.currentTimeMillis();
		locks = new LinkedList<ChestLock>();
		this.world = world;
		this.x = x;
		this.z = z;
	}

	public static int getRange() {
		return range;
	}

	/**
	 * Returns true if the given location is in this chunk
	 * 
	 * @param l
	 * @return
	 */
	public boolean contains(Location l) {
		int rx1 = x * range;
		int rz1 = z * range;
		int bx = l.getBlockX(), bz = l.getBlockZ();
		return l.getWorld().equals(world) && bx >= rx1 && bx < rx1 + range && bz >= rz1 && bz < rz1 + range;
	}

	LinkedList<ChestLock> locks;

	protected long lastAccessed() {
		return lastAccessed;
	}

	protected ChestLock getLock(Location l) {
		lastAccessed = System.currentTimeMillis();
		for (ChestLock cl : locks)
			if (cl.getLocation().equals(l))
				return cl;
		return null;
	}

	protected void addLock(ChestLock lock) {
		locks.add(lock);
	}

	protected void update(ChestLock l) {
		locks.remove(l);
		locks.add(l);
	}

	protected void remove(ChestLock l) {
		locks.remove(l);
	}

	/**
	 * Removes all locks by player
	 * 
	 * @param player
	 *            {@link UUID} of player
	 */
	public void removePlayer(UUID player) {
		for (ChestLock l : new ArrayList<ChestLock>(locks))
			if (l.getUUID().equals(player))
				locks.remove(l);
	}

	public void updatePlayername(UUID player, String name) {
		for (ChestLock l : locks)
			if (l.getUUID().equals(player))
				l.setName(name);
	}
}
