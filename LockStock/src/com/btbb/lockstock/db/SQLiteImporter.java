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
import java.util.logging.Level;

import org.bukkit.Bukkit;

import com.btbb.lockstock.LockAPI;
import com.btbb.lockstock.LockStockPlugin;

public class SQLiteImporter {

	public static int importFromSQLite() {

		try {
			Database db = new SQLite(LockStockPlugin.lsp, "locks.db");
			Connection c = db.getConnection();
			c = db.getConnection();
			PreparedStatement ps = c.prepareStatement("SELECT * FROM `lockstock`");

			int ind = 0;
			ResultSet res = ps.executeQuery();
			while (res.next()) {
				LockAPI.addLock(LockDatabase.getChestLock(res), null);
				ind++;
			}
			LockAPI.resetCache();

			res.close();
			c.close();
			return ind;
		} catch (SQLException exc) {
			Bukkit.getLogger().log(Level.WARNING, "Failed to import from SQLite", exc);
		}
		return -1;
	}
}
