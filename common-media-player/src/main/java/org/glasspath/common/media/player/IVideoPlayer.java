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
package org.glasspath.common.media.player;

import java.util.prefs.Preferences;

import org.glasspath.common.media.player.IVideoPlayerListener.VideoPlayerStatistics;
import org.glasspath.common.media.player.IVideoPlayerPanel.Loop;
import org.glasspath.common.swing.FrameContext;

public interface IVideoPlayer extends FrameContext {

	public static final boolean DEFAULT_PLAYBACK_STATE_PLAYING = true;

	public IVideoPlayerPanel getVideoPlayerPanel();

	public ControlsBar getControlsBar();

	public Preferences getPreferences();

	public void addVideoPlayerListener(IVideoPlayerListener listener);

	public void removeVideoPlayerListener(IVideoPlayerListener listener);

	public void fireVideoOpened(String path);

	public void firePlaybackStateChanged(boolean playing);

	public void fireLoopChanged(Loop loop);

	public void fireTimestampChanged(long timestamp);

	public void fireRecordingStateChanged(boolean recording);

	public void fireStatisticsUpdated(VideoPlayerStatistics statistics);

	public void fireVideoClosed(String path);

	public void fireVideoPlayerClosed();

	public boolean isPlaying();

	public ILoopHandler getLoopHandler();

	public int getRate();

	public void exit();

}
