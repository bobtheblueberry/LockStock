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

import java.util.Date;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.btbb.lockstock.meta.MetaPerm;
import com.btbb.lockstock.meta.MetaPerm.PermLevel;

/**
 * Holds information about a protected block
 * 
 * @author Serge
 *
 */
public class ChestLock {

	public static enum LockType {
		PRIVATE("Private"), PASSWORD("Encryption"), PUBLIC("Public");
		private String name;

		private LockType(String name) {
			this.name = name;
		}

		public String toString() {
			return name;
		}
	};

	private UUID owner;
	public void setOwner(UUID owner) {
		this.owner = owner;
	}

	private String name;
	private LockType lockType;
	private int id;
	private Location location;
	private int blockId;
	private Date created;
	private String password;
	/**
	 * Holds info such as other players who can access lock
	 */
	private MetaPerm meta;

	@SuppressWarnings("deprecation")
	public ChestLock(Player owner, LockType type, Block b, String password) {
		this.owner = owner.getUniqueId();
		this.name = owner.getName();
		this.lockType = type;
		this.location = b.getLocation();
		if (b.getType() == Material.BURNING_FURNACE)
			this.blockId = Material.FURNACE.getId();
		else
			this.blockId = b.getTypeId();
		this.created = new Date();
		this.password = password;
	}

	public ChestLock(Player owner, LockType type, Block b) {
		this(owner, type, b, null);
	}

	@SuppressWarnings("deprecation")
	public ChestLock(UUID owner, String name, LockType type, Location loc, int id, int blockId, Date created, String password,
			String data) {
		this.owner = owner;
		this.name = name;
		this.lockType = type;
		this.location = loc;
		this.id = id;
		if (blockId == Material.BURNING_FURNACE.getId())
			blockId = Material.FURNACE.getId();
		this.blockId = blockId;
		this.created = created;
		this.password = password;
		processMetaJson(data);
	}

	@SuppressWarnings("deprecation")
	private ChestLock(UUID owner, String name, LockType type, Location loc, int id, int blockId, Date created, String password,
			MetaPerm meta) {
		this.owner = owner;
		this.name = name;
		this.lockType = type;
		this.location = loc;
		this.id = id;
		if (blockId == Material.BURNING_FURNACE.getId())
			blockId = Material.FURNACE.getId();
		this.blockId = blockId;
		this.created = created;
		this.password = password;
		this.meta = meta;
	}

	/**
	 * Clones a {@link ChestLock} and assigns it with a different location
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public ChestLock clone(Location l) {
		return new ChestLock(owner, name, lockType, l, id, blockId, created, password, meta);
	}

	public UUID getUUID() {
		return owner;
	}

	public LockType getType() {
		return lockType;
	}

	public int getId() {
		return id;
	}

	public Location getLocation() {
		return location;
	}

	public int getBlockId() {
		return blockId;
	}

	public Date getCreated() {
		return created;
	}

	public String getPassword() {
		return password;
	}

	public MetaPerm getMetaPerm() {
		return meta;
	}

	@SuppressWarnings("unchecked")
	public String getMetaData() {
		if (meta == null)
			return null;

		JSONObject jsonData = new JSONObject();
		JSONArray root = new JSONArray();

		Set<Entry<UUID, PermLevel>> set = meta.getPerms();
		for (Entry<UUID, PermLevel> en : set) {
			JSONObject object = new JSONObject();
			object.put("uuid", en.getKey().toString());
			object.put("level", en.getValue().ordinal());
			root.add(object);
		}
		jsonData.put("others", root);
		return jsonData.toJSONString();
	}

	/**
	 * Get's the name of the player who set it.
	 * 
	 * @return The player's name.
	 */
	public String getName() {
		return name;
	}

	public boolean equals(Object o) {
		if (!(o instanceof ChestLock))
			return false;
		ChestLock cl = (ChestLock) o;
		return cl.blockId == this.blockId && cl.owner.equals(this.owner) && cl.location.equals(this.location);
	}

	public String toString() {
		return "ChestLock by " + name + " at " + location;
	}

	@SuppressWarnings("deprecation")
	public Material getMaterial() {
		return Material.getMaterial(blockId);
	}

	/**
	 * Returns false if the block is not the correct Material
	 * 
	 * @return
	 */
	public boolean validate() {
		Block b = location.getBlock();
		Material cm = getMaterial();
		if (cm.equals(Material.FURNACE))
			return b.getType() == Material.FURNACE || b.getType() == Material.BURNING_FURNACE;
		return cm.equals(b.getType());
	}

	public void setId(int int1) {
		this.id = int1;
	}

	private static JSONParser jsonParser = new JSONParser();

	private void processMetaJson(String data) {
		if (data == null || data.trim().isEmpty())
			return;
		Object object = null;
		try {
			object = jsonParser.parse(data);
		} catch (Exception e) {
			return;
		}
		if (!(object instanceof JSONObject))
			return;
		JSONObject base = (JSONObject) object;
		Object others = base.get("others");
		if (others != null && (others instanceof JSONArray)) {
			meta = new MetaPerm(this);
			JSONArray array = (JSONArray) others;
			for (Object node : array) {
				if (!(node instanceof JSONObject))
					continue;
				JSONObject map = (JSONObject) node;
				UUID uuid = UUID.fromString((String) map.get("uuid"));
				PermLevel pl = PermLevel.values()[((Long) map.get("level")).intValue()];
				meta.setPerm(uuid, pl);
			}
		}
	}

	public void addMetaPerm() {
		meta = new MetaPerm(this);
	}

	public void setPassword(String sha) {
		this.password = sha;
	}

	public void setName(String name) {
		this.name = name;
	}

}
