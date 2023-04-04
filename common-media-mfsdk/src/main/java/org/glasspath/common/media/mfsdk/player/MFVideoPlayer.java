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
package org.glasspath.common.media.mfsdk.player;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.glasspath.common.media.mfsdk.MFUtils;
import org.glasspath.common.media.player.IVideoPlayer;
import org.glasspath.common.media.player.IVideoPlayerPanel;
import org.glasspath.common.media.player.VideoPlayer;
import org.glasspath.common.media.video.Video;
import org.glasspath.common.swing.frame.FrameUtils;
import org.glasspath.common.swing.theme.Theme;

public class MFVideoPlayer extends VideoPlayer {

	public MFVideoPlayer(Video video) {
		super(video);
	}

	public MFVideoPlayer(String path) {
		super(path);
	}

	public MFVideoPlayer(Video video, boolean exitOnClose, boolean startSocketClient, int port) {
		super(video, exitOnClose, startSocketClient, port);
	}

	public MFVideoPlayer(String path, int port) {
		super(path, port);
	}

	@Override
	protected IVideoPlayerPanel createVideoPlayerPanel(IVideoPlayer videoPlayer, Video video) {
		return new MFVideoPlayerPanel(videoPlayer, video);
	}

	@Override
	protected void shutdown() {
		MFUtils.INSTANCE.shutdown();
	}

	public static void main(String[] args) {

		MFUtils.INSTANCE.start();

		System.setProperty("sun.awt.noerasebackground", "true");
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);

		Theme.load(Theme.THEME_DARK.getId());

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {

				try {
					FrameUtils.installLookAndFeel(null); // TODO
				} catch (Exception e) {
					e.printStackTrace(); // TODO
				}

				if (args != null && (args.length == 1 || args.length == 2)) {

					if (args.length == 1) {

						new MFVideoPlayer(args[0]);

					} else if (args.length == 2) {

						try {
							new MFVideoPlayer(args[0], Integer.parseInt(args[1]));
						} catch (NumberFormatException e) {
							e.printStackTrace();
						}

					}

				} else {
					new MFVideoPlayer(new Video(), true, false, 0);
				}

			}
		});

	}

}
