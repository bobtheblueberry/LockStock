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

import org.bukkit.plugin.Plugin;

/**
 * Abstract Database class, serves as a base for any connection method (MySQL, SQLite, etc.)
 * 
 */
public abstract class Database {

    /**
     * Plugin instance, use for plugin.getDataFolder() and plugin.getLogger()
     */
    protected Plugin plugin;

    /**
     * Creates a new Database
     * 
     * @param plugin
     *            Plugin instance
     */
    protected Database(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens a connection with the database, or returns the existing one if it is open
     * 
     * @return Connection opened
     */
    public abstract Connection getConnection();
    
    public abstract void closeConnection();
}