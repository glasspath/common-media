/*
 * This file is part of Glasspath Common.
 * Copyright (C) 2011 - 2023 Remco Poelstra
 * Authors: Remco Poelstra
 * 
 * This program is offered under a commercial and under the AGPL license.
 * For commercial licensing, contact us at https://glasspath.org. For AGPL licensing, see below.
 * 
 * AGPL licensing:
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.glasspath.common.media.player.tools;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.prefs.Preferences;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.glasspath.common.media.player.IVideoPlayer;
import org.glasspath.common.media.player.IVideoPlayerPanel;
import org.glasspath.common.os.OsUtils;
import org.glasspath.common.os.preferences.BoolPref;
import org.glasspath.common.swing.tools.AbstractTools;

public class ViewTools extends AbstractTools<IVideoPlayer> {

	public static final BoolPref ALWAYS_ON_TOP_PREF = new BoolPref("alwaysOnTop", false);
	public static final BoolPref SHOW_OVERLAY_PREF = new BoolPref("showOverlay", true);

	public ViewTools(IVideoPlayer context) {
		super(context, "View");

		menu.add(createAlwaysOnTopMenuItem(context));

		// Rest of menu is populated after IVideoPlayerPanel is created

	}

	public static JCheckBoxMenuItem createAlwaysOnTopMenuItem(IVideoPlayer context) {

		JCheckBoxMenuItem alwaysOnTopMenuItem = new JCheckBoxMenuItem("Always on Top");
		alwaysOnTopMenuItem.setSelected(ALWAYS_ON_TOP_PREF.get(context.getPreferences()));
		alwaysOnTopMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				context.getFrame().setAlwaysOnTop(alwaysOnTopMenuItem.isSelected());
				ALWAYS_ON_TOP_PREF.put(context.getPreferences(), alwaysOnTopMenuItem.isSelected());
			}
		});

		return alwaysOnTopMenuItem;

	}

	public static JCheckBoxMenuItem createShowOverlayMenuItem(IVideoPlayerPanel videoPlayerPanel, Preferences preferences) {

		JCheckBoxMenuItem showOverlayMenuItem = new JCheckBoxMenuItem("Show overlay");
		showOverlayMenuItem.setSelected(SHOW_OVERLAY_PREF.get(preferences));
		showOverlayMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_9, OsUtils.CTRL_OR_CMD_MASK));
		showOverlayMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				videoPlayerPanel.setOverlayVisible(showOverlayMenuItem.isSelected());
				videoPlayerPanel.getComponent().repaint();
				SHOW_OVERLAY_PREF.put(preferences, showOverlayMenuItem.isSelected());
			}
		});

		// TODO: Listen for changes and update menu item

		return showOverlayMenuItem;

	}

	public static JMenuItem createResetViewMenuItem(IVideoPlayerPanel videoPlayerPanel) {

		JMenuItem resetViewMenuItem = new JMenuItem("Reset View");
		resetViewMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, OsUtils.CTRL_OR_CMD_MASK));
		resetViewMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				videoPlayerPanel.resetView();
			}
		});

		return resetViewMenuItem;

	}

}
