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

import java.io.IOException;
import java.io.OutputStream;

public class H264AnnexBWriter {

	private OutputStream outputStream = null;
	private int nalUnitType = 0;
	private long timestamp = 0;

	public H264AnnexBWriter() {

	}

	public OutputStream getOutputStream() {
		return outputStream;
	}

	public void setOutputStream(OutputStream outputStream) {
		this.outputStream = outputStream;
	}

	public void rtpPacketReceived(RtpPacket rtpPacket) {

		switch (rtpPacket.getNalFragmentType()) {

		case NAL_UNIT:

			nalUnitType = rtpPacket.getNalType();
			timestamp = rtpPacket.getTimestamp();

			write(H264NalUnit.NAL_START_PREFIX_CODE);
			write(rtpPacket.getBytes(), rtpPacket.getHeaderLength(), rtpPacket.getBytes().length - rtpPacket.getHeaderLength());

			nalUnitWritten(nalUnitType, timestamp);

			break;

		case FU_A:

			if (rtpPacket.isStart()) {

				nalUnitType = rtpPacket.getNalTypeOctet();
				timestamp = rtpPacket.getTimestamp();

				write(H264NalUnit.NAL_START_PREFIX_CODE);
				write(rtpPacket.getNalTypeOctet());

			}

			write(rtpPacket.getBytes(), rtpPacket.getHeaderLength() + 2, rtpPacket.getBytes().length - (rtpPacket.getHeaderLength() + 2));

			if (rtpPacket.isEnd()) {
				nalUnitWritten(nalUnitType, timestamp);
			}

			break;

		/* TODO: This frame type includes a series of concatenated NAL units, each preceded by a 16-bit size field
		case STAP_A:
			break;
		*/

		default:
			System.err.println("NAL: Unimplemented unit type: " + rtpPacket.getNalFragmentType());
			break;

		}

	}

	private void write(byte b) {
		if (outputStream != null) {
			try {
				outputStream.write(b);
			} catch (IOException e) {
				e.printStackTrace(); // TODO
			}
		}
	}

	private void write(byte[] bytes) {
		if (outputStream != null) {
			try {
				outputStream.write(bytes);
			} catch (IOException e) {
				e.printStackTrace(); // TODO
			}
		}
	}

	private void write(byte[] bytes, int offset, int length) {
		if (outputStream != null) {
			try {
				outputStream.write(bytes, offset, length);
			} catch (IOException e) {
				e.printStackTrace(); // TODO
			}
		}
	}

	public void nalUnitWritten(int nalUnitType, long timestamp) {

	}

}
