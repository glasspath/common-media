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

import org.glasspath.common.media.video.Resolution;
import org.jcodec.common.Codec;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.Brand;

public class Mp4Recording extends Recording {

	private SeekableByteChannel sink = null;
	private Mp4Muxer muxer = null;
	private MuxerTrack videoTrack = null;

	public Mp4Recording(String path, Resolution resolution, long created, int timeScale) {
		super(path, created, timeScale);

		try {

			sink = writableChannel(new File(path));
			muxer = Mp4Muxer.createMp4Muxer(sink, Brand.MP4);
			muxer.setCreated(created);

			// TODO: Support more ColorSpaces?
			VideoCodecMeta videoCodecMeta = VideoCodecMeta.createSimpleVideoCodecMeta(new Size(resolution.getWidth(), resolution.getHeight()), ColorSpace.YUV420);

			videoTrack = muxer.addVideoTrack(Codec.H264, videoCodecMeta);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public boolean isReady() {
		return videoTrack != null;
	}

	public void addFrame(Packet frame) throws IOException {

		videoTrack.addFrame(frame);
		bytesWritten += frame.data.limit();

	}

	@Override
	public boolean close() {
		return close(null);
	}

	public boolean close(UUIDBox uuidBox) {

		boolean result = true;

		if (muxer != null) {

			try {
				if (uuidBox != null) {
					muxer.setUUIDBox(uuidBox);
				}
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

		return result;

	}

}
