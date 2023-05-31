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
package org.glasspath.media.recorder;

public abstract class Recording {

	protected String path = null;
	protected final long created;
	protected final int timeScale;
	protected long ptsStart = 0L;
	protected long ptsEnd = 0L;
	protected long frameCount = 0;
	protected long bytesWritten = 0;
	protected long ended = 0L;

	public Recording(String path, long created, int timeScale) {
		this.path = path;
		this.created = created;
		this.timeScale = timeScale;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public long getCreated() {
		return created;
	}

	public int getTimeScale() {
		return timeScale;
	}

	public long getDuration() {
		if (timeScale >= 1000 && ptsEnd > ptsStart) {
			return toMillis(ptsEnd - ptsStart);
		} else {
			return 0;
		}
	}

	private long toMillis(long time) {
		return time / (timeScale / 1000);
	}

	public long getFrameCount() {
		return frameCount;
	}

	public long getBytesWritten() {
		return bytesWritten;
	}

	public long getEnded() {
		return ended;
	}

	public abstract boolean close();

}
