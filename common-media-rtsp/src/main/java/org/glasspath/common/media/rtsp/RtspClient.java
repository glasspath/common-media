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

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.glasspath.common.Common;
import org.glasspath.common.media.rtsp.RtspDescribeResponseParser.AudioTrackInfo;
import org.glasspath.common.media.rtsp.RtspDescribeResponseParser.TrackInfo;
import org.glasspath.common.media.rtsp.RtspDescribeResponseParser.VideoTrackInfo;
import org.glasspath.common.media.rtsp.RtspRequestBuilder.RtspRequest;

public class RtspClient {

	public static boolean TODO_DEBUG = false;
	
	public static final String USER_AGENT = "Lavf58.29.100";
	public static int CONNECT_TIMEOUT = 2000;

	private RtspUrl rtspUrl = null;
	private boolean disconnected = true;
	private Socket socket = null;
	private DataInputStream dataInputStream = null;
	private BufferedWriter bufferedWriter = null;
	private RtspStreamReader streamReader = null;
	private RtspResponseParser rtspResponseParser = null;
	private int cSeq = 0;
	private String session = null;
	private String[] options = null;
	private VideoTrackInfo videoTrackInfo = null;
	private AudioTrackInfo audioTrackInfo = null;
	private List<RtspInterleavedFrame> frames = new ArrayList<>();

	public RtspClient() {

	}

	public RtspClient(RtspUrl rtspUrl) {
		this.rtspUrl = rtspUrl;
	}

	public RtspUrl getRtspUrl() {
		return rtspUrl;
	}

	public void setRtspUrl(RtspUrl rtspUrl) {
		this.rtspUrl = rtspUrl;
	}

	public boolean connect() {

		if (rtspUrl != null) {

			try {

				socket = new Socket();
				socket.connect(new InetSocketAddress(rtspUrl.getHost(), rtspUrl.getPort()), CONNECT_TIMEOUT);
				dataInputStream = new DataInputStream(socket.getInputStream());
				bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

				createRtspStreamReader();

				disconnected = false;

				return true;

			} catch (UnknownHostException e) {
				if (TODO_DEBUG) {
					Common.LOGGER.debug("Exception while connecting", e);
				}
			} catch (IOException e) {
				if (TODO_DEBUG) {
					Common.LOGGER.debug("Exception while connecting", e);
				}
			}

		}

		return false;

	}

	private void createRtspStreamReader() {

		if (streamReader != null) {
			streamReader.exit();
		}

		streamReader = new RtspStreamReader(dataInputStream) {

			@Override
			public boolean rtspMessageReceived(String message) {

				if (TODO_DEBUG) {
					System.out.println("Response:");
					System.out.println(message);
				}

				RtspResponseParser parser = getRtspResponseParser();
				if (parser != null) {
					parser.parseMessage(message);
				}

				return true;

			}

			@Override
			public boolean rtspInterleavedFrameReceived(RtspInterleavedFrame rtspInterleavedFrame) {

				if (frames.size() < 100) {
					frames.add(rtspInterleavedFrame);
				}

				rtpPacketReceived(rtspInterleavedFrame.getRtpPacket());

				return true;

			}
		};

		streamReader.setRtspParserEnabled(true);
		streamReader.setRtpParserEnabled(false);
		streamReader.start();

	}

	public void rtpPacketReceived(RtpPacket rtpPacket) {

	}

	public void disconnect() {

		if (streamReader != null) {

			streamReader.exit();

			int count = 0;
			while (!streamReader.isExited() && count < 100) {

				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					if (TODO_DEBUG) {
						Common.LOGGER.debug("Exception while disconnecting", e);
					}
				}

				count++;

			}

			streamReader = null;

		}

		if (socket != null) {

			try {
				socket.close();
			} catch (IOException e) {
				if (TODO_DEBUG) {
					Common.LOGGER.debug("Exception while disconnecting", e);
				}
			}

			socket = null;

		}

		disconnected = true;

	}

	public boolean isDisconnected() {
		return disconnected;
	}

	private synchronized RtspResponseParser getRtspResponseParser() {
		return rtspResponseParser;
	}

	private synchronized void setRtspResponseParser(RtspResponseParser rtspResponseParser) {
		this.rtspResponseParser = rtspResponseParser;
	}

	private void waitForRtspResponseParserCompletion(RtspResponseParser parser) {
		setRtspResponseParser(parser);
		parser.waitForCompletion();
		setRtspResponseParser(null);
	}

	private void updateSession(RtspResponseParser parser) {
		if (session == null && parser.getSession() != null) {
			session = parser.getSession();
		}
	}

	public void sendOptionsRequest() {

		if (rtspUrl != null) {

			// TODO?
			session = null;

			write(new RtspRequestBuilder(rtspUrl)
					.begin(RtspRequest.OPTIONS)
					.appendCSeq(++cSeq)
					.appendUserAgent(USER_AGENT)
					.appendBasicAuthentication()
					.appendSession(session)
					.end()
					.getRequest());

			RtspOptionsResponseParser parser = new RtspOptionsResponseParser();
			waitForRtspResponseParserCompletion(parser);

			updateSession(parser);
			options = parser.getOptions();

		}

	}

	public String[] getOptions() {
		return options;
	}

	public void sendDescribeRequest() {

		if (rtspUrl != null) {

			write(new RtspRequestBuilder(rtspUrl)
					.begin(RtspRequest.DESCRIBE)
					.appendCSeq(++cSeq)
					.appendUserAgent(USER_AGENT)
					.appendBasicAuthentication()
					.appendSession(session)
					.end()
					.getRequest());

			RtspDescribeResponseParser parser = new RtspDescribeResponseParser();
			waitForRtspResponseParserCompletion(parser);

			updateSession(parser);
			videoTrackInfo = parser.getVideoTrackInfo();
			audioTrackInfo = parser.getAudioTrackInfo();

		}

	}

	public VideoTrackInfo getVideoTrackInfo() {
		return videoTrackInfo;
	}

	public AudioTrackInfo getAudioTrackInfo() {
		return audioTrackInfo;
	}

	public void sendSetupRequest(TrackInfo trackInfo, String transport) {

		if (rtspUrl != null) {

			String control;
			if (trackInfo.trackIdentifier.identifier != null && trackInfo.trackId >= 0) {
				control = trackInfo.trackIdentifier.identifier + trackInfo.trackId;
			} else {
				control = trackInfo.control;
			}

			boolean append;
			if (control != null && control.toLowerCase().startsWith("rtsp")) {
				append = false;
			} else {
				append = true;
			}

			write(new RtspRequestBuilder(rtspUrl)
					.begin(RtspRequest.SETUP, control, append)
					.appendTransport(transport)
					.appendCSeq(++cSeq)
					.appendUserAgent(USER_AGENT)
					.appendBasicAuthentication()
					.appendSession(session)
					.end()
					.getRequest());

			RtspSetupResponseParser parser = new RtspSetupResponseParser();
			waitForRtspResponseParserCompletion(parser);

			updateSession(parser);

		}

	}

	public boolean sendPlayRequest(String range) {

		if (rtspUrl != null) {

			write(new RtspRequestBuilder(rtspUrl)
					.begin(RtspRequest.PLAY)
					.appendCSeq(++cSeq)
					.appendUserAgent(USER_AGENT)
					.appendBasicAuthentication()
					.appendSession(session)
					.appendRange(range)
					.end()
					.getRequest());

			RtspPlayResponseParser parser = new RtspPlayResponseParser() {

				@Override
				public void parseMessage(String message) {
					super.parseMessage(message);

					streamReader.setRtspParserEnabled(false);
					streamReader.setRtpParserEnabled(true);

				}
			};
			waitForRtspResponseParserCompletion(parser);

			updateSession(parser);

			return parser.getReplyCode() == 200;

		} else {
			return false;
		}

	}

	public List<RtspInterleavedFrame> getFrames() {
		return frames;
	}

	public void sendTeardownRequest() {

		if (rtspUrl != null) {

			write(new RtspRequestBuilder(rtspUrl)
					.begin(RtspRequest.TEARDOWN)
					.appendCSeq(++cSeq)
					.appendUserAgent(USER_AGENT)
					.appendBasicAuthentication()
					.appendSession(session)
					.end()
					.getRequest());

			// No response is sent for TEARDOWN

		}

	}

	private void write(String message) {

		if (TODO_DEBUG) {
			System.out.println("Request:");
			System.out.println(message);
		}

		if (bufferedWriter != null) {
			try {
				bufferedWriter.write(message);
				bufferedWriter.flush();
			} catch (IOException e) {
				if (TODO_DEBUG) {
					Common.LOGGER.debug("Exception while writing message", e);
				}
			}
		}

	}

}
