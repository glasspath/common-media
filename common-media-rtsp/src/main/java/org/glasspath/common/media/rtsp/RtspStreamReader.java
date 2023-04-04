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

import java.io.DataInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.glasspath.common.media.rtsp.RtpPacket.FirstRtpHeaderByte;
import org.glasspath.common.media.rtsp.RtpPacket.SecondRtpHeaderByte;

public abstract class RtspStreamReader {

	public boolean TODO_DEBUG = false;

	public static final int READ_INTERVAL = 1;
	public static final int READ_BUFFER_LENGTH = 1024 * 100;
	public static final int MESSAGE_BUFFER_LENGTH = 1024 * 100;
	public static final String CONTENT_LENGTH_KEY_LOWER_CASE = "content-length: ";
	public static final int MAX_CONTENT_LENGTH = MESSAGE_BUFFER_LENGTH - 1000;

	public static enum RtspParserState {
		WAIT_FOR_HEADER_CR1,
		WAIT_FOR_HEADER_LF1,
		WAIT_FOR_HEADER_CR2,
		WAIT_FOR_HEADER_LF2,
		WAIT_FOR_CONTENT
	}

	public static enum RtpParserState {
		WAIT_FOR_RTSP_MAGIC_BYTE,
		WAIT_FOR_RTSP_CHANNEL,
		WAIT_FOR_RTSP_LENGTH,
		WAIT_FOR_FIRST_HEADER_BYTE,
		WAIT_FOR_SECOND_HEADER_BYTE,
		WAIT_FOR_SEQUENCE_NUMBER,
		WAIT_FOR_END_OF_PACKET
	}

	private final DataInputStream dataInputStream;
	private final byte[] readBuffer = new byte[READ_BUFFER_LENGTH];
	private final byte[] messageBuffer = new byte[MESSAGE_BUFFER_LENGTH];

	private int readLength = 0;
	private int readIndex = 0;
	private int messageIndex = 0;

	private boolean rtspParserEnabled = true;
	private RtspParserState rtspParserState = RtspParserState.WAIT_FOR_HEADER_CR1;
	private int lastLineStartIndex = 0;
	private int rtspContentLength = 0;
	private int rtspContentByteCount = 0;

	private boolean rtpParserEnabled = true;
	private RtpParserState rtpParserState = RtpParserState.WAIT_FOR_RTSP_MAGIC_BYTE;
	private RtspInterleavedFrame rtspFrame = null;
	private int rtspFrameStartIndex = -1;
	private byte rtspFrameLengthByte0 = 0;
	private byte rtspFrameLengthByte1 = 0;
	private final ByteReader rtpHeaderByteReader = new ByteReader();
	private FirstRtpHeaderByte firstRtpHeaderByte = null;
	private SecondRtpHeaderByte secondRtpHeaderByte = null;
	private byte rtpSequenceNumberByte0 = 0;
	private byte rtpSequenceNumberByte1 = 0;
	private Integer rtpSequenceNumber = null;

	private boolean exit = false;
	private boolean exited = false;

	private boolean printDebugReceivedBytes = false;
	private byte[] debugReceivedBytes = new byte[20];
	private int debugReceivedBytesIndex = 0;

	private byte b;

	public boolean tempLengthFix = false; // TODO!

	public RtspStreamReader(DataInputStream dataInputStream) {
		this.dataInputStream = dataInputStream;
	}

	public boolean isRtspParserEnabled() {
		return rtspParserEnabled;
	}

	public void setRtspParserEnabled(boolean rtspParserEnabled) {
		this.rtspParserEnabled = rtspParserEnabled;
	}

	public boolean isRtpParserEnabled() {
		return rtpParserEnabled;
	}

	public void setRtpParserEnabled(boolean rtpParserEnabled) {
		this.rtpParserEnabled = rtpParserEnabled;
	}

	public void start() {

		new Thread(new Runnable() {

			// int count = 0;

			@Override
			public void run() {

				while (!exit) {

					try {

						// while (dataInputStream.available() > 0) {
						// readByte(dataInputStream.readByte());
						// }

						readLength = dataInputStream.read(readBuffer, 0, readBuffer.length);
						readIndex = 0;

						// System.out.println("Parsing " + readLength + " bytes, exit = " + exit);
						parseReceivedBytes();

					} catch (Exception e) {
						e.printStackTrace();
					}

					try {
						Thread.sleep(READ_INTERVAL);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					/*
					count++;
					if (count > 10) {
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						count = 0;
					}
					 */

				}

				if (TODO_DEBUG) {
					System.out.println("RTSP Stream Reader exited");
				}
				exited = true;

			}
		}).start();

	}

	protected void parseReceivedBytes() {

		while (readIndex < readLength) {

			if (messageIndex >= messageBuffer.length) {
				reset();
			}

			b = readBuffer[readIndex];
			readIndex++;

			messageBuffer[messageIndex] = b;
			messageIndex++;

			// System.out.print((char)b);

			if (rtspParserEnabled) {

				switch (rtspParserState) {

				case WAIT_FOR_HEADER_CR1:

					if ((char) b == '\r') {
						rtspParserState = RtspParserState.WAIT_FOR_HEADER_LF1;
					}
					break;

				case WAIT_FOR_HEADER_LF1:

					if ((char) b == '\n') {

						rtspParserState = RtspParserState.WAIT_FOR_HEADER_CR2;

						try {

							String line = new String(messageBuffer, lastLineStartIndex, (messageIndex - lastLineStartIndex), "UTF-8");
							line = line.toLowerCase();
							if (line.startsWith(CONTENT_LENGTH_KEY_LOWER_CASE)) {
								rtspContentLength = Integer.parseInt(line.substring(CONTENT_LENGTH_KEY_LOWER_CASE.length(), line.length() - 2));
							}

						} catch (Exception e) {
							e.printStackTrace();
						}

						lastLineStartIndex = messageIndex;

					}
					break;

				case WAIT_FOR_HEADER_CR2:

					if ((char) b == '\r') {
						rtspParserState = RtspParserState.WAIT_FOR_HEADER_LF2;
					} else {
						rtspParserState = RtspParserState.WAIT_FOR_HEADER_LF1;
					}
					break;

				case WAIT_FOR_HEADER_LF2:

					if ((char) b == '\n') {

						if (rtspContentLength < 0 || rtspContentLength > MAX_CONTENT_LENGTH) {
							if (TODO_DEBUG) {
								System.err.println("Invalid content length: " + rtspContentLength);
							}
							rtspContentLength = 0;
						}

						if (rtspContentLength == 0) {

							rtspParserState = RtspParserState.WAIT_FOR_HEADER_CR1;

							try {
								if (rtspMessageReceived(new String(messageBuffer, 0, messageIndex - 1, "UTF-8"))) {
									reset();
								}
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}

							lastLineStartIndex = messageIndex;

						} else {
							rtspContentByteCount = 0;
							rtspParserState = RtspParserState.WAIT_FOR_CONTENT;
						}

					}
					break;

				case WAIT_FOR_CONTENT:

					rtspContentByteCount++;
					// System.out.println(rtspContentByteCount + " ----->>>> " + rtspContentLength);
					if (rtspContentByteCount >= rtspContentLength) {

						rtspParserState = RtspParserState.WAIT_FOR_HEADER_CR1;

						try {
							if (rtspMessageReceived(new String(messageBuffer, 0, messageIndex - 1, "UTF-8"))) {
								reset();
							}
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}

						lastLineStartIndex = messageIndex;

					}
					break;

				default:
					break;
				}

			}

			if (rtpParserEnabled) {

				switch (rtpParserState) {

				case WAIT_FOR_RTSP_MAGIC_BYTE:
					if (b == RtspInterleavedFrame.MAGIC_BYTE) {
						rtspFrame = new RtspInterleavedFrame(new RtpPacket());
						firstRtpHeaderByte = rtspFrame.getRtpPacket().getFirstRtpHeaderByte();
						secondRtpHeaderByte = rtspFrame.getRtpPacket().getSecondRtpHeaderByte();
						rtspFrameStartIndex = messageIndex - 1;
						rtpParserState = RtpParserState.WAIT_FOR_RTSP_CHANNEL;
						// } else {
						// System.err.println("Not the magic byte!");
					}
					break;

				case WAIT_FOR_RTSP_CHANNEL:
					rtspFrame.setChannel(b);
					rtpParserState = RtpParserState.WAIT_FOR_RTSP_LENGTH;
					break;

				case WAIT_FOR_RTSP_LENGTH:
					if (messageIndex == rtspFrameStartIndex + 3) {
						rtspFrameLengthByte0 = b;
					} else if (messageIndex == rtspFrameStartIndex + 4) {
						rtspFrameLengthByte1 = b;
						if (tempLengthFix) {
							rtspFrame.setLength(RtpPacket.toUInt16(rtspFrameLengthByte1, rtspFrameLengthByte0));
						} else {
							rtspFrame.setLength(RtpPacket.toUInt16(rtspFrameLengthByte0, rtspFrameLengthByte1));
						}
						if (rtspFrame.getLength() >= 0 && rtspFrame.getLength() < messageBuffer.length - rtspFrameStartIndex) {
							rtpParserState = RtpParserState.WAIT_FOR_FIRST_HEADER_BYTE;
						} else {
							if (TODO_DEBUG) {
								System.err.println("Invalid RTSP interleaved frame length: " + rtspFrame.getLength());
							}
							rtpParserState = RtpParserState.WAIT_FOR_RTSP_MAGIC_BYTE;
						}
					}
					break;

				case WAIT_FOR_FIRST_HEADER_BYTE:
					rtpHeaderByteReader.setCurrentByte(b);
					firstRtpHeaderByte.parse(rtpHeaderByteReader);
					if (firstRtpHeaderByte.version == RtpPacket.VERSION) {
						rtpParserState = RtpParserState.WAIT_FOR_SECOND_HEADER_BYTE;
					} else {
						if (TODO_DEBUG) {
							System.err.println("Invalid RTP packet version: " + firstRtpHeaderByte.version);
						}
						rtpParserState = RtpParserState.WAIT_FOR_END_OF_PACKET;
					}
					break;

				case WAIT_FOR_SECOND_HEADER_BYTE:
					rtpHeaderByteReader.setCurrentByte(b);
					secondRtpHeaderByte.parse(rtpHeaderByteReader);
					if (secondRtpHeaderByte.payloadType == RtpPacket.PayloadType.TODO_DEFAULT.getTypeValue()) {
						rtpParserState = RtpParserState.WAIT_FOR_SEQUENCE_NUMBER;
					} else {
						if (TODO_DEBUG) {
							System.err.println("Invalid RTP packet payload type: " + secondRtpHeaderByte.payloadType);
						}
						rtpParserState = RtpParserState.WAIT_FOR_END_OF_PACKET;
					}
					break;

				case WAIT_FOR_SEQUENCE_NUMBER:
					if (messageIndex == rtspFrameStartIndex + RtspInterleavedFrame.HEADER_LENGTH + 3) {
						rtpSequenceNumberByte0 = b;
					} else if (messageIndex == rtspFrameStartIndex + RtspInterleavedFrame.HEADER_LENGTH + 4) {

						rtpSequenceNumberByte1 = b;
						int sequenceNumber = RtpPacket.toUInt16(rtpSequenceNumberByte0, rtpSequenceNumberByte1);

						if (rtpSequenceNumber == null || (sequenceNumber > rtpSequenceNumber && sequenceNumber < rtpSequenceNumber + 25)) {
							rtpSequenceNumber = sequenceNumber;
							// System.out.println("RTP Packet header detected, payload type: " + secondRtpHeaderByte.payloadType + ", sequence number: " + sequenceNumber + ", length: " + rtspFrame.getLength());
						}

						rtpParserState = RtpParserState.WAIT_FOR_END_OF_PACKET;

					}
					break;

				case WAIT_FOR_END_OF_PACKET:
					if (messageIndex >= rtspFrameStartIndex + RtspInterleavedFrame.HEADER_LENGTH + rtspFrame.getLength()) {

						if (secondRtpHeaderByte.payloadType == RtpPacket.PayloadType.TODO_DEFAULT.getTypeValue()) {

							int from = rtspFrameStartIndex + RtspInterleavedFrame.HEADER_LENGTH;
							int to = from + rtspFrame.getLength();
							rtspFrame.getRtpPacket().parseBytes(Arrays.copyOfRange(messageBuffer, from, to));

							if (rtspInterleavedFrameReceived(rtspFrame)) {
								reset();
							}

						}

						rtpParserState = RtpParserState.WAIT_FOR_RTSP_MAGIC_BYTE;
						rtspFrame = null;
						rtspFrameStartIndex = -1;

					}
					break;

				default:
					break;
				}

				// debugReceivedByte(b);

			}

		}

	}

	private void reset() {

		rtspParserState = RtspParserState.WAIT_FOR_HEADER_CR1;
		lastLineStartIndex = 0;
		rtspContentLength = 0;

		rtpParserState = RtpParserState.WAIT_FOR_RTSP_MAGIC_BYTE;
		rtspFrame = null;
		rtspFrameStartIndex = -1;

		messageIndex = 0;

	}

	protected void debugReceivedByte(byte b) {

		if (printDebugReceivedBytes) {

			if (debugReceivedBytesIndex < debugReceivedBytes.length) {

				debugReceivedBytes[debugReceivedBytesIndex] = b;

				debugReceivedBytesIndex++;
				if (debugReceivedBytesIndex >= debugReceivedBytes.length) {

					for (byte debugByte : debugReceivedBytes) {
						System.out.print(String.format("%02X ", debugByte) + " ");
					}
					System.out.println("");

					printDebugReceivedBytes = false;

				}

			}

		} else {
			debugReceivedBytesIndex = 0;
		}

	}

	public void exit() {
		exit = true;
	}

	public boolean isExited() {
		return exited;
	}

	public abstract boolean rtspMessageReceived(String message);

	public abstract boolean rtspInterleavedFrameReceived(RtspInterleavedFrame rtspInterleavedFrame);

}
