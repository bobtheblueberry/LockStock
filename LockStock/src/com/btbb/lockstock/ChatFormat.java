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

import java.util.LinkedList;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

public class ChatFormat {

	private ChatFormat() {
	}

	/**
	 * Inserts numbers and color coding
	 * 
	 * 
	 * @param message Message to format
	 * @param objects %1 %2 %3, etc
	 * @return formatted string
	 */
	public static String format(String message, Object... objects) {
		if (message == null)
			return null;
		String funnyChar = new Character((char) 167).toString();
		message = message.replace("&i", ChatColor.ITALIC.toString());
		message = message.replace("&", funnyChar);

		int i = 1;
		if (objects != null)
			for (Object o : objects)
				message = message.replace("%" + i++, (o != null) ? o.toString() : "null");
		return message;
	}

	public static String getFriendlyMaterialName(int id) {
		@SuppressWarnings("deprecation")
		Material m = Material.getMaterial(id);
		return getFriendlyMaterialName(m);
	}

	public static String getFriendlyMaterialName(Material m) {
		String s = m.toString().toLowerCase();
		s = m.toString().toUpperCase().substring(0, 1) + m.toString().toLowerCase().substring(1);
		String[] split = s.split("_");
		s = split[0];
		for (int i = 1; i < split.length; i++) {
			s = s + " " + split[i].substring(0, 1).toUpperCase() + split[i].substring(1);
		}

		return s;
	}

	public static PageBuilder getPageBuilder() {
		return new PageBuilder();
	}

	public static class PageBuilder {
		LinkedList<String> lines;

		public PageBuilder() {
			this.lines = new LinkedList<String>();
		}

		public PageBuilder addPlainMessage(String msg) {
			lines.add(msg);
			return this;
		}

		public PageBuilder addFormattedMessage(String msg, Object... objects) {
			lines.add(ChatFormat.format(msg, objects));
			return this;
		}

		public void send(CommandSender s, String title, String command, String pageStr) {
			send(s, title, command, pageStr, false);
		}

		public int getLines() {
			return lines.size();
		}

		public void send(CommandSender s, String title, String command, String pageStr, boolean pagesIn2ndRow) {
			int page = 1;
			if (pageStr != null)
				try {
					page = Integer.parseInt(pageStr);
				} catch (NumberFormatException exc) {
				}
			send(s, title, command, page, pagesIn2ndRow);
		}

		public void send(CommandSender s, String title, String command, int page) {
			send(s, title, command, page, false);
		}

		/**
		 * This method half heartedly tries to send only 8 lines or less to the
		 * player at a time
		 * 
		 * @param s
		 * @param title
		 * @param command
		 * @param page
		 * @param pagesIn2ndRow
		 */
		public void send(CommandSender s, String title, String command, int page, boolean pagesIn2ndRow) {
			double dbl = Math.min(5, calculateDoubleLines());
			boolean hasPages = lines.size() + dbl > 9;
			int maxLines = (hasPages) ? 8 - (int) dbl : 9;
			int lastPage = (int) Math.ceil(((double) lines.size()) / (8.0d - dbl));
			if (page > lastPage)
				page = lastPage;
			if (page < 1)
				page = 1;
			String heading = getHeading(page, lastPage, command, pagesIn2ndRow);
			if (lines.size() > 7 && ChatColor.stripColor(heading).length() > 64) {
				maxLines = 7;
				lastPage = (int) Math.ceil(((double) lines.size()) / (7.0d - dbl));
				heading = getHeading(page, lastPage, command, pagesIn2ndRow);
				hasPages = true;
			}

			if (hasPages)
				s.sendMessage(heading);

			if (pagesIn2ndRow && hasPages)
				s.sendMessage(ChatFormat.format("%1&e --- &dPage (%2 of %3)", title, page, lastPage));
			else
				s.sendMessage(ChatFormat.format("%1", title));

			for (int i = (page - 1) * maxLines; i < lines.size() && i < (page - 1) * maxLines + maxLines; i++)
				s.sendMessage(lines.get(i));

		}

		private int calculateDoubleLines() {
			int i = 0;
			for (String s : lines)
				if (ChatColor.stripColor(s).length() > 64)
					i++;
			if (i > 3 && lines.size() > 8)
				i = i / (lines.size() / 8);
			return i;
		}

		private String getHeading(int page, int lastPage, String command, boolean pagesIn2ndRow) {
			if (pagesIn2ndRow)
				return (page < lastPage) ? format("&3Use /%1 %2 to view the next page", command, page + 1) : "";

			return ChatFormat.format("&dPage (%1 of %2)%3", page, lastPage,
					(page < lastPage) ? format("&3Use /%1 %2 to view the next page", command, page + 1) : "");
		}
	}
}
