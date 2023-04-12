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
package org.glasspath.common.media.rtsp;

public class TrackInfo {

	public static enum TrackIdentifier {

		TRACK_ID("trackID="),
		TRACK("track"),
		STREAM("stream");

		public final String identifier;

		private TrackIdentifier(String identifier) {
			this.identifier = identifier;
		}

	}

	public static final String DEFAULT_MEDIA_TRANSPORT_PROTOCOL = "RTP/AVP";

	private String control = "";
	private TrackIdentifier trackIdentifier = TrackIdentifier.TRACK_ID;
	private int trackId = -1;
	private int mediaPort = 0;
	private String mediaTransportProtocol = DEFAULT_MEDIA_TRANSPORT_PROTOCOL;
	private int mediaFormat = 0;
	private String spropsParameterSets = null;

	public TrackInfo() {

	}

	public String getControl() {
		return control;
	}

	public void setControl(String control) {
		this.control = control;
	}

	public TrackIdentifier getTrackIdentifier() {
		return trackIdentifier;
	}

	public void setTrackIdentifier(TrackIdentifier trackIdentifier) {
		this.trackIdentifier = trackIdentifier;
	}

	public int getTrackId() {
		return trackId;
	}

	public void setTrackId(int trackId) {
		this.trackId = trackId;
	}

	public int getMediaPort() {
		return mediaPort;
	}

	public void setMediaPort(int mediaPort) {
		this.mediaPort = mediaPort;
	}

	public String getMediaTransportProtocol() {
		return mediaTransportProtocol;
	}

	public void setMediaTransportProtocol(String mediaTransportProtocol) {
		this.mediaTransportProtocol = mediaTransportProtocol;
	}

	public int getMediaFormat() {
		return mediaFormat;
	}

	public void setMediaFormat(int mediaFormat) {
		this.mediaFormat = mediaFormat;
	}

	public String getSpropsParameterSets() {
		return spropsParameterSets;
	}

	public void setSpropsParameterSets(String spropsParameterSets) {
		this.spropsParameterSets = spropsParameterSets;
	}

	public static String getDefaultMediaTransportProtocol() {
		return DEFAULT_MEDIA_TRANSPORT_PROTOCOL;
	}

	@Override
	public String toString() {
		return trackIdentifier.identifier + " = " + trackId;
	}

	public static class VideoTrackInfo extends TrackInfo {

		public static final double DEFAULT_FRAME_RATE = 30.0;
		public static final int DEFAULT_WIDTH = 1280;
		public static final int DEFAULT_HEIGHT = 780;

		private double frameRate = DEFAULT_FRAME_RATE;
		private int width = DEFAULT_WIDTH;
		private int height = DEFAULT_HEIGHT;

		public VideoTrackInfo() {

		}

		public double getFrameRate() {
			return frameRate;
		}

		public void setFrameRate(double frameRate) {
			this.frameRate = frameRate;
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

	}

	public static class AudioTrackInfo extends TrackInfo {

		public AudioTrackInfo() {

		}

	}

}
