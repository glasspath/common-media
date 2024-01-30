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
package org.glasspath.common.media.video;

public abstract class FrameLoader {

	protected DefaultVideo video = null;

	public FrameLoader() {

	}

	public void installOnVideo(DefaultVideo video) {
		installOnVideo(video, 0, 0, true);
	}

	public void installOnVideo(DefaultVideo video, int width, int height, boolean closeFile) {
		this.video = video;
	}

	public void loadFrame(FrameLoaderCallback callback, long timestamp, int width, int height) {
		loadFrame(callback, timestamp, width, height, false);
	}

	public abstract boolean loadFrame(FrameLoaderCallback callback, long timestamp, int width, int height, boolean returnFirstFrame);

	public abstract void loadFrames(FrameLoaderCallback callback, long from, long to, int frameHeight, int totalWidth, int frameSpacing);

	public abstract void frameLoaderCallbackCanceled(FrameLoaderCallback callback);

	public abstract void close();

}
