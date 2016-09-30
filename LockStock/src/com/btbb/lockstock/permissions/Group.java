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
package com.btbb.lockstock.permissions;

import java.util.HashMap;

import org.bukkit.Material;

public class Group {

	/**
	 * default: <num> in yaml
	 */
	protected Integer defaultValue = null;
	protected HashMap<Material, Integer> mappings;
	protected String name;

	public Group(String name) {
		mappings = new HashMap<Material, Integer>();
		this.name = name;
	}
	
	public Integer getFor(Material m) {
		return mappings.get(m);
	}

	public void add(Material m, int val) {
		mappings.put(m, val);
	}

	public void setDefault(int val) {
		defaultValue = val;
	}

	public boolean hasDefaultValue() {
		return defaultValue != null;
	}
	
	public int getDefaultValue() {
		return defaultValue;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return name;
	}
}
