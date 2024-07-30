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

import java.util.Map;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

public interface IFFVideoFrameReader {

	public void start() throws FrameGrabber.Exception;

	public String getVideoCodecName();

	public void setVideoCodecName(String videoCodecName);

	public void setVideoOption(String key, String value);

	public Map<String, String> getMetadata();

	public Map<String, String> getVideoMetadata();

	public double getFrameRate();

	public long getLengthInTime();

	public long getTimestamp();

	public void setVideoTimestamp(long timestamp) throws FrameGrabber.Exception;

	public Frame grabFrame(boolean doAudio, boolean doVideo, boolean doProcessing, boolean keyFrames) throws FrameGrabber.Exception;

	public int getImageWidth();

	public void setImageWidth(int imageWidth);

	public int getImageHeight();

	public void setImageHeight(int imageHeight);

	public int getPixelFormat();

	public int getMaxSeekCount();

	public void setMaxSeekCount(int maxSeekCount);

	public void close() throws FrameGrabber.Exception;

}
