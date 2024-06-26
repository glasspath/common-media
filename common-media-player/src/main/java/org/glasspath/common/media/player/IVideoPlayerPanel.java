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

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;

import org.glasspath.common.media.video.Video;

public interface IVideoPlayerPanel {

	public JComponent getComponent();

	public void populateViewMenu(JMenu menu, Action editOverlayAction);

	public void videoPlayerShown();

	public void open(Video video);

	public Video getVideo();

	public void close();

	public boolean isRepeatEnabled();

	public void setRepeatEnabled(boolean repeatEnabled);

	public IOverlay getOverlay();

	public void setOverlay(IOverlay overlay);

	public boolean isOverlayVisible();

	public void setOverlayVisible(boolean visible);

	public void play();

	public void pause();

	public void togglePlaying();

	public boolean isPlaying();

	public void nextFrame(int step);

	public void previousFrame(int step);

	public long getTimestamp();

	public void setTimestamp(long timestamp, boolean play);

	public long getDuration();

	public Loop getLoop();

	public void setLoop(Loop loop);

	public void exportLoop(ExportRequest request);

	public void resetView();

	public void exit();

	public boolean isExited();

	public static class Loop {

		public Long fromTimestamp = null;
		public Long toTimestamp = null;

	}

}
