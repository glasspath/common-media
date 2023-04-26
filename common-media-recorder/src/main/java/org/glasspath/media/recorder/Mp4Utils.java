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

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Atom;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.DataBox;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.NameBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.UdtaBox;

public class Mp4Utils {

	private Mp4Utils() {

	}

	public static UUIDBox parseUUIDBox(File source, UUID uuid) throws IOException {

		SeekableByteChannel input = null;

		try {
			input = NIOUtils.readableChannel(source);
			return parseUUIDBox(input, uuid);
		} finally {
			if (input != null) {
				input.close();
			}
		}

	}

	public static UUIDBox parseUUIDBox(SeekableByteChannel input, UUID uuid) throws IOException {

		for (Atom atom : MP4Util.getRootAtoms(input)) {

			if (UUIDBox.FOURCC.equals(atom.getHeader().getFourcc()) && atom.getHeader().getBodySize() >= UUIDBox.UUID_SIZE) {

				UUIDBox uuidBox = new UUIDBox(Header.createHeader(UUIDBox.FOURCC, 0));

				// Parse UUID only
				input.setPosition(atom.getOffset() + atom.getHeader().headerSize());
				uuidBox.parseUUID(NIOUtils.fetchFromChannel(input, UUIDBox.UUID_SIZE));

				// Parse data if UUID matches requested UUID
				if (uuid.equals(uuidBox.getUUID())) {

					if (atom.getHeader().getBodySize() < Box.MAX_BOX_SIZE) {
						uuidBox.parseData(NIOUtils.fetchFromChannel(input, (int) atom.getHeader().getBodySize() - UUIDBox.UUID_SIZE));
						return uuidBox;
					} else {
						return null;
					}

				}

			}

		}

		return null;

	}

	// TODO
	public static void todoInspectVideoFile(String path) {

		if (path.endsWith(".mp4")) {

			SeekableByteChannel input = null;

			try {

				input = NIOUtils.readableChannel(new File(path));

				UUIDBox uuidBox = Mp4Utils.parseUUIDBox(input, UUID.fromString("40279e33-acf7-470a-be18-d2b4290e930b"));
				if (uuidBox != null) {

					byte[] bytes = uuidBox.getData();
					if (bytes != null && bytes.length >= 2) {

						System.out.println("uuid box found, UUID = " + uuidBox.getUUID() + ", length = " + bytes.length + ", first = " + bytes[0] + ", last = " + bytes[bytes.length - 1]);

					}

				} else {
					System.err.println("uuid box not found..");
				}

				MovieBox moovBox = MP4Util.parseMovieChannel(input);
				if (moovBox != null) {

					UdtaBox udtaBox = NodeBox.findFirst(moovBox, UdtaBox.class, "udta");
					if (udtaBox != null) {

						NameBox nameBox = NodeBox.findFirst(udtaBox, NameBox.class, "name");
						if (nameBox != null) {

							System.out.println("name box found, name = " + nameBox.getName());

							DataBox dataBox = NodeBox.findFirst(udtaBox, DataBox.class, "data");
							if (dataBox != null) {

								byte[] bytes = dataBox.getData();
								if (bytes != null && bytes.length >= 2) {

									System.out.println("data box found, length = " + bytes.length + ", first = " + bytes[0] + ", last = " + bytes[bytes.length - 1]);

								}

							} else {
								System.err.println("data box not found..");
							}

						} else {
							System.err.println("name box not found..");
						}

					} else {
						System.err.println("udta box not found..");
					}

				} else {
					System.err.println("moov box not found..");
				}

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (input != null) {
					try {
						input.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		}

	}

}
