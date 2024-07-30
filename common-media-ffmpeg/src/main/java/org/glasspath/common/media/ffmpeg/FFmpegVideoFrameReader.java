/*
 * This file is part of Glasspath Common.
 * Copyright (C) 2011 - 2024 Remco Poelstra
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
package org.glasspath.common.media.ffmpeg;

import java.io.File;

import org.bytedeco.javacv.FFmpegFrameGrabber;

public class FFmpegVideoFrameReader extends FFmpegFrameGrabber implements IFFVideoFrameReader {

	private int maxSeekCount = -1;
	private int seekCount = 0;

	public FFmpegVideoFrameReader(File file) {
		super(file);
	}

	public FFmpegVideoFrameReader(String path) {
		super(path);
	}

	@Override
	public int getMaxSeekCount() {
		return maxSeekCount;
	}

	@Override
	public void setMaxSeekCount(int maxSeekCount) {
		this.maxSeekCount = maxSeekCount;
	}

	@Override
	public void setVideoTimestamp(long timestamp) throws Exception {
		seekCount = 0;
		super.setVideoTimestamp(timestamp);
	}

	@Override
	public double getFrameRate() {

		if (maxSeekCount >= 0) {

			// TODO? This is a hack to trick FFmpegFrameGrabber,
			// it uses getFrameRate() to calculate the frame duration,
			// this way 'if (ts + frameDuration > timestamp)' will return immediately
			// getFrameRate() is called 2 times before the while loop and 2 times
			// in each iteration.
			if (seekCount >= (maxSeekCount * 2) + 2) {
				return 0.01;
			} else {
				seekCount++;
				return super.getFrameRate();
			}

		} else {
			return super.getFrameRate();
		}

	}

}
