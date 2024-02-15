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

public enum Resolution {

	W640_H480("640x480, 480p, SD, VGA, 4:3", 640, 480),
	W800_H600("800x600, SVGA, 4:3", 800, 600),
	W960_H540("960x540, 1080p / 2, 16:9", 960, 540),
	W1280_H720("1280x720, 720p, HD, 16:9", 1280, 720),
	W1920_H1080("1920x1080, 1080p, FHD, 16:9", 1920, 1080),
	W2048_H1080("2048x1080, 1440p, 2K", 2048, 1080),
	W2560_H1440("2560x1440, 1440p, QHD, 16:9", 2560, 1440),
	W3840_H2160("3840x2160, 2160p, 4K, 19:10", 3840, 2160),
	W7680_H4320("7680x4320, 4320p, 8K, 16:9", 7680, 4320);

	private final String name;
	private final int width;
	private final int height;

	private Resolution(String name, int width, int height) {
		this.name = name;
		this.width = width;
		this.height = height;
	}

	public String getName() {
		return name;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	@Override
	public String toString() {
		return name;
	}

}
