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

import org.glasspath.common.media.rtsp.TrackInfo.AudioTrackInfo;
import org.glasspath.common.media.rtsp.TrackInfo.TrackIdentifier;
import org.glasspath.common.media.rtsp.TrackInfo.VideoTrackInfo;

public class RtspDescribeResponseParser extends RtspResponseParser {

	public static final String M_VIDEO_KEY_LOWER_CASE = "m=video ";
	public static final String M_AUDIO_KEY_LOWER_CASE = "m=audio ";
	public static final String A_CONTROL_KEY_LOWER_CASE = "a=control:";
	public static final String A_FRAME_RATE_KEY_LOWER_CASE = "a=framerate:";
	public static final String A_SPROP_PARAMETER_SETS_KEY_LOWER_CASE = "sprop-parameter-sets=";

	private VideoTrackInfo videoTrackInfo = null;
	private AudioTrackInfo audioTrackInfo = null;
	private TrackInfo lastConfiguredTrack = null;
	private int indexOf = -1;

	public RtspDescribeResponseParser() {

	}

	public VideoTrackInfo getVideoTrackInfo() {
		return videoTrackInfo;
	}

	public AudioTrackInfo getAudioTrackInfo() {
		return audioTrackInfo;
	}

	@Override
	public void parseMessageLine(String line) {

		String lineLowerCase = line.toLowerCase();

		if (videoTrackInfo == null && lineLowerCase.startsWith(M_VIDEO_KEY_LOWER_CASE)) {

			videoTrackInfo = new VideoTrackInfo();
			lastConfiguredTrack = videoTrackInfo;

			updateMediaInfo(videoTrackInfo, line.substring(M_VIDEO_KEY_LOWER_CASE.length()));

		} else if (audioTrackInfo == null && lineLowerCase.startsWith(M_AUDIO_KEY_LOWER_CASE)) {

			audioTrackInfo = new AudioTrackInfo();
			lastConfiguredTrack = audioTrackInfo;

			updateMediaInfo(audioTrackInfo, line.substring(M_AUDIO_KEY_LOWER_CASE.length()));

		} else if (lastConfiguredTrack != null && lastConfiguredTrack == videoTrackInfo && lineLowerCase.startsWith(A_FRAME_RATE_KEY_LOWER_CASE)) {

			try {
				videoTrackInfo.setFrameRate(Double.parseDouble(line.substring(A_FRAME_RATE_KEY_LOWER_CASE.length())));
			} catch (Exception e) {
				e.printStackTrace();
			}

		} else if (lastConfiguredTrack != null && lineLowerCase.startsWith(A_CONTROL_KEY_LOWER_CASE)) {

			String trackIdString = line.substring(A_CONTROL_KEY_LOWER_CASE.length());
			lastConfiguredTrack.setControl(trackIdString);

			TrackIdentifier trackIdentifier;

			if (trackIdString.startsWith(TrackIdentifier.TRACK_ID.identifier)) {
				trackIdentifier = TrackIdentifier.TRACK_ID;
			} else if (trackIdString.startsWith(TrackIdentifier.TRACK.identifier)) {
				trackIdentifier = TrackIdentifier.TRACK;
			} else if (trackIdString.startsWith(TrackIdentifier.STREAM.identifier)) {
				trackIdentifier = TrackIdentifier.STREAM;
			} else {
				trackIdentifier = null;
			}

			if (trackIdentifier != null && trackIdString.length() > trackIdentifier.identifier.length()) {

				try {

					final int trackId = Integer.parseInt(trackIdString.substring(trackIdentifier.identifier.length()));

					lastConfiguredTrack.setTrackIdentifier(trackIdentifier);
					lastConfiguredTrack.setTrackId(trackId);

				} catch (Exception e) {
					e.printStackTrace();
				}

			}

		} else if (lastConfiguredTrack != null && (indexOf = lineLowerCase.indexOf(A_SPROP_PARAMETER_SETS_KEY_LOWER_CASE)) >= 0) {

			String spropParameterSets = line.substring(indexOf + A_SPROP_PARAMETER_SETS_KEY_LOWER_CASE.length());
			if ((indexOf = spropParameterSets.indexOf(";")) > 0) {
				spropParameterSets = spropParameterSets.substring(0, indexOf);
			}

			lastConfiguredTrack.setSpropsParameterSets(spropParameterSets);

		}

	}

	private void updateMediaInfo(TrackInfo track, String mediaInfoAsString) {

		String[] mediaInfo = mediaInfoAsString.split(" ");
		if (mediaInfo.length == 3) {

			try {
				track.setMediaPort(Integer.parseInt(mediaInfo[0]));
			} catch (Exception e) {
				e.printStackTrace();
			}

			track.setMediaTransportProtocol(mediaInfo[1]);

			try {
				track.setMediaFormat(Integer.parseInt(mediaInfo[2]));
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

}
