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

import java.nio.ByteBuffer;

import org.glasspath.common.media.h264.H264NalUnit;
import org.glasspath.common.media.h264.H264NalUnit.NalFragmentType;

public class RtpPacket {

	public static enum PayloadType {

		UNKNOWN(-1),
		// TODO
		TODO_DEFAULT(96)
		// TODO
		;

		private final int typeValue;

		private PayloadType(int typeValue) {
			this.typeValue = typeValue;
		}

		public int getTypeValue() {
			return typeValue;
		}

	}

	public static final int VERSION = 2;
	public static final int DEFAULT_HEADER_LENGTH = 12;

	private byte[] bytes = null;
	private final FirstRtpHeaderByte firstRtpHeaderByte = new FirstRtpHeaderByte();
	private final SecondRtpHeaderByte secondRtpHeaderByte = new SecondRtpHeaderByte();
	private int sequenceNumber = 0;
	private long timestamp = 0;
	private long ssrc = 0;
	private long[] csrcList = new long[0];
	private int extentionCustomField = 0;
	private int extentionLength = 0;
	private int headerLength = DEFAULT_HEADER_LENGTH;

	private byte nalFBits;
	private byte nalNriBits;
	private byte nalType;
	private boolean fuStart = false;
	private boolean fuEnd = false;
	private byte fuNalType;
	private NalFragmentType nalFragmentType = NalFragmentType.UNKNOWN;

	public RtpPacket() {

	}

	public RtpPacket(byte[] bytes) {
		parseBytes(bytes);
	}

	public void parseBytes(byte[] bytes) {

		if (bytes != null && bytes.length >= DEFAULT_HEADER_LENGTH) {

			this.bytes = bytes;

			headerLength = DEFAULT_HEADER_LENGTH;

			ByteReader reader = new ByteReader(bytes[0]);
			firstRtpHeaderByte.parse(reader);

			reader.setCurrentByte(bytes[1]);
			secondRtpHeaderByte.parse(reader);

			sequenceNumber = toUInt16(bytes[2], bytes[3]);

			ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, 4, bytes.length - 4);
			timestamp = toUInt32(byteBuffer.getInt());
			ssrc = toUInt32(byteBuffer.getInt());

			if (firstRtpHeaderByte.csrcCount > 0) {
				csrcList = new long[firstRtpHeaderByte.csrcCount];
				for (int i = 0; i < csrcList.length; i++) {
					csrcList[i] = toUInt32(byteBuffer.getInt());
				}
				headerLength += 4 * firstRtpHeaderByte.csrcCount;
			}

			byte nalUnitOctet = byteBuffer.get();
			nalFBits = (byte) (nalUnitOctet & 0x80);
			nalNriBits = (byte) (nalUnitOctet & 0x60);
			nalType = (byte) (nalUnitOctet & 0x1F);

			if (nalType >= NalFragmentType.NAL_UNIT.getValueFrom() && nalType <= NalFragmentType.NAL_UNIT.getValueTo()) {
				nalFragmentType = NalFragmentType.NAL_UNIT;
				/*
				if (nalType == 7) {
					System.out.println("SPS: " + nalUnitOctet);
				} else if (nalType == 8) {
					System.out.println("PPS: " + nalUnitOctet);
				}
				 */
			} else if (nalType == NalFragmentType.FU_A.getValueFrom()) {
				nalFragmentType = NalFragmentType.FU_A;
			} else if (nalType == NalFragmentType.STAP_A.getValueFrom()) {
				nalFragmentType = NalFragmentType.STAP_A;
			}

			byte fuHeader = byteBuffer.get();
			fuStart = ((fuHeader & 0x80) != 0);
			fuEnd = ((fuHeader & 0x40) != 0);
			fuNalType = (byte) (fuHeader & 0x1F);

			// System.out.println(this);

			if (firstRtpHeaderByte.extension) {
				extentionCustomField = readUInt16(byteBuffer);
				extentionLength = readUInt16(byteBuffer);
				headerLength += 4 + extentionLength; // TODO: Is this correct?
			}

		}

	}

	public static byte[] parseNalUnit(H264NalUnit nalUnit) {

		byte[] bytes = new byte[(nalUnit.bytes.length - 4) + 12];

		int padding = 0, extension = 0, cc = 0, marker = 0, sequenceNumber = 0, ssrc = 0;

		// fill the header array of byte with RTP header fields
		bytes[0] = (byte) (VERSION << 6 | padding << 5 | extension << 4 | cc);
		bytes[1] = (byte) (marker << 7 | PayloadType.TODO_DEFAULT.typeValue & 0x000000FF);
		bytes[2] = (byte) (sequenceNumber >> 8);
		bytes[3] = (byte) (sequenceNumber & 0xFF);
		bytes[4] = (byte) (nalUnit.timestamp >> 24);
		bytes[5] = (byte) (nalUnit.timestamp >> 16);
		bytes[6] = (byte) (nalUnit.timestamp >> 8);
		bytes[7] = (byte) (nalUnit.timestamp & 0xFF);
		bytes[8] = (byte) (ssrc >> 24);
		bytes[9] = (byte) (ssrc >> 16);
		bytes[10] = (byte) (ssrc >> 8);
		bytes[11] = (byte) (ssrc & 0xFF);

		System.arraycopy(nalUnit.bytes, 4, bytes, 12, nalUnit.bytes.length - 4);

		return bytes;

	}

	public byte[] getBytes() {
		return bytes;
	}

	public ByteBuffer getPayload(int offset) {
		offset += headerLength;
		return ByteBuffer.wrap(bytes, offset, bytes.length - offset);
	}

	public FirstRtpHeaderByte getFirstRtpHeaderByte() {
		return firstRtpHeaderByte;
	}

	public SecondRtpHeaderByte getSecondRtpHeaderByte() {
		return secondRtpHeaderByte;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public long getSsrc() {
		return ssrc;
	}

	public long[] getCsrcList() {
		return csrcList;
	}

	public int getExtentionCustomField() {
		return extentionCustomField;
	}

	public int getExtentionLength() {
		return extentionLength;
	}

	public int getHeaderLength() {
		return headerLength;
	}

	public byte getNalType() {
		return nalType;
	}

	public NalFragmentType getNalFragmentType() {
		return nalFragmentType;
	}

	public byte getNalTypeOctet() {
		return (byte) (fuNalType | nalFBits | nalNriBits);
	}

	public boolean isStart() {
		return fuStart;
	}

	public boolean isEnd() {
		return fuEnd;
	}

	@Override
	public String toString() {
		return "RTP"
				+ " v: " + firstRtpHeaderByte.version
				+ " p: " + (firstRtpHeaderByte.padding ? 1 : 0)
				+ " x: " + (firstRtpHeaderByte.extension ? 1 : 0)
				+ " csrcCount: " + firstRtpHeaderByte.csrcCount
				+ " m: " + (secondRtpHeaderByte.marker ? 1 : 0)
				+ " pt: " + secondRtpHeaderByte.payloadType
				+ " seq: " + sequenceNumber
				+ " t: " + timestamp
				+ " ssrc: " + ssrc
				+ " hl: " + headerLength
				+ " type: " + nalFragmentType
				+ " nal1: " + nalType
				+ " nal2: " + fuNalType
				+ " start: " + (fuStart ? 1 : 0)
				+ " end: " + (fuEnd ? 1 : 0);
	}

	public static class FirstRtpHeaderByte {

		public int version = 0;
		public boolean padding = false;
		public boolean extension = false;
		public int csrcCount = 0;

		public FirstRtpHeaderByte() {

		}

		public FirstRtpHeaderByte(byte b) {
			parse(b);
		}

		public void parse(Byte b) {
			parse(new ByteReader(b));
		}

		public void parse(ByteReader reader) {
			version = (int) reader.readNBit(2);
			padding = reader.readBool();
			extension = reader.readBool();
			csrcCount = (int) reader.readNBit(4);
		}

	}

	public static class SecondRtpHeaderByte {

		public boolean marker = false;
		public int payloadType = 0;

		public SecondRtpHeaderByte() {

		}

		public SecondRtpHeaderByte(byte b) {
			parse(b);
		}

		public void parse(Byte b) {
			parse(new ByteReader(b));
		}

		public void parse(ByteReader reader) {
			marker = reader.readBool();
			payloadType = (int) reader.readNBit(7);
		}

	}

	public static int toInt(byte b) {
		return b < 0 ? b + 256 : b;
	}

	public static int readUInt16(ByteBuffer byteBuffer) {
		int result = 0;
		result += toInt(byteBuffer.get()) << 8;
		result += toInt(byteBuffer.get());
		return result;
	}

	public static int toUInt16(byte b0, byte b1) {
		int result = 0;
		result += toInt(b0) << 8;
		result += toInt(b1);
		return result;
	}

	public static long toUInt32(int i) {
		long value = i;
		if (value < 0) {
			value += 1L << 32;
		}
		return value;
	}

}
