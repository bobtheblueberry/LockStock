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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

import org.bukkit.plugin.Plugin;

/**
 * Connects to and uses a SQLite database
 * 
 * @author tips48
 */
public class SQLite extends Database {
	private final String dbLocation;

	private Connection connection;

	/**
	 * Creates a new SQLite instance
	 * 
	 * @param plugin
	 *            Plugin instance
	 * @param dbLocation
	 *            Location of the Database (Must end in .db)
	 */
	public SQLite(Plugin plugin, String dbLocation) {
		super(plugin);
		this.dbLocation = dbLocation;
		this.connection = null;
	}

	@Override
	public Connection getConnection() {
		File file = new File(plugin.getDataFolder(), dbLocation);
		if (!(file.exists())) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				plugin.getLogger().log(Level.SEVERE, "Unable to create database!");
			}
		}
		try {
			if (connection != null && !connection.isClosed())
				return connection;
			Class.forName("org.sqlite.JDBC");
			connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + File.separator + dbLocation);
		} catch (SQLException e) {
			plugin.getLogger().log(Level.SEVERE, "Could not connect to SQLite server! because: " + e.getMessage());
		} catch (ClassNotFoundException e) {
			plugin.getLogger().log(Level.SEVERE, "JDBC Driver not found!");
		}
		return connection;
	}

	@Override
	public void closeConnection() {
		try {
			connection.close();
		} catch (SQLException e) {
		}
		connection = null;
	}
}
