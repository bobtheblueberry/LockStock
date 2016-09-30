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
package com.btbb.lockstock.listener;

import java.util.LinkedHashMap;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.btbb.lockstock.LockAPI;
import com.btbb.lockstock.PlayerSettings.InteractMode;
import com.btbb.lockstock.cmd.ClickOperation;

/**
 * Handles users clicking block they wish to protect
 * 
 * @author Serge
 *
 */
public class LockOperationListener implements Listener {

	/**
	 * pending operations that have been registered with in game commands
	 */
	private LinkedHashMap<Player, ClickOperation> operations;

	public LockOperationListener() {
		operations = new LinkedHashMap<Player, ClickOperation>();
	}

	public void addOperation(Player p, ClickOperation op) {
		operations.put(p, op);
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		if (!event.hasBlock())
			return;
		Block block = event.getClickedBlock();
		if (LockAPI.isWorldDisabled(block.getWorld()))
			return;
		ClickOperation op = operations.get(event.getPlayer());
		if (op == null)
			return;
		event.setCancelled(true);
		if (LockAPI.getPlayerSettings(event.getPlayer()).getInteractMode() == InteractMode.SINGLE)
			operations.remove(event.getPlayer());
		op.execute(block);
	}

	public boolean isOperating(Player player) {
		for (Player p : operations.keySet())
			if (player.equals(p))
				return true;
		return false;
	}
	
	public boolean remove(Player p) {
		return operations.remove(p) != null;
	}

}
