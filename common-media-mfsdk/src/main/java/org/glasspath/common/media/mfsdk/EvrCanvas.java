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
package org.glasspath.common.media.mfsdk;

import java.awt.Canvas;
import java.awt.Graphics;

@SuppressWarnings("serial")
public class EvrCanvas extends Canvas {

	static {
		if (!MFUtils.isNativeLibraryLoaded()) {
			System.err.println("EvrCanvas: mfsdk lib is not loaded..");
		}
	}

	public static enum PlayerInstanceState {

		PLAYER_INSTANCE_STATE_FREE(0),
		PLAYER_INSTANCE_STATE_RESERVED(1),
		PLAYER_INSTANCE_STATE_RUNNING(2),
		PLAYER_INSTANCE_STATE_STOPPING(3);

		private final int value;

		private PlayerInstanceState(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

	}

	public static enum PlayerState {

		PLAYER_STATE_CLOSED(0),
		PLAYER_STATE_READY(1),
		PLAYER_STATE_OPEN_PENDING(2),
		PLAYER_STATE_STARTED(3),
		PLAYER_STATE_PAUSED(4),
		PLAYER_STATE_STOPPED(5),
		PLAYER_STATE_CLOSING(6);

		private final int value;

		private PlayerState(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

	}

	public static enum PlayerBufferingState {

		PLAYER_BUFFERING_STATE_IDLE(0),
		PLAYER_BUFFERING_STATE_START(1),
		PLAYER_BUFFERING_STATE_BUFFERING(2),
		PLAYER_BUFFERING_STATE_STOP(3);

		private final int value;

		private PlayerBufferingState(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

	}

	public EvrCanvas() {
		// setIgnoreRepaint(true);
	}

	public native long createPlayer();

	public native int startPlayer(long instance, int sourceType, String path, double scale);

	public native int getPlayerInstanceState(long instance);

	public native int play(long instance);

	public native int pause(long instance);

	public native int setRate(long instance, float rate);

	public native int seek(long instance, long timestamp);

	public native int getPlayerState(long instance);

	public native long getDuration(long instance);

	public native long getTimestamp(long instance);

	public native int getPlayerBufferingState(long instance);

	public native void queueNalUnit(long instance, byte[] nalUnitData, int nalUnitDataLength, long timestamp);

	public native void setVideoPosition(long instance, boolean customSrc, float srcLeft, float srcTop, float srcRight, float srcBottom, int dstLeft, int dstTop, int dstRight, int dstBottom);

	public native void stopPlayer(long instance);

	public native int setPaintBackgroundOnly(long instance, boolean paintBackgroundOnly);

	public native int repaintPlayer(long instance);

	@Override
	public void paint(Graphics g) {

	}

	@Override
	public void paintAll(Graphics g) {

	}

}
