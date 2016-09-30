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

import java.util.UUID;

import com.btbb.lockstock.meta.PasswordList;
import com.btbb.lockstock.permissions.LockCount;

/**
 * Player settings, such as passwords entered and locking options
 * 
 * @author Serge
 *
 */
public class PlayerSettings {

	public static enum AutoLockMode {
		ALWAYS_LOCK,NEVER,DEFAULT
	}
	
	public enum InteractMode {
		SINGLE, PERSISTENT
	}

	private LockCount lockCount;
	private PasswordList passwordList;
	private AutoLockMode autoLockMode = AutoLockMode.DEFAULT;
	private InteractMode interactMode = InteractMode.SINGLE;

	public InteractMode getInteractMode() {
		return interactMode;
	}

	public void setInteractMode(InteractMode interactMode) {
		this.interactMode = interactMode;
	}

	public AutoLockMode getAutoLockMode() {
		return autoLockMode;
	}

	public void setAutoLock(AutoLockMode autoLock) {
		this.autoLockMode = autoLock;
	}

	private UUID uuid;

	public PlayerSettings(UUID player) {
		this.setUuid(player);
	}

	public LockCount getLockCount() {
		return lockCount;
	}

	public void setLockCount(LockCount lockCount) {
		this.lockCount = lockCount;
	}

	public PasswordList getPasswordList() {
		return passwordList;
	}

	public void setPasswordList(PasswordList passwordList) {
		this.passwordList = passwordList;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public void resetLockCount() {
		this.lockCount = null;
	}
}
