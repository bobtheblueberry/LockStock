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

import static com.btbb.lockstock.cache.ChunkCache.getRange;

import java.util.LinkedList;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import com.btbb.lockstock.ChestLock;
import com.btbb.lockstock.LockStockPlugin;
import com.btbb.lockstock.garbage.GarbageAccumulator;

/**
 * 
 * Manages storing of chest locks in ram
 * 
 * @author Serge
 *
 */
public class CacheManager implements GarbageAccumulator {

	private LinkedList<ChunkCache> caches;

	public static enum UpdateOperation {
		ADD, REMOVE
	}

	public static enum PlayerUpdateOperation {
		CLEARPLAYER, UPDATE_PLAYERNAME
	}

	public CacheManager() {
		caches = new LinkedList<ChunkCache>();
	}

	/**
	 * 
	 * @param op must be {@link UpdateOperation#ADD} or
	 *            {@link UpdateOperation#REMOVE}
	 * @param l
	 */
	public void updateCache(UpdateOperation op, ChestLock l) {
		ChunkCache cache = null;
		for (ChunkCache cc : caches)
			if (cc.contains(l.getLocation())) {
				cache = cc;
				break;
			}
		if (cache == null)
			return;
		if (op == UpdateOperation.ADD)
			cache.addLock(l);
		else if (op == UpdateOperation.REMOVE)
			cache.remove(l);
	}

	/**
	 * Removes player's entries from cache
	 * 
	 * @param op must be {@link PlayerUpdateOperation#CLEARPLAYER}
	 * @param player the player
	 */
	public void updateCache(PlayerUpdateOperation op, UUID player) {
		if (!op.equals(PlayerUpdateOperation.CLEARPLAYER))
			return;
		for (ChunkCache cc : caches)
			cc.removePlayer(player);
	}

	/**
	 * Updates player name
	 * 
	 * @param op must be {@link PlayerUpdateOperation#UPDATE_PLAYERNAME}
	 * @param player the player
	 * @param name new player name
	 */
	public void updateCache(PlayerUpdateOperation op, UUID player, String name) {
		if (!op.equals(PlayerUpdateOperation.UPDATE_PLAYERNAME))
			return;
		for (ChunkCache cc : caches)
			cc.updatePlayername(player, name);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void collect() {
		for (ChunkCache e : (LinkedList<ChunkCache>) caches.clone())
			if (e.lastAccessed() + LockStockPlugin.lsp.getGarbageCollectionIntervalMillis() < System.currentTimeMillis())
				caches.remove(e);
	}

	public ChestLock getLock(Location loc) {
		return getLock(loc.getBlock());
	}

	public ChestLock getLock(Block b) {
		Location loc = b.getLocation();
		ChestLock l = getChunk(loc).getLock(loc);
		if (l != null)
			return l;

		// search for neighboring double chests
		if (b == null || b.getType() == Material.AIR)
			return l;
		Material m;
		if (b.getType() == Material.CHEST)
			m = b.getType();
		else if (b.getType() == Material.TRAPPED_CHEST)
			m = b.getType();
		else
			return l;
		Location loc2 = loc.clone().add(1.0, 0, 0);
		l = getChunk(loc2).getLock(loc2);
		if (testfor(l, m))
			return l;
		loc2 = loc.clone().add(-1.0, 0, 0);
		l = getChunk(loc2).getLock(loc2);
		if (testfor(l, m))
			return l;
		loc2 = loc.clone().add(0, 0, 1.0);
		l = getChunk(loc2).getLock(loc2);
		if (testfor(l, m))
			return l;
		loc2 = loc.clone().add(0, 0, -1.0);
		l = getChunk(loc2).getLock(loc2);
		if (testfor(l, m))
			return l;

		return l;
	}

	private boolean testfor(ChestLock l, Material m) {
		return l != null && l.getMaterial().equals(m);
	}

	private ChunkCache getChunk(Location l) {
		for (ChunkCache cc : caches)
			if (cc.contains(l))
				return cc;
		double ccx, ccz;
		ccx = Math.floor(((double) l.getBlockX()) / getRange());
		ccz = Math.floor(((double) l.getBlockZ()) / getRange());
		ChunkCache cc = new ChunkCache(l.getWorld(), (int) ccx, (int) ccz);
		caches.add(cc);
		int x1, z1;
		x1 = cc.getX() * getRange();
		z1 = cc.getZ() * getRange();
		for (ChestLock cl : LockStockPlugin.lsp.getLockDatabase().getLocksInRange(l.getWorld().getName(), x1, z1, x1 + getRange(),
				z1 + getRange()))
			cc.addLock(cl);
		return cc;
	}

	/**
	 * Do not use this method, it is very savage
	 */
	public void resetCache() {
		caches = new LinkedList<ChunkCache>();
	}
}
