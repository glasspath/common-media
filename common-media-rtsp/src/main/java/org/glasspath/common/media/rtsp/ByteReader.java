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

public class ByteReader {

	private byte[] bytes = new byte[0];
	private int index = 0;
	private int bitIndex = 0;
	private int currentByte = 0;
	private int nextByte = 0;

	public ByteReader() {

	}

	public ByteReader(byte b) {
		currentByte = b;
	}

	public ByteReader(byte[] bytes) {
		this(bytes, 0);
	}

	public ByteReader(byte[] bytes, int index) {
		setBytes(bytes);
		setIndex(index);
	}

	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
		bitIndex = 0;
	}

	public void setCurrentByte(int currentByte) {
		this.currentByte = currentByte;
		bitIndex = 0;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {

		this.index = index;

		currentByte = bytes[index];
		if (index + 1 < bytes.length) {
			nextByte = bytes[index + 1];
		}

	}

	public int getBitIndex() {
		return bitIndex;
	}

	public void setBitIndex(int bitIndex) {
		this.bitIndex = bitIndex;
	}

	private void next() {
		currentByte = nextByte;
		nextByte = bytes[++index];
		bitIndex = 0;
	}

	public boolean readBool() {
		return read1Bit() == 1;
	}

	public int read1Bit() {

		if (bitIndex == 8) {
			next();
		}

		int value = (currentByte >> (7 - bitIndex)) & 1;

		bitIndex++;

		return value;

	}

	public long readNBit(int n) {

		long value = 0;

		for (int i = 0; i < n; i++) {
			value <<= 1;
			value |= read1Bit();
		}

		return value;

	}

	public int readByte() {

		if (bitIndex > 0) {
			next();
		}

		int value = currentByte;

		next();

		return value;

	}

	public long readRemainingByte() {
		return readNBit(8 - bitIndex);
	}

}
