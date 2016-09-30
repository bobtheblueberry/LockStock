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
package com.btbb.bukkit;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.btbb.mojang.nameutils.NameFetcher;
import com.btbb.mojang.nameutils.NameTimestampPair;
import com.btbb.mojang.nameutils.UUIDFetcher;

/**
 * Single class for getting bukkit UUIDs and player names
 * 
 * @author Serge
 *
 */
public class PlayerUtils {

	private PlayerUtils() {
	}

	public static UUID getUUID(String name) {
		for (Player plr : Bukkit.getServer().getOnlinePlayers())
			if (plr.getName().equalsIgnoreCase(name))
				return plr.getUniqueId();
		for (OfflinePlayer plr : Bukkit.getServer().getOfflinePlayers())
			if (plr.getName().equalsIgnoreCase(name))
				return plr.getUniqueId();
		try {
			return UUIDFetcher.getUUIDOf(name);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static UUID getMojangUUID(String name) {
		try {
			return UUIDFetcher.getUUIDOf(name);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Gets player name by searching through server players then by mojang
	 * lookup
	 * 
	 * @param id
	 * @return
	 */
	public static String getPlayerName(UUID id) {
		for (Player plr : Bukkit.getServer().getOnlinePlayers())
			if (plr.getUniqueId().equals(id))
				return plr.getName();
		for (OfflinePlayer plr : Bukkit.getServer().getOfflinePlayers())
			if (plr.getUniqueId().equals(id))
				return plr.getName();
		return getMojangPlayerName(id);
	}

	public static String getMojangPlayerName(UUID id) {
		List<NameTimestampPair> list = null;
		try {
			list = new NameFetcher(id).call();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (list == null || list.isEmpty())
			return null;
		return list.get(list.size() - 1).name;
	}

	public static Player getOnlinePlayer(String name) {
		for (Player plr : Bukkit.getServer().getOnlinePlayers())
			if (plr.getName().equalsIgnoreCase(name))
				return plr;
		return null;
	}

	public static OfflinePlayer getOfflinePlayer(String name) {
		for (OfflinePlayer plr : Bukkit.getServer().getOfflinePlayers())
			if (plr.getName().equalsIgnoreCase(name))
				return plr;
		return null;
	}
}
