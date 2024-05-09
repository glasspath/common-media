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
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.glasspath.common.media.player.IVideoPlayer;
import org.glasspath.common.media.player.icons.Icons;
import org.glasspath.common.media.video.Video;
import org.glasspath.common.os.OsUtils;
import org.glasspath.common.swing.file.chooser.FileChooser;
import org.glasspath.common.swing.tools.AbstractTools;

public class FileTools extends AbstractTools<IVideoPlayer> {

	public FileTools(IVideoPlayer context) {
		super(context, "File");

		JMenuItem openMenuItem = new JMenuItem("Open");
		menu.add(openMenuItem);
		openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, OsUtils.CTRL_OR_CMD_MASK));
		openMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				// TODO
				List<String> extensions = new ArrayList<>();
				extensions.add("mp4");
				extensions.add("mkv");
				extensions.add("asf");

				// TODO: Create utility method that returns the selected File directly
				String path = FileChooser.browseForFileWithExtensions(extensions, Icons.motionPlayBlue, false, context.getFrame(), context.getPreferences(), "lastOpenedFile");
				if (path != null) {
					File file = new File(path);
					context.open(new Video(file.getName(), file.getAbsolutePath()));
				}

			}
		});

		JMenuItem closeMenuItem = new JMenuItem("Close");
		menu.add(closeMenuItem);
		closeMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				context.close();
			}
		});

		context.populateFileMenu(menu);

		menu.addSeparator();

		JMenuItem exitMenuItem = new JMenuItem("Exit");
		menu.add(exitMenuItem);
		exitMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				context.exit();
			}
		});

	}

}
