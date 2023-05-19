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

import java.nio.ByteBuffer;

import org.glasspath.common.media.h264.H264NalUnit;
import org.glasspath.common.media.rtsp.H264ParameterSets;
import org.glasspath.common.media.video.Resolution;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Packet.FrameType;

public abstract class Mp4Recorder extends H264NalUnitRecorder<Mp4Recording> {

	public static boolean TODO_DEBUG = false;
	public static boolean TODO_USE_FIXED_30FPS_PTS = true;

	private Mp4Recording recording = null;
	private int frameNumber = 0;

	private long todoFixed30FpsPts = 0;

	public Mp4Recorder() {

	}

	@Override
	protected boolean createFile(String recordPath, Resolution resolution, H264ParameterSets parameterSets, long pts, long created) {

		frameNumber = 0;

		try {

			if (TODO_DEBUG) {
				System.out.println("Creating MP4 muxer for file: " + recordPath);
			}

			recording = new Mp4Recording(recordPath, resolution, created, getTimeScale());
			if (recording.isReady() && parameterSets != null && parameterSets.sequenceParameterSet != null && parameterSets.pictureParameterSet != null) {

				Packet frame = nextFrame(parameterSets.sequenceParameterSet, pts, 0);
				if (frame != null) {

					recording.addFrame(frame);

					frame = nextFrame(parameterSets.pictureParameterSet, pts, 0);
					if (frame != null) {

						recording.addFrame(frame);
						recording.ptsStart = frame.pts;

						return true;

					}

				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;

	}

	@Override
	protected Mp4Recording getRecording() {
		return recording;
	}

	@Override
	protected boolean writeNalUnit(H264NalUnit nalUnit, long pts, long duration) {

		if (recording.isReady()) {

			try {

				Packet frame = nextFrame(nalUnit, pts, duration);
				if (frame != null) {
					recording.addFrame(frame);
					recording.ptsEnd = frame.pts + frame.duration;
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		return false;

	}

	@Override
	protected long getBytesWritten() {
		return recording != null ? recording.getBytesWritten() : 0;
	}

	protected UUIDBox createUUIDBox() {
		return null;
	}

	@Override
	protected void recordingEnded(long timestamp) {
		if (recording != null) {
			recording.ended = timestamp;
		}
	}

	@Override
	protected boolean closeFile(Mp4Recording recording) {

		boolean result = true;

		if (recording != null) {
			result = recording.close(createUUIDBox());
		} else {
			result = false;
		}

		if (TODO_DEBUG) {
			System.out.println(frameNumber + " NAL units recorded");
		}

		return result;

	}

	private Packet nextFrame(H264NalUnit nalUnit, long pts, long duration) {

		Packet frame = null;

		try {

			if (TODO_DEBUG) {
				if (nalUnit.isSequenceParameterSet()) {
					System.out.println("Writing sequence parameter set to mp4 file");
				} else if (nalUnit.isPictureParameterSet()) {
					System.out.println("Writing picture parameter set to mp4 file");
				} else if (nalUnit.isIFrame()) {
					System.out.println("Writing IFrame to mp4 file");
				} else if (!nalUnit.isFrame()) {
					System.err.println("Writing unknown NalUnit to mp4 file");
				}
			}

			ByteBuffer byteBuffer = ByteBuffer.wrap(nalUnit.bytes);

			FrameType frameType;
			if (nalUnit.isIFrame()) {
				frameType = FrameType.KEY;
			} else if (nalUnit.isFrame()) {
				frameType = FrameType.INTER;
			} else {
				frameType = FrameType.UNKNOWN; // TODO? (This will be set for SPS/PPS)
			}

			// Tape timecode: https://github.com/jcodec/jcodec/issues/21
			// Just ignore, should be null. This is used by the older brother of MP4 - Apple Quicktime which supports tape timecode

			if (TODO_USE_FIXED_30FPS_PTS) {

				duration = 3333;
				frame = new Packet(byteBuffer, todoFixed30FpsPts, getTimeScale(), duration, frameNumber, frameType, null, frameNumber);
				todoFixed30FpsPts += duration;

			} else {
				frame = new Packet(byteBuffer, pts, getTimeScale(), duration, frameNumber, frameType, null, frameNumber);
			}

			// frame = new Packet(byteBuffer, pts, getTimeScale(), duration, frameNumber, frameType, null, frameNumber);
			// frame = MP4Packet.createMP4Packet(byteBuffer, pts, getTimeScale(), duration, frameNumber, frameType, null, frameNumber, pts, frameNumber);

			frameNumber++;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return frame;

	}

}
