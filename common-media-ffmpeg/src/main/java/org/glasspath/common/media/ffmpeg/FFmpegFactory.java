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

public class FFmpegFactory {

	private static FFmpegFactory instance = new FFmpegFactory();

	public FFmpegFactory() {

	}

	public static FFmpegFactory getInstance() {
		return instance;
	}

	public static void setInstance(FFmpegFactory instance) {
		FFmpegFactory.instance = instance;
	}

	public IFFVideoFrameReader createVideoFrameReader(File file) {
		return new FFmpegVideoFrameReader(file);
	}

	public IFFVideoFrameReader createVideoFrameReader(String path) {
		return new FFmpegVideoFrameReader(path);
	}

}
