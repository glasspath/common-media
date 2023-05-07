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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.FileTypeBox;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.MovieHeaderBox;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

public class Mp4Muxer extends MP4Muxer {

	private long created = System.currentTimeMillis();
	private UUIDBox uuidBox = null;

	public Mp4Muxer(SeekableByteChannel output, FileTypeBox fileTypeBox) throws IOException {
		super(output, fileTypeBox);
	}

	public MovieBox finalizeHeader() throws IOException {

		MovieBox movie = super.finalizeHeader();

		if (movie != null) {

			List<Box> boxes = movie.getBoxes();
			if (boxes.size() > 0) {

				Box box = boxes.get(0);
				if (box instanceof MovieHeaderBox) {

					MovieHeaderBox org = (MovieHeaderBox) box;

					MovieHeaderBox movieHeaderBox = MovieHeaderBox.createMovieHeaderBox(
							org.getTimescale(),
							org.getDuration(),
							org.getRate(),
							org.getVolume(),
							created,
							created,
							org.getMatrix(),
							org.getNextTrackId());

					boxes.remove(0);
					boxes.add(0, movieHeaderBox);

				}

			}

		}

		return movie;

	}

	@Override
	public void storeHeader(MovieBox movie) throws IOException {

		long mdatSize = out.position() - mdatOffset + 8;
		MP4Util.writeMovie(out, movie);

		if (uuidBox != null) {

			int sizeHint = uuidBox.estimateSize() + (4 << 10);

			ByteBuffer buf = ByteBuffer.allocate(sizeHint * 4);
			uuidBox.write(buf);
			buf.flip();
			out.write(buf);

		}

		out.setPosition(mdatOffset);
		NIOUtils.writeLong(out, mdatSize);

	}

	public long getCreated() {
		return created;
	}

	public void setCreated(long created) {
		this.created = created;
	}

	public UUIDBox getUuidBox() {
		return uuidBox;
	}

	public void setUuidBox(UUIDBox uuidBox) {
		this.uuidBox = uuidBox;
	}

	public static Mp4Muxer createMp4Muxer(SeekableByteChannel output, Brand brand) throws IOException {
		return new Mp4Muxer(output, brand.getFileTypeBox());
	}

}
