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

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.glasspath.common.media.player.FramePanel;
import org.glasspath.common.media.player.IVideoPlayer;
import org.glasspath.common.os.OsUtils;
import org.glasspath.common.swing.tools.AbstractTools;

public class ViewTools extends AbstractTools<IVideoPlayer> {

	public ViewTools(IVideoPlayer context) {
		super(context, "View");

		JCheckBoxMenuItem alwaysOnTopMenuItem = new JCheckBoxMenuItem("Always on Top"); // TODO
		menu.add(alwaysOnTopMenuItem);
		alwaysOnTopMenuItem.setSelected(context.getPreferences().getBoolean("alwaysOnTop", false));
		alwaysOnTopMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				context.getFrame().setAlwaysOnTop(alwaysOnTopMenuItem.isSelected());
				context.getPreferences().putBoolean("alwaysOnTop", alwaysOnTopMenuItem.isSelected());
			}
		});
		context.getFrame().setAlwaysOnTop(alwaysOnTopMenuItem.isSelected());

		menu.addSeparator();

		JMenuItem resetViewMenuItem = new JMenuItem("Reset View");
		menu.add(resetViewMenuItem);
		resetViewMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, OsUtils.CTRL_OR_CMD_MASK));
		resetViewMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (context.getVideoPlayerPanel() != null) {
					context.getVideoPlayerPanel().resetView();
				}
			}
		});

		// TODO
		JCheckBoxMenuItem overlayMenuItem = new JCheckBoxMenuItem("Show overlay");
		menu.add(overlayMenuItem);
		overlayMenuItem.setSelected(FramePanel.TODO_TEST_OVERLAY);
		overlayMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_9, OsUtils.CTRL_OR_CMD_MASK));
		overlayMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				FramePanel.TODO_TEST_OVERLAY = overlayMenuItem.isSelected();
				if (context.getVideoPlayerPanel() != null) {
					context.getVideoPlayerPanel().getComponent().repaint();
				}
			}
		});

	}

}
