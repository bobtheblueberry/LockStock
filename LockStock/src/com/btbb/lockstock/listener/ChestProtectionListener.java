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

import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import com.btbb.lockstock.ChatFormat;
import com.btbb.lockstock.ChestLock;
import com.btbb.lockstock.ChestLock.LockType;
import com.btbb.lockstock.LockAPI;
import com.btbb.lockstock.LockStockPlugin;
import com.btbb.lockstock.PlayerSettings.AutoLockMode;
import com.btbb.lockstock.permissions.LockPermissions;

public class ChestProtectionListener implements Listener {

	LockStockPlugin plugin;

	public ChestProtectionListener(LockStockPlugin p) {
		this.plugin = p;
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (LockAPI.isWorldDisabled(event.getPlayer().getWorld()))
			return;
		Player player = event.getPlayer();
		if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		Block block = event.getClickedBlock();
		if (!event.hasBlock())
			return;
		if (LockAPI.isBlockLockable(block.getType())) {
			ChestLock m = LockAPI.getLock(event.getClickedBlock());
			if (m == null)
				return;
			if (!LockPermissions.getPermissions().canPlayerOpen(player, m)) {
				event.setCancelled(true);
				if (!plugin.getLockOperationListener().isOperating(player)){
					String msg;
					if (m.getType() == LockType.PASSWORD)
						msg = "&c&oThis %1 is encrypted with a password. See &7/lockstock decrypt";
					else
						msg = "&c&oThis %1 is locked!" ;
				
					player.sendMessage(ChatFormat.format(msg,ChatFormat.getFriendlyMaterialName(block.getType())));
				}
			}

		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (LockAPI.isWorldDisabled(event.getBlock().getWorld()))
			return;
		// Remove invalid protections, autolock blocks
		Block b = event.getBlock();
		if (b == null)
			return;
		if (!LockAPI.isBlockLockable(b.getType()))
			return;
		ChestLock l = LockAPI.getLock(b);
		if (l != null && l.getLocation().equals(b.getLocation())) {
			LockAPI.removeLock(l);
			event.getPlayer().sendMessage(ChatFormat.format("&4[LS]&cRemoved corrupted lock."));
			l = null;
		}
		if (l != null)
			return;
		Player p = event.getPlayer();
		AutoLockMode alm = LockAPI.getPlayerSettings(p).getAutoLockMode();
		if (alm == AutoLockMode.NEVER || (alm == AutoLockMode.DEFAULT && !LockAPI.isBlockAutoLockable(b.getType())))
			return;
		if (LockPermissions.getPermissions().isAboveLimitFor(p, b.getType())) {
			if (plugin.isAutoprotect_warn())
				p.sendMessage(ChatFormat.format("&7Auto-lock: Block limit exceeded, not locking..."));
			return;
		}
		if (!LockPermissions.getPermissions().canPlayerLock(p))
			return;
		LockAPI.addLock(l = new ChestLock(event.getPlayer(), ChestLock.LockType.PRIVATE, b), event.getPlayer());
		p.sendMessage(ChatFormat.format("&6Private lock created on %1", ChatFormat.getFriendlyMaterialName(l.getMaterial())));

	}

	@EventHandler(ignoreCancelled = true)
	public void onPistonExtend(BlockPistonExtendEvent event) {
		if (LockAPI.isWorldDisabled(event.getBlock().getWorld()))
			return;
		if (!LockAPI.anvilsEnabled())
			return;
		if (handleAnvils(event.getBlocks(), true))
			event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPistonRetract(BlockPistonRetractEvent event) {
		if (LockAPI.isWorldDisabled(event.getBlock().getWorld()))
			return;
		if (!LockAPI.anvilsEnabled())
			return;
		if (handleAnvils(event.getBlocks(), true))
			event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (LockAPI.isWorldDisabled(block.getWorld()))
			return;
		ChestLock anvil;
		if (LockAPI.anvilsEnabled() && (anvil = handleAnvils(block)) != null) {
			event.getPlayer()
					.sendMessage(ChatFormat.format("&cAnvil at (%1,%2,%3) is fall protected",
							anvil.getLocation().getBlockX(),
							anvil.getLocation().getBlockY(), anvil.getLocation().getBlockZ()));
			event.setCancelled(true);
		}
		if (!LockAPI.isBlockLockable(block.getType()))
			return;
		ChestLock m = LockAPI.getLock(block);
		if (m == null)
			return;
		if (!LockPermissions.getPermissions().canPlayerDelete(event.getPlayer(), m)) {
			if (LockPermissions.getPermissions().canPlayerInfo(event.getPlayer(), m))
				event.getPlayer().sendMessage(ChatFormat.format("&c[LS]: This %2 is locked by &7%1", m.getName(),
						ChatFormat.getFriendlyMaterialName(m.getBlockId())));
			else
				event.getPlayer()
						.sendMessage(ChatFormat.format("&c[LS]: This %2 is locked", ChatFormat.getFriendlyMaterialName(m.getBlockId())));
			event.setCancelled(true);
		}
		// removing handled by onBlockBreakMonitor
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onBlockBreakMonitor(BlockBreakEvent event) {
		if (LockAPI.isWorldDisabled(event.getBlock().getWorld()))
			return;
		Block block = event.getBlock();
		if (block == null)
			return;

		if (!LockAPI.isBlockLockable(block.getType()))
			return;
		ChestLock m = LockAPI.getLock(block);
		if (m == null)
			return;
		if (m.getLocation().equals(block.getLocation()) // for double chests
				&& LockAPI.smartRemoveLock(m))
			event.getPlayer().sendMessage(ChatFormat.format("&a%1 Removed.", ChatFormat.getFriendlyMaterialName(m.getBlockId())));
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockExplode(BlockExplodeEvent event) {
		if (LockAPI.isWorldDisabled(event.getBlock().getWorld()))
			return;
		Block block = event.getBlock();
		if (!LockAPI.isBlockLockable(block.getType()))
			return;
		ChestLock m = LockAPI.getLock(block);
		if (m != null)
			event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		if (LockAPI.isWorldDisabled(event.getLocation().getWorld()))
			return;
		if (LockAPI.anvilsEnabled())
			handleAnvils(event.blockList(), false);
		for (Block block : event.blockList().toArray(new Block[event.blockList().size()])) {
			if (!LockAPI.isBlockLockable(block.getType()))
				continue;
			ChestLock m = LockAPI.getLock(block);
			if (m != null)
				event.blockList().remove(block);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {
		if (LockAPI.isWorldDisabled(event.getBlock().getWorld()))
			return;
		if (LockAPI.anvilsEnabled())
			if (handleAnvils(event.getBlock()) != null)
				event.setCancelled(true);
	}

	private boolean handleAnvils(List<Block> list, boolean returnTrueOnFind) {
		for (Block block : list.toArray(new Block[list.size()])) {
			Block up = block.getRelative(BlockFace.UP);
			ChestLock cl = null;
			boolean upG = isBlockGravityEffected(up.getType());
			if (upG)
				cl = LockAPI.getLock(up);
			if (cl != null) {
				if (returnTrueOnFind)
					return true;
				list.remove(block);
			}
			if (upG || isBlockGravityEffected(block.getType()))
				if (recursiveCheckGravityBlock(block) != null) {
					if (returnTrueOnFind)
						return true;
					list.remove(block);
				}
		}
		return false;
	}

	private ChestLock recursiveCheckGravityBlock(Block start) {
		Block up = start.getRelative(BlockFace.UP);
		while (true) {
			boolean gravity = isBlockGravityEffected(up.getType());

			if (!gravity)
				break;
			else {
				ChestLock cl = LockAPI.getLock(up);
				if (cl != null)
					return cl;
			}
			up = up.getRelative(BlockFace.UP);
		}
		return null;
	}

	private ChestLock handleAnvils(Block block) {
		Block up = block.getRelative(BlockFace.UP);
		ChestLock cl = null;
		boolean upG = isBlockGravityEffected(up.getType());
		if (upG)
			cl = LockAPI.getLock(up);
		if (cl != null)
			return cl;
		if (upG || isBlockGravityEffected(block.getType()))
			if ((cl = recursiveCheckGravityBlock(block)) != null)
				return cl;
		return cl;
	}

	private boolean isBlockGravityEffected(Material m) {
		return m == Material.SAND || m == Material.GRAVEL || m == Material.ANVIL || m == Material.DRAGON_EGG;
	}

	@EventHandler(ignoreCancelled = true)
	public void onMoveItem(InventoryMoveItemEvent event) {
		try {
			if (!allowTransfer(event.getSource(), event.getDestination()))
				event.setCancelled(true);

		} catch (Exception e) {
			Bukkit.getLogger().log(Level.SEVERE, "[LockStock]Failed to handle InventoryMoveEvent", e);
		}
	}

	private boolean allowTransfer(Inventory isrc, Inventory idest) {
		Location src = getLocation(isrc.getHolder());
		if (LockAPI.isWorldDisabled(src.getWorld()))
			return true;
		ChestLock srcLock = LockAPI.getLock(src);
		if (srcLock == null)
			return true;

		Location dest = getLocation(idest.getHolder());
		ChestLock destLock = LockAPI.getLock(dest);

		return LockAPI.allowTransfer(srcLock, destLock);
	}

	private Location getLocation(InventoryHolder ih) {
		if (ih instanceof BlockState)
			return ((BlockState) ih).getLocation();
		else if (ih instanceof DoubleChest)
			return ((DoubleChest) ih).getLocation();
		return null;
	}
}
