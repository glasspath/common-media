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

public class VideoPlayerSocketServer extends AbstractSocketServer {

	public static final String FILE_OPENED_PREFIX = "file opened: ";
	public static final String FILE_CLOSED_PREFIX = "file closed: ";
	public static final String TIMESTAMP_CHANGED_PREFIX = "t: ";
	public static final String VIDEO_PLAYER_CLOSED_PREFIX = "video player closed";

	private final IVideoPlayerListener listener;

	public VideoPlayerSocketServer(IVideoPlayerListener listener, int portFrom, int portTo) {
		super(portFrom, portTo);
		this.listener = listener;
	}

	@Override
	public void socketServerStarted(int port) {

	}

	@Override
	public void parseLine(String line) {

		if (line.startsWith(TIMESTAMP_CHANGED_PREFIX)) {

			try {
				listener.timestampChanged(Long.parseLong(line.substring(TIMESTAMP_CHANGED_PREFIX.length())));
			} catch (Exception e) {
				e.printStackTrace();
			}

		} else if (line.startsWith(FILE_OPENED_PREFIX)) {
			listener.videoOpened(line.substring(FILE_OPENED_PREFIX.length()));
		} else if (line.startsWith(FILE_CLOSED_PREFIX)) {
			listener.videoClosed(line.substring(FILE_CLOSED_PREFIX.length()));
		} else if (line.startsWith(VIDEO_PLAYER_CLOSED_PREFIX)) {
			listener.videoPlayerClosed();
			videoPlayerClosed();
		}

	}

	@Override
	public void clientDisconnected() {

	}

	@Override
	public void socketThreadExited() {

	}

	public void videoPlayerClosed() {

	}

}
