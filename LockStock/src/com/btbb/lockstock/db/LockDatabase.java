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
package com.btbb.lockstock.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import com.btbb.lockstock.ChestLock;
import com.mysql.jdbc.Statement;

public class LockDatabase {

	private Database db;
	private String table;

	private LockDatabase(Database db, String table) {
		this.db = db;
		this.table = table;
	}

	public static LockDatabase initialize(Database db, String tablename, boolean mySql) throws SQLException {
		Connection c = null;
		try {
			c = db.getConnection();
			if (!c.getMetaData().getTables(null, null, tablename, null).next()) {
				if (mySql) {
					PreparedStatement ps = c.prepareStatement("CREATE TABLE `" + tablename + "` (\n"
							+ "`id` int(11) NOT NULL AUTO_INCREMENT,\n"
							+ "`uuid` varchar(36) ,\n"
							+ "`name` varchar(36) ,\n"
							+ "`type` int(11) ,\n"
							+ "`x` int(11) ,\n"
							+ "`y` int(11) ,\n"
							+ "`z` int(11) ,\n"
							+ "`data` text , \n"
							+ "`blockid` int(11),\n"
							+ "`world` varchar(255),\n"
							+ "`password` varchar(255) DEFAULT NULL,\n"
							+ "`created` datetime DEFAULT CURRENT_TIMESTAMP,\n"
							+ " PRIMARY KEY (`id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8;");
					ps.execute();
				} else {
					c.prepareStatement("PRAGMA encoding = \"UTF-8\"").execute();
					PreparedStatement ps = c.prepareStatement(
							"CREATE TABLE IF NOT EXISTS `" + tablename + "` (\n"
									+ "`id` int(11) PRIMARY KEY,\n"
									+ "`uuid` varchar(36) ,\n"
									+ "`name` varchar(36) ,\n"
									+ "`type` int(11) ,\n"
									+ "`x` int(11) ,\n"
									+ "`y` int(11) ,\n"
									+ "`z` int(11) ,\n"
									+ "`data` text , \n"
									+ "`blockid` int(11),\n"
									+ "`world` varchar(255),\n"
									+ "`password` varchar(255) DEFAULT NULL,\n"
									+ "`created` datetime DEFAULT CURRENT_TIMESTAMP)");
					ps.execute();
				}
			}
		} catch (SQLException exc) {
			throw exc;
		}
		return new LockDatabase(db, tablename);
	}

	public ChestLock getLock(Location l) {
		ChestLock lock = null;
		Connection c = null;
		try {
			c = db.getConnection();
			PreparedStatement ps = c.prepareStatement("SELECT * FROM `" + table + "` WHERE x = ? AND y = ? AND z = ? AND world = ?");
			ps.setInt(1, l.getBlockX());
			ps.setInt(2, l.getBlockY());
			ps.setInt(3, l.getBlockZ());
			ps.setString(4, l.getWorld().getName());
			ResultSet res = ps.executeQuery();
			if (res.next())
				lock = getChestLock(res);
			res.close();
		} catch (SQLException exc) {
			Bukkit.getLogger().log(Level.SEVERE, "[LockStock]Could not check protection at " + l, exc);
		}
		return lock;
	}

	public List<ChestLock> getLocksInRange(String world, int x, int z, int x2, int z2) {
		LinkedList<ChestLock> list = new LinkedList<ChestLock>();
		Connection c = null;
		try {
			c = db.getConnection();
			PreparedStatement ps = c.prepareStatement(
					"SELECT * FROM `" + table + "` WHERE x >= ? AND x < ? AND z >= ? AND z < ? AND world = ?");
			ps.setInt(1, x);
			ps.setInt(2, x2);
			ps.setInt(3, z);
			ps.setInt(4, z2);
			ps.setString(5, world);
			ResultSet res = ps.executeQuery();
			while (res.next())
				list.add(getChestLock(res));
			res.close();
		} catch (SQLException exc) {
			Bukkit.getLogger().log(Level.SEVERE, "[LockStock]Could not get protections", exc);
		}
		return list;
	}

	public List<ChestLock> getLocksByPlayer(UUID player, String world) {
		LinkedList<ChestLock> list = new LinkedList<ChestLock>();
		Connection c = null;
		try {
			c = db.getConnection();
			PreparedStatement ps = c.prepareStatement(
					"SELECT * FROM `" + table + "` WHERE uuid = ?" + ((world != null) ? " AND world = ?" : ""));
			ps.setString(1, player.toString());
			if (world != null)
			ps.setString(2, world);
			ResultSet res = ps.executeQuery();
			while (res.next())
				list.add(getChestLock(res));
			res.close();
		} catch (SQLException exc) {
			Bukkit.getLogger().log(Level.SEVERE, "[LockStock]Could not get protections", exc);
		}
		return list;
	}

	public void addLock(ChestLock lock, boolean genNewId) {
		Connection c = null;
		try {
			c = db.getConnection();
			PreparedStatement ps = c.prepareStatement("INSERT INTO `" + table
					+ "` (`uuid`, `name`, `type`, `x`, `y`, `z`, `blockId`, `world`, `password`, `created`, `data`"
					+ ((genNewId) ? "" : ", `id`")
					+ ")"
					+ " VALUES(?,?,?,?,?,?,?,?,?,?,?" + ((genNewId) ? "" : ",?") + ")", Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, lock.getUUID().toString());
			ps.setString(2, lock.getName());
			ps.setInt(3, lock.getType().ordinal());
			ps.setInt(4, lock.getLocation().getBlockX());
			ps.setInt(5, lock.getLocation().getBlockY());
			ps.setInt(6, lock.getLocation().getBlockZ());
			ps.setInt(7, lock.getBlockId());
			ps.setString(8, lock.getLocation().getWorld().getName());
			ps.setString(9, lock.getPassword());
			ps.setTimestamp(10, new Timestamp(lock.getCreated().getTime()));
			ps.setString(11, lock.getMetaData());
			if (!genNewId)
				ps.setInt(12, lock.getId());
			ps.executeUpdate();
			if (genNewId) {
				ResultSet rs = ps.getGeneratedKeys();
				if (rs.next())
					lock.setId(rs.getInt(1));
				rs.close();
			}
		} catch (SQLException exc) {
			Bukkit.getLogger().log(Level.SEVERE, "[LockStock]Could not add protection at " + lock.getLocation(), exc);
		}
	}

	public void updateLock(ChestLock lock) {
		Connection c = null;
		try {
			c = db.getConnection();
			PreparedStatement ps = c.prepareStatement("UPDATE `" + table
					+ "` SET `uuid` = ?, `name` = ?, `type` = ?, `x` = ?, `y` = ?, `z` = ?,"
					+ " `blockId` = ?, `world` = ?, `password` = ?, `created` = ?, `data` = ?"
					+ " WHERE `id` = ?");
			ps.setString(1, lock.getUUID().toString());
			ps.setString(2, lock.getName());
			ps.setInt(3, lock.getType().ordinal());
			ps.setInt(4, lock.getLocation().getBlockX());
			ps.setInt(5, lock.getLocation().getBlockY());
			ps.setInt(6, lock.getLocation().getBlockZ());
			ps.setInt(7, lock.getBlockId());
			ps.setString(8, lock.getLocation().getWorld().getName());
			ps.setString(9, lock.getPassword());
			ps.setTimestamp(10, new Timestamp(lock.getCreated().getTime()));
			ps.setString(11, lock.getMetaData());
			ps.setInt(12, lock.getId());
			ps.executeUpdate();
		} catch (SQLException exc) {
			Bukkit.getLogger().log(Level.SEVERE, "[LockStock]Could not update protection at " + lock.getLocation(), exc);
		}
	}

	public void removeLock(ChestLock lock) {
		Connection c = null;
		try {
			c = db.getConnection();
			PreparedStatement ps = c.prepareStatement("DELETE FROM `" + table
					+ "`WHERE `x` = ? AND `y` = ? AND `z` = ? AND `world` = ?");
			ps.setInt(1, lock.getLocation().getBlockX());
			ps.setInt(2, lock.getLocation().getBlockY());
			ps.setInt(3, lock.getLocation().getBlockZ());
			ps.setString(4, lock.getLocation().getWorld().getName());
			ps.execute();
		} catch (SQLException exc) {
			Bukkit.getLogger().log(Level.SEVERE, "[LockStock]Could not remove protection at " + lock.getLocation(), exc);
		}
	}

	public int removeLocksByPlayer(UUID uuid, String worldname) {
		Connection c = null;
		int i = 0;
		try {
			c = db.getConnection();
			PreparedStatement ps = c.prepareStatement("DELETE FROM `" + table
					+ "`WHERE `uuid` = ?" + ((worldname != null) ? " AND `world` = ?" : ""));
			ps.setString(1, uuid.toString());
			if (worldname != null)
				ps.setString(2, worldname);
			i = ps.executeUpdate();
		} catch (SQLException exc) {
			Bukkit.getLogger().log(Level.SEVERE, "[LockStock]Could clear player " + uuid, exc);
		}
		return i;
	}

	public Map<Material, Integer> getLockCount(UUID id) {
		Connection c = null;
		try {
			c = db.getConnection();
			PreparedStatement ps = c.prepareStatement("SELECT `blockid` FROM `" + table
					+ "` WHERE `uuid` = ?");
			ps.setString(1, id.toString());
			ResultSet s = ps.executeQuery();
			Map<Material, Integer> map = new HashMap<Material, Integer>();
			while (s.next()) {
				@SuppressWarnings("deprecation")
				Material m = Material.getMaterial(s.getInt(1));
				if (m == null)
					continue;
				Integer i = map.get(m);
				if (i == null)
					map.put(m, 1);
				else
					map.put(m, i + 1);
			}
			s.close();
			return map;
		} catch (SQLException exc) {
			Bukkit.getLogger().log(Level.SEVERE, "[LockStock]Could look for protections for " + id, exc);
		}
		return null;
	}

	public int updatePlayername(UUID id, String newname) {
		Connection c = null;
		int num = 0;
		try {
			c = db.getConnection();
			PreparedStatement ps = c.prepareStatement("UPDATE `" + table
					+ "` SET `name` = ? WHERE `uuid` = ?");
			ps.setString(1, newname);
			ps.setString(2, id.toString());
			num = ps.executeUpdate();
		} catch (SQLException exc) {
			Bukkit.getLogger().log(Level.SEVERE, "[LockStock]Could update playername for " + id, exc);
		}
		return num;
	}

	protected static ChestLock getChestLock(ResultSet res) throws SQLException {
		Location l = new Location(Bukkit.getWorld(res.getString("world")), res.getInt("x"), res.getInt("y"), res.getInt("z"));
		UUID uuid = UUID.fromString(res.getString("uuid"));
		return new ChestLock(
				uuid,
				res.getString("name"),
				ChestLock.LockType.values()[res.getInt("type")],
				l,
				res.getInt("id"),
				res.getInt("blockid"),
				res.getTimestamp("created"),
				res.getString("password"),
				res.getString("data"));
	}

	public Database getDatabase() {
		return db;
	}
}
