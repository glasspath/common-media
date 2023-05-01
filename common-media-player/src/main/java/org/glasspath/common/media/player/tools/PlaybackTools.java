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

import org.glasspath.common.media.player.ILoopHandler;
import org.glasspath.common.media.player.IVideoPlayer;
import org.glasspath.common.media.player.IVideoPlayerPanel.Loop;
import org.glasspath.common.media.player.VideoPlayerAdapter;
import org.glasspath.common.os.preferences.BoolPref;
import org.glasspath.common.swing.tools.AbstractTools;

public class PlaybackTools extends AbstractTools<IVideoPlayer> {

	public static final BoolPref REPEAT_ENABLED_PREF = new BoolPref("repeatEnabled", true);

	public PlaybackTools(IVideoPlayer context) {
		super(context, "Playback");

		JCheckBoxMenuItem repeatEnabledMenuItem = new JCheckBoxMenuItem("Repeat");
		menu.add(repeatEnabledMenuItem);
		repeatEnabledMenuItem.setSelected(REPEAT_ENABLED_PREF.get(context.getPreferences()));
		repeatEnabledMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (context.getVideoPlayerPanel() != null) {
					context.getVideoPlayerPanel().setRepeatEnabled(repeatEnabledMenuItem.isSelected());
				}
				REPEAT_ENABLED_PREF.put(context.getPreferences(), repeatEnabledMenuItem.isSelected());
			}
		});

		JMenuItem playPauseMenuItem = new JMenuItem(IVideoPlayer.DEFAULT_PLAYBACK_STATE_PLAYING ? "Pause" : "Play");
		menu.add(playPauseMenuItem);
		playPauseMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
		playPauseMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (context.getVideoPlayerPanel() != null) {
					context.getVideoPlayerPanel().togglePlaying();
				}
			}
		});

		JMenuItem clearLoopMarkersMenuItem;
		JMenuItem exportLoopToGifMenuItem;
		ILoopHandler loopHandler = context.getLoopHandler();

		if (loopHandler != null) {

			menu.addSeparator();

			JMenuItem loopFromMenuItem = new JMenuItem("Loop From"); // TODO
			menu.add(loopFromMenuItem);
			loopFromMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.SHIFT_DOWN_MASK));
			loopFromMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (context.getVideoPlayerPanel() != null) {
						loopHandler.setLoopFrom(context.getVideoPlayerPanel().getTimestamp());
					}
				}
			});

			JMenuItem loopToMenuItem = new JMenuItem("Loop To"); // TODO
			menu.add(loopToMenuItem);
			loopToMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.SHIFT_DOWN_MASK));
			loopToMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (context.getVideoPlayerPanel() != null) {
						loopHandler.setLoopTo(context.getVideoPlayerPanel().getTimestamp());
					}
				}
			});

			clearLoopMarkersMenuItem = new JMenuItem("Clear Loop Markers"); // TODO
			menu.add(clearLoopMarkersMenuItem);
			clearLoopMarkersMenuItem.setEnabled(false);
			clearLoopMarkersMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.SHIFT_DOWN_MASK));
			clearLoopMarkersMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					loopHandler.clearLoopMarkers();
				}
			});

			exportLoopToGifMenuItem = new JMenuItem("Export Loop to Gif"); // TODO
			menu.add(exportLoopToGifMenuItem);
			exportLoopToGifMenuItem.setEnabled(false);
			exportLoopToGifMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.SHIFT_DOWN_MASK));
			exportLoopToGifMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					loopHandler.exportLoopToGif();
				}
			});

		} else {
			clearLoopMarkersMenuItem = null;
			exportLoopToGifMenuItem = null;
		}

		context.addVideoPlayerListener(new VideoPlayerAdapter() {

			@Override
			public void playbackStateChanged(boolean playing) {
				playPauseMenuItem.setText(context.getVideoPlayerPanel() != null && context.getVideoPlayerPanel().isPlaying() ? "Pause" : "Play");
			}

			@Override
			public void loopChanged(Loop loop) {
				if (clearLoopMarkersMenuItem != null && exportLoopToGifMenuItem != null) {
					clearLoopMarkersMenuItem.setEnabled(loop != null);
					exportLoopToGifMenuItem.setEnabled(loop != null && loop.fromTimestamp != null && loop.toTimestamp != null);
				}
			}
		});

	}

}
