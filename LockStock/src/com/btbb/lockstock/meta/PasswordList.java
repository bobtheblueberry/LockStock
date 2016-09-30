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
package com.btbb.lockstock.meta;

import java.util.HashMap;

import org.bukkit.entity.Player;

import com.btbb.lockstock.ChestLock;

public class PasswordList {
	private Player player;
	private HashMap<Integer, String> passwords;

	public PasswordList(Player p) {
		this.player = p;
		passwords = new HashMap<Integer, String>();
	}

	public Player getPlayer() {
		return player;
	}

	public boolean matchPassword(ChestLock lock) {
		String pass = passwords.get(lock.getId());
		if (pass != null)
			return pass.equals(lock.getPassword());
		return false;
	}

	public void put(int id, String password) {
		passwords.put(id, password);
	}

	public void remove(int id) {
		passwords.remove(id);
	}
}
