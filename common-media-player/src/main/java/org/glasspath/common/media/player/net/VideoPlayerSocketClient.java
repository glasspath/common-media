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
package org.glasspath.common.media.player.net;

import org.glasspath.common.media.player.IVideoPlayerListener;
import org.glasspath.common.media.player.VideoPlayer;

public class VideoPlayerSocketClient extends AbstractSocketClient implements IVideoPlayerListener {

	public VideoPlayerSocketClient(VideoPlayer videoPlayer, int port) {
		super("127.0.0.1", port);
		videoPlayer.addVideoPlayerListener(this);
	}

	@Override
	public void clientConnected() {

	}

	@Override
	public void parseLine(String line) {

	}

	@Override
	public void clientDisconnected() {

	}

	@Override
	public void socketThreadExited() {

	}

	@Override
	public void videoOpened(String path) {
		String s = VideoPlayerSocketServer.FILE_OPENED_PREFIX + path;
		setWelcomeMessage(s);
		writeLine(s);
	}

	@Override
	public void videoClosed(String path) {
		String s = VideoPlayerSocketServer.FILE_CLOSED_PREFIX + path;
		setWelcomeMessage(s);
		writeLine(s);
	}

	@Override
	public void timestampChanged(long timestamp) {
		writeLine(VideoPlayerSocketServer.TIMESTAMP_CHANGED_PREFIX + timestamp);
	}

	@Override
	public void playbackEnded() {

	}

	@Override
	public void videoPlayerClosed() {
		String s = VideoPlayerSocketServer.VIDEO_PLAYER_CLOSED_PREFIX;
		setWelcomeMessage(s);
		writeLine(s);
	}

}
