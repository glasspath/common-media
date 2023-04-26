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
import java.util.UUID;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.Header;

public class UUIDBox extends Box {

	public static final String FOURCC = "uuid";
	public static final int UUID_SIZE = 16;

	private UUID uuid = null;
	private byte[] data = null;

	public UUIDBox(Header header) {
		super(header);
	}

	public static UUIDBox createUUIDBox(UUID uuid, byte[] data) {

		UUIDBox box = new UUIDBox(Header.createHeader(FOURCC, 0));
		box.uuid = uuid;
		box.data = data;

		return box;

	}

	@Override
	public void parse(ByteBuffer buf) {
		parseUUID(buf);
		parseData(buf);
	}

	public void parseUUID(ByteBuffer buf) {

		long mostSignificantBits = buf.getLong();
		long leastSignificantBits = buf.getLong();

		uuid = new UUID(mostSignificantBits, leastSignificantBits);

	}

	public void parseData(ByteBuffer buf) {
		data = NIOUtils.toArray(NIOUtils.readBuf(buf));
	}

	public UUID getUUID() {
		return uuid;
	}

	public byte[] getData() {
		return data;
	}

	@Override
	protected void doWrite(ByteBuffer out) {
		out.putLong(uuid.getMostSignificantBits());
		out.putLong(uuid.getLeastSignificantBits());
		out.put(data);
	}

	@Override
	public int estimateSize() {
		return 8 + UUID_SIZE + data.length;
	}

	public static String fourcc() {
		return FOURCC;
	}

}
