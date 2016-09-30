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
package com.btbb.lockstock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.btbb.lockstock.ChestLock.LockType;
import com.btbb.lockstock.cache.CacheManager;
import com.btbb.lockstock.cache.CacheManager.PlayerUpdateOperation;
import com.btbb.lockstock.cache.CacheManager.UpdateOperation;
import com.btbb.lockstock.meta.PasswordList;
import com.btbb.lockstock.permissions.LockCount;
import com.google.common.hash.Hashing;

public class LockAPI {

	private static HashMap<UUID, PlayerSettings> playerSettings;
	private static ArrayList<Material> supportedMaterials;
	private static ArrayList<Material> autolockMaterials;
	private static HashMap<World, Boolean> worldsDisabled;

	private static boolean enableAnvil;

	public static void init() {
		playerSettings = new HashMap<UUID, PlayerSettings>();
	}

	protected static void loadLockableMaterials(List<String> materials) {
		if (materials == null) {
			supportedMaterials = new ArrayList<Material>();
			return;
		}
		supportedMaterials = new ArrayList<Material>(materials.size());
		for (String s : materials) {
			Material m;
			try {
				m = getMaterial(s);
			} catch (Exception e) {
				m = null;
			}
			if (m == null)
				LockStockPlugin.log("Unknown Block: " + s);
			else
				supportedMaterials.add(m);
		}
		enableAnvil = supportedMaterials.contains(Material.ANVIL);
	}

	protected static void loadAutoprotectMaterials(List<String> materials) {
		if (materials == null) {
			autolockMaterials = new ArrayList<Material>();
			return;
		}
		autolockMaterials = new ArrayList<Material>(materials.size());
		for (String s : materials) {
			Material m;
			try {
				m = getMaterial(s);
			} catch (Exception e) {
				m = null;
			}
			if (m == null)
				LockStockPlugin.log("Unknown Block: " + s);
			else
				autolockMaterials.add(m);
		}
	}

	protected static void loadWorldsDisabled(List<String> worlds) {
		worldsDisabled = new HashMap<World, Boolean>();
		if (worlds == null)
			return;
		for (String s : worlds) {
			World w = Bukkit.getWorld(s);
			if (w == null)
				LockStockPlugin.log("No such world: " + s);
			else {
				worldsDisabled.put(w, true);
				LockStockPlugin.log("World " + s + " is disabled.");
			}
		}
	}

	public static PlayerSettings getPlayerSettings(Player p) {
		return getPlayerSettings(p.getUniqueId());
	}

	public static PlayerSettings getPlayerSettings(UUID p) {
		PlayerSettings ps = playerSettings.get(p);
		if (ps == null) {
			ps = new PlayerSettings(p);
			playerSettings.put(p, ps);
		}
		return ps;
	}

	public static LockCount getLockCount(Player player) {
		PlayerSettings ps = getPlayerSettings(player);
		LockCount c = ps.getLockCount();
		if (c == null)
			ps.setLockCount(c = getLockCountFromDb(player));

		return c;
	}

	private static LockCount getLockCountFromDb(Player player) {
		UUID uuid = player.getUniqueId();
		Map<Material, Integer> map = LockStockPlugin.lsp.getLockDatabase().getLockCount(uuid);
		LockCount c = new LockCount(uuid);
		for (Entry<Material, Integer> entry : map.entrySet()) {
			c.set(entry.getKey(), entry.getValue());
		}
		return c;
	}

	public static boolean isWorldDisabled(World w) {
		Boolean b = worldsDisabled.get(w);
		if (b != null && b.booleanValue())
			return true;
		return false;
	}

	public static boolean allowTransfer(ChestLock src, ChestLock dest) {
		return src.getType() == LockType.PUBLIC || (dest != null && src.getUUID().equals(dest.getUUID()));
	}

	/**
	 * Shortcut method to save precious CPU time
	 * 
	 * @return true if anvils can be protected
	 */
	public static boolean anvilsEnabled() {
		return enableAnvil;
	}

	public static ChestLock getLock(Block b) {
		try {
			return LockStockPlugin.lsp.getCache().getLock(b);
		} catch (Exception exc) {
			return null;
		}
	}

	public static ChestLock getLock(Location l) {
		try {
			return LockStockPlugin.lsp.getCache().getLock(l);
		} catch (Exception exc) {
			return null;
		}

	}

	public static String getSHA(String s) {
		return Hashing.sha1().hashString(s, java.nio.charset.Charset.forName("UTF-8")).toString();
	}

	public static boolean isBlockLockable(Material m) {
		if (m == Material.BURNING_FURNACE)
			return isBlockLockable(Material.FURNACE);

		return supportedMaterials.contains(m);
	}

	public static boolean isBlockAutoLockable(Material m) {
		if (m == Material.BURNING_FURNACE)
			return isBlockAutoLockable(Material.FURNACE);

		return autolockMaterials.contains(m);
	}

	public static boolean isChestPasswordUnlocked(ChestLock l, Player player) {
		PlayerSettings ps = playerSettings.get(player.getUniqueId());
		if (ps == null || ps.getPasswordList() == null)
			return false;
		PasswordList pl = ps.getPasswordList();
		return pl.matchPassword(l);
	}

	@SuppressWarnings("deprecation")
	public static Material getMaterial(String s) throws Exception {
		if (s == null)
			return null;
		if (s.matches("[0-9]+"))
			return Material.getMaterial(Integer.parseInt(s));
		s = s.toUpperCase();
		if (s.startsWith("MINECRAFT:"))
			s = s.substring("minecraft:".length());
		Material m = Material.getMaterial(s);
		if (m == null)
			// try this..
			m = Material.getMaterial(s.split(" ")[0]);
		return m;
	}

	/**
	 * Returns true if the password was correct
	 * 
	 * @param cl Lock to unlock with password
	 * @param player
	 * @param password
	 * @return
	 */
	public static boolean passwordUnlock(ChestLock cl, Player player, String password) {
		if (cl.getPassword() == null || cl.getPassword().isEmpty())
			return false;
		String sha1 = getSHA(password);
		if (!sha1.equals(cl.getPassword()))
			return false;
		PlayerSettings ps = getPlayerSettings(player);
		PasswordList pl = ps.getPasswordList();
		if (pl == null) {
			ps.setPasswordList(pl = new PasswordList(player));
		}
		pl.put(cl.getId(), sha1);

		return true;
	}

	public static void addLock(ChestLock cl, Player player) {
		LockStockPlugin.lsp.getLockDatabase().addLock(cl, true);
		LockStockPlugin.lsp.getCache().updateCache(UpdateOperation.ADD, cl);
		// increase lock count
		if (player == null)
			return;
		LockCount c = getLockCount(player);
		c.add(cl.getMaterial());
	}

	public static void removeLock(ChestLock cl) {
		LockStockPlugin.lsp.getLockDatabase().removeLock(cl);
		LockStockPlugin.lsp.getCache().updateCache(UpdateOperation.REMOVE, cl);

		// remove password entries
		for (Entry<UUID, PlayerSettings> entry : playerSettings.entrySet()) {
			PasswordList l = entry.getValue().getPasswordList();
			if (l != null)
				l.remove(cl.getId());
		}

		// remove from lock count
		PlayerSettings ps = playerSettings.get(cl.getUUID());
		if (ps != null) {
			LockCount c = ps.getLockCount();
			if (c != null)
				c.remove(cl.getMaterial());
		}
	}

	private static boolean smartMove(ChestLock l, Location newLocation) {
		ChestLock nl = l.clone(newLocation);
		LockStockPlugin.lsp.getLockDatabase().removeLock(l);
		LockStockPlugin.lsp.getLockDatabase().addLock(nl, false);
		LockStockPlugin.lsp.getCache().updateCache(CacheManager.UpdateOperation.REMOVE, l);
		LockStockPlugin.lsp.getCache().updateCache(CacheManager.UpdateOperation.ADD, nl);
		return false;
	}

	/**
	 * Removes a lock on a block, or moves it if it's a double chest.
	 * 
	 * @param m
	 * @return true if the lock was removed, false if the lock was moved to the
	 *         neighboring chest
	 */
	public static boolean smartRemoveLock(ChestLock l) {
		Block b = l.getLocation().getBlock();
		if (b == null || b.getType() == Material.AIR)
			return true;

		Material m = l.getMaterial();
		if (m != Material.CHEST && m != Material.TRAPPED_CHEST) {
			removeLock(l);
			return true;
		}
		Location l2 = l.getLocation().clone().add(1.0, 0, 0);
		if (l2.getBlock().getType().equals(m))
			return smartMove(l, l2);
		l2 = l.getLocation().clone().add(-1.0, 0, 0);
		if (l2.getBlock().getType().equals(m))
			return smartMove(l, l2);
		l2 = l.getLocation().clone().add(0, 0, 1.0);
		if (l2.getBlock().getType().equals(m))
			return smartMove(l, l2);
		l2 = l.getLocation().clone().add(0, 0, -1.0);
		if (l2.getBlock().getType().equals(m))
			return smartMove(l, l2);

		removeLock(l);
		return true;
	}

	/**
	 * Updates the lock by saving it to the database
	 * 
	 * @param l the lock
	 */
	public static void updateLock(ChestLock l) {
		LockStockPlugin.lsp.getLockDatabase().updateLock(l);
		// don't need to update it in the cache
	}

	public static int updatePlayername(UUID uuid, String newname) {
		// update database
		int num = LockStockPlugin.lsp.getLockDatabase().updatePlayername(uuid, newname);
		// update cache
		LockStockPlugin.lsp.getCache().updateCache(CacheManager.PlayerUpdateOperation.UPDATE_PLAYERNAME, uuid, newname);
		return num;
	}

	private LockAPI() {
	}

	/**
	 * Deletes all locks by player
	 * 
	 * @param p
	 * @param world World name, or null for all worlds
	 * @return Number of locks deleted
	 */
	public static int clearPlayer(UUID p, String world) {
		int ind = LockStockPlugin.lsp.getLockDatabase().removeLocksByPlayer(p, world);
		// now update the cache
		LockStockPlugin.lsp.getCache().updateCache(PlayerUpdateOperation.CLEARPLAYER, p);
		// remove from lock count
		PlayerSettings ps = playerSettings.get(p);
		if (ps != null)
			ps.setLockCount(null);

		return ind;
	}

	/**
	 * Big Bad method, only to be used when big bad things happen to the database
	 */
	public static void resetCache() {
		LockStockPlugin.lsp.getCache().resetCache();
		for (PlayerSettings ps : playerSettings.values())
			ps.resetLockCount();
	}
}
