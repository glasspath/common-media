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
package org.glasspath.common.media.ffmpeg.player;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.glasspath.common.media.player.IVideoPlayer;
import org.glasspath.common.media.player.IVideoPlayerPanel;
import org.glasspath.common.media.player.VideoPlayer;
import org.glasspath.common.media.video.Video;
import org.glasspath.common.swing.frame.FrameUtils;
import org.glasspath.common.swing.theme.Theme;

public class FFVideoPlayer extends VideoPlayer {

	public FFVideoPlayer(Video video) {
		super(video);
	}

	public FFVideoPlayer(String path) {
		super(path);
	}

	public FFVideoPlayer(Video video, boolean exitOnClose, boolean startSocketClient, int port) {
		super(video, exitOnClose, startSocketClient, port);
	}

	public FFVideoPlayer(String path, int port) {
		super(path, port);
	}

	@Override
	protected IVideoPlayerPanel createVideoPlayerPanel(IVideoPlayer videoPlayer, Video video) {
		return new FFVideoPlayerPanel(videoPlayer, video);
	}

	@Override
	protected void shutdown() {
		System.out.println("Shutdown FFVideoPlayer");
	}

	public static void main(String[] args) {

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

						// new FFVideoPlayer(args[0]);
						new FFVideoPlayer(loadFromPath(args[0]), true, false, 0); // TODO!

					} else if (args.length == 2) {

						try {
							new FFVideoPlayer(args[0], Integer.parseInt(args[1]));
						} catch (NumberFormatException e) {
							e.printStackTrace();
						}

					}

				} else {
					new FFVideoPlayer(null, true, false, 0);
				}

			}
		});

	}

}
