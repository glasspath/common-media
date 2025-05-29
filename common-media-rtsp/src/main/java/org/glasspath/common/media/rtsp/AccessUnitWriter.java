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

import java.io.IOException;
import java.io.OutputStream;

public abstract class AccessUnitWriter {

	private OutputStream outputStream = null;

	public AccessUnitWriter() {

	}

	public OutputStream getOutputStream() {
		return outputStream;
	}

	public void setOutputStream(OutputStream outputStream) {
		this.outputStream = outputStream;
	}

	public abstract void rtpPacketReceived(RtpPacket rtpPacket);

	protected void write(byte b) {
		if (outputStream != null) {
			try {
				outputStream.write(b);
			} catch (IOException e) {
				e.printStackTrace(); // TODO
			}
		}
	}

	protected void write(byte[] bytes) {
		if (outputStream != null) {
			try {
				outputStream.write(bytes);
			} catch (IOException e) {
				e.printStackTrace(); // TODO
			}
		}
	}

	protected void write(byte[] bytes, int offset, int length) {
		if (outputStream != null) {
			try {
				outputStream.write(bytes, offset, length);
			} catch (IOException e) {
				e.printStackTrace(); // TODO
			}
		}
	}

	public abstract void accessUnitWritten(int unitType, long timestamp);

}
