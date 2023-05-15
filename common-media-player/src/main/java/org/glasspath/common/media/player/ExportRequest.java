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

import java.io.File;

import org.glasspath.common.media.video.Resolution;

public abstract class ExportRequest {

	public static final int DEFAULT_WIDTH = (int) (Resolution.HD_720P.getWidth() * 0.5);
	public static final int DEFAULT_HEIGHT = (int) (Resolution.HD_720P.getHeight() * 0.5);
	public static final int DEFAULT_INTERVAL = 100;

	private final File file;
	private int width = DEFAULT_WIDTH;
	private int height = DEFAULT_HEIGHT;
	private int interval = DEFAULT_INTERVAL;

	public ExportRequest(File file) {
		this.file = file;
	}

	public File getFile() {
		return file;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getInterval() {
		return interval;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	public abstract void update(int progress, int total);

	public abstract void finish();

}
