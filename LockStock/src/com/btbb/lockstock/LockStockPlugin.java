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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.Metrics;

import com.btbb.lockstock.cache.CacheManager;
import com.btbb.lockstock.cmd.AdminCommands;
import com.btbb.lockstock.cmd.ChestLockCommands;
import com.btbb.lockstock.db.Database;
import com.btbb.lockstock.db.LockDatabase;
import com.btbb.lockstock.db.MySQL;
import com.btbb.lockstock.db.SQLite;
import com.btbb.lockstock.garbage.GarbageCollector;
import com.btbb.lockstock.listener.ChestProtectionListener;
import com.btbb.lockstock.listener.LockOperationListener;
import com.btbb.lockstock.permissions.Group;
import com.btbb.lockstock.permissions.GroupPermissionsUtil;
import com.btbb.lockstock.permissions.LockPermissions;
import com.griefcraft.integration.IPermissions;
import com.griefcraft.integration.permissions.BukkitPermissions;
import com.griefcraft.integration.permissions.PEXPermissions;
import com.griefcraft.integration.permissions.SuperPermsPermissions;
import com.griefcraft.integration.permissions.VaultPermissions;
import com.griefcraft.integration.permissions.bPermissions;

public class LockStockPlugin extends JavaPlugin {

	private FileConfiguration config;

	/**
	 * The instance of this class
	 */
	public static LockStockPlugin lsp;
	private boolean useMysql;
	private LockOperationListener creator;
	private GarbageCollector garbage;
	private long collectionInterval;
	private CacheManager cache;
	private boolean autoprotect_warn;

	public boolean isAutoprotect_warn() {
		return autoprotect_warn;
	}

	/**
	 * Create a default configuration file from the .jar.
	 * 
	 * @param name
	 */
	public void setupConfig() {
		getDataFolder().mkdir();
		File configFile = new File(getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			InputStream in = null;
			OutputStream out = null;
			try {
				in = getClass().getResourceAsStream("/config.yml");
				out = new BufferedOutputStream(new FileOutputStream(configFile));
				int n;
				while ((n = in.read()) != -1) {
					out.write(n);
				}
			} catch (IOException e) {
				getLogger().log(Level.WARNING, "[LockStock]Cannot write config.yml", e);
				e.printStackTrace();
			} finally {
				try {
					in.close();
				} catch (IOException e) {
				}
				try {
					out.close();
				} catch (IOException e) {
				}
			}
		}
		this.config = getConfig();
		List<String> list = config.getStringList("blocks");
		LockAPI.loadLockableMaterials(list);
		list = config.getStringList("autoprotect");
		LockAPI.loadAutoprotectMaterials(list);
		list = config.getStringList("worlds-disabled");
		LockAPI.loadWorldsDisabled(list);
		autoprotect_warn = config.getBoolean("autoprotect-warn", false);
		LockPermissions.getPermissions().setOpeatorIsAdmin(config.getBoolean("op-is-admin", true));
	}

	IPermissions permissions;

	public IPermissions getPermissions() {
		return permissions;
	}

	private void setupPermissions() {
		// Thanks Hidendra
		permissions = new SuperPermsPermissions();

		if (getServer().getPluginManager().getPlugin("Vault") != null) {
			permissions = new VaultPermissions();
		} else if (getServer().getPluginManager().getPlugin("PermissionsBukkit") != null) {
			permissions = new BukkitPermissions();
		} else if (getServer().getPluginManager().getPlugin("PermissionsEx") != null) {
			permissions = new PEXPermissions();
		} else if (getServer().getPluginManager().getPlugin("bPermissions") != null) {
			permissions = new bPermissions();
		}
	}

	private void setupGroupPerms() {
		// set up group perms like max blocks allowed

		for (String group : config.getConfigurationSection("groups").getKeys(false)) {
			ConfigurationSection configSec = config.getConfigurationSection("groups." + group);
			if (configSec == null)
				return;
			Group g = new Group(group);
			for (Entry<String, Object> entry : configSec.getValues(false).entrySet()) {
				String block = entry.getKey();
				String value = entry.getValue().toString();
				int val;
				if (value.equalsIgnoreCase("unlimited")) {
					val = Integer.MAX_VALUE;
				} else {
					val = Integer.parseInt(value);
				}
				if (block.equalsIgnoreCase("default"))
					g.setDefault(val);
				else
					try {
						Material m = LockAPI.getMaterial(block);
						if (m != null)
							g.add(m, val);
					} catch (Throwable e) {
						log("Unknown block for " + group + "." + block + ": " + block);
						continue;
					}
			}
			GroupPermissionsUtil.getUtils().addGroup(g);
		}
	}

	protected LockDatabase ldb;

	@Override
	public void onEnable() {
		LockStockPlugin.lsp = this;
		setupConfig();
		try {
			Metrics metrics = new Metrics(this);
			metrics.start();
		} catch (IOException e) {
		}
		if (!initDB())// plugin has failed to load
			return;
		setupPermissions();
		setupGroupPerms();
		LockAPI.init();
		ChestLockCommands clc = new ChestLockCommands(this);
		// register events
		getServer().getPluginManager().registerEvents(new ChestProtectionListener(this), this);
		getServer().getPluginManager().registerEvents(clc, this);
		creator = new LockOperationListener();
		getServer().getPluginManager().registerEvents(creator, this);
		// register commands
		getServer().getPluginCommand("lockstock").setExecutor(clc);
		getServer().getPluginCommand("lock").setExecutor(clc);
		getServer().getPluginCommand("unlock").setExecutor(clc);
		AdminCommands admin = new AdminCommands();
		getServer().getPluginCommand("lockstockadmin").setExecutor(admin);
		setupGC();
		cache = new CacheManager();
		garbage.addAccumulator(cache);
	}

	@Override
	public void onDisable() {
		if (ldb != null && ldb.getDatabase() != null)
			ldb.getDatabase().closeConnection();
	}

	private void setupGC() {
		collectionInterval = getConfig().getInt("garbage-collection", 10) * 1200;
		garbage = new GarbageCollector();
		Bukkit.getScheduler().runTaskTimerAsynchronously(this, garbage, collectionInterval, collectionInterval);
	}

	private boolean initDB() {

		useMysql = getConfig().getBoolean("mysql", false);
		if (useMysql) {
			try {
				Database db = new MySQL(this,
						getConfig().getString("mysql-hostname"),
						getConfig().getString("mysql-port"),
						getConfig().getString("mysql-database"),
						getConfig().getString("mysql-user"),
						getConfig().getString("mysql-password"));
				ldb = LockDatabase.initialize(db, getConfig().getString("mysql-table", "lockstock"), true);
			} catch (Exception exc) {
				useMysql = false;
				getLogger().log(Level.WARNING, ChatColor.DARK_RED + "[LockStock]Failed to initialize MySQL. Using SQLite", exc);
			}
		}
		if (!useMysql) {
			Database db = new SQLite(this, "locks.db");
			try {
				ldb = LockDatabase.initialize(db, "lockstock", false);
			} catch (Exception e) {
				getLogger().log(Level.SEVERE, ChatColor.DARK_RED + "[LockStock]Cannot initialize SQLite! Disabling LockStock", e);
				getServer().getPluginManager().disablePlugin(this);
				return false;
			}
		}
		return true;
	}

	public boolean useMysql() {
		return useMysql;
	}

	public static void log(String str) {
		Bukkit.getLogger().info(ChatColor.YELLOW + "[LockStock]" + ChatColor.DARK_AQUA + str);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		sender.sendMessage("[LockStock]Unhandled Command: " + label);
		return false;
	}

	public LockDatabase getLockDatabase() {
		return ldb;
	}

	public LockOperationListener getLockOperationListener() {
		return creator;
	}

	/**
	 * 
	 * @return Garbage collection interval in milliseconds
	 */
	public long getGarbageCollectionIntervalMillis() {
		return collectionInterval * 50;
	}

	public CacheManager getCache() {
		return cache;
	}
}
