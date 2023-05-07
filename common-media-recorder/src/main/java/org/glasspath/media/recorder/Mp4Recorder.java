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

import static org.jcodec.common.io.NIOUtils.writableChannel;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.glasspath.common.media.h264.H264NalUnit;
import org.glasspath.common.media.rtsp.H264ParameterSets;
import org.glasspath.common.media.video.Resolution;
import org.jcodec.common.Codec;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Packet.FrameType;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.Brand;

public abstract class Mp4Recorder extends H264NalUnitRecorder {

	public static boolean TODO_DEBUG = false;
	public static boolean TODO_USE_FIXED_30FPS_PTS = true;

	private SeekableByteChannel sink = null;
	private Mp4Muxer muxer = null;
	private MuxerTrack videoTrack = null;
	private int frameNumber = 0;
	private long bytesWritten = 0;

	private long todoFixed30FpsPts = 0;

	public Mp4Recorder() {

	}

	@Override
	protected boolean createFile(String recordPath, Resolution resolution, H264ParameterSets parameterSets, long pts, long created) {

		try {

			if (muxer != null) {
				System.err.println("Mp4Recorder: Muxer was not closed!");
			}

			if (TODO_DEBUG) {
				System.out.println("Creating MP4 muxer for file: " + recordPath);
			}

			sink = writableChannel(new File(recordPath));
			muxer = Mp4Muxer.createMp4Muxer(sink, Brand.MP4);
			muxer.setCreated(created);

			VideoCodecMeta videoCodecMeta = VideoCodecMeta.createSimpleVideoCodecMeta(new Size(getResolution().getWidth(), getResolution().getHeight()), ColorSpace.YUV420);

			videoTrack = muxer.addVideoTrack(Codec.H264, videoCodecMeta);

			if (videoTrack != null && parameterSets != null && parameterSets.sequenceParameterSet != null && parameterSets.pictureParameterSet != null) {

				Packet frame = nextFrame(parameterSets.sequenceParameterSet, pts, 0);
				if (frame != null) {

					videoTrack.addFrame(frame);
					bytesWritten += frame.data.limit();

					frame = nextFrame(parameterSets.pictureParameterSet, pts, 0);
					if (frame != null) {

						videoTrack.addFrame(frame);
						bytesWritten += frame.data.limit();

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
	protected boolean writeNalUnit(H264NalUnit nalUnit, long pts, long duration) {

		if (videoTrack != null) {

			try {

				Packet frame = nextFrame(nalUnit, pts, duration);
				if (frame != null) {
					videoTrack.addFrame(frame);
					bytesWritten += frame.data.limit();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		return false;

	}

	@Override
	protected long getBytesWritten() {
		return bytesWritten;
	}
	
	protected abstract UUIDBox createUuidBox();

	@Override
	protected boolean closeFile() {

		boolean result = true;

		if (muxer != null) {

			try {
				muxer.setUuidBox(createUuidBox());
				muxer.finish();
			} catch (IOException e) {
				result = false;
				e.printStackTrace();
			}

			if (sink != null) {

				try {
					sink.close();
				} catch (IOException e) {
					result = false;
					e.printStackTrace();
				}

			}

		} else {
			result = false;
		}

		videoTrack = null;
		muxer = null;
		sink = null;

		if (TODO_DEBUG) {
			System.out.println(frameNumber + " NAL units recorded");
		}

		frameNumber = 0;
		bytesWritten = 0;

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
