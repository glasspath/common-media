/*
 * This file is part of Glasspath Common.
 * Copyright (C) 2011 - 2025 Remco Poelstra
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

public abstract class AacAdtsWriter extends AccessUnitWriter {

	public static final int AAC_PROFILE_LOW_COMPLEXITY = 1;
	public static final int[] DEFAULT_SAMPLE_RATES = { 96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000, 11025, 8000, 7350 };

	private long timestamp = 0;

	// TODO: Temporary test for audio recorded from Axis camera (it seems we have to use 16000 even though the camera uses 8000)
	private byte[] adtsHeader = generateADTSHeader(AAC_PROFILE_LOW_COMPLEXITY, getAACSampleRateIndex(16000), 1, 1024); // TODO

	public AacAdtsWriter() {

	}

	@Override
	public void rtpPacketReceived(RtpPacket rtpPacket) {

		timestamp = rtpPacket.getTimestamp();

		// int sizelength = 13;
		int indexlength = 3;
		// int indexdeltalength = 3;

		int offset = rtpPacket.getHeaderLength();

		byte[] auData = new byte[rtpPacket.getBytes().length - offset];
		System.arraycopy(rtpPacket.getBytes(), offset, auData, 0, auData.length);

		int auHeaderLength = (auData[0] << 8) | auData[1];
		if (auHeaderLength == 16) { // TODO!

			int auHeaderEntry = (auData[2] << 8) | (auData[3] & 0xFF);

			int auEntrySize = auHeaderEntry >> indexlength;
			int index = 4; // TODO!
			if (auEntrySize <= auData.length - index) {

				// System.out.println("Writing aac frame, auEntrySize = " + auEntrySize);
				write(adtsHeader, 0, adtsHeader.length);
				write(auData, index, auEntrySize);
				accessUnitWritten(0, timestamp); 

			} else {
				System.err.println("Invalid auEntrySize: " + auEntrySize);
			}

		} else {
			System.err.println("TODO: Parse multiple aac frames from rtp packet, auHeaderLength = " + auHeaderLength);
		}

	}

	// TODO: Generate by ChatGPT, check if this is correct..
	public static byte[] generateADTSHeader(int aacProfile, int sampleRateIndex, int channelConfig, int frameLength) {

		byte[] adtsHeader = new byte[7];

		int fullFrameLength = frameLength + 7; // ADTS header (7 bytes) + AAC payload

		// Syncword (0xFFF)
		adtsHeader[0] = (byte) 0xFF;
		adtsHeader[1] = (byte) 0xF1; // MPEG-4, Layer 0, Protection Absent

		// Profile (AAC-LC = 1)
		adtsHeader[2] = (byte) ((aacProfile << 6) | (sampleRateIndex << 2) | (channelConfig >> 2));

		// Channel Configuration (lower 2 bits) + Frame Length (first 2 bits)
		adtsHeader[3] = (byte) (((channelConfig & 3) << 6) | (fullFrameLength >> 11));

		// Frame Length (middle 8 bits)
		adtsHeader[4] = (byte) ((fullFrameLength >> 3) & 0xFF);

		// Frame Length (last 3 bits) + Buffer fullness (0x7FF for variable bit rate)
		adtsHeader[5] = (byte) (((fullFrameLength & 7) << 5) | 0x1F);

		// Buffer fullness (last 6 bits) + Number of AAC frames (always 1)
		adtsHeader[6] = (byte) 0xFC;

		return adtsHeader;

	}

	public static int getAACSampleRateIndex(int sampleRate) {

		for (int i = 0; i < DEFAULT_SAMPLE_RATES.length; i++) {
			if (DEFAULT_SAMPLE_RATES[i] == sampleRate) {
				return i;
			}
		}

		return 4;

	}

}
