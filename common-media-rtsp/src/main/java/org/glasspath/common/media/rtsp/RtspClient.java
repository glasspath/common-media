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

import org.glasspath.common.Common;
import org.glasspath.common.media.rtsp.RtspDescribeResponseParser.AudioTrackInfo;
import org.glasspath.common.media.rtsp.RtspDescribeResponseParser.TrackInfo;
import org.glasspath.common.media.rtsp.RtspDescribeResponseParser.VideoTrackInfo;
import org.glasspath.common.media.rtsp.RtspRequestBuilder.RtspRequest;
import org.glasspath.common.media.rtsp.RtspResponseParser.Authentication;

public class RtspClient {

	public static boolean TODO_DEBUG = true;

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
	private Authentication authentication = null;
	private String session = null;
	private String[] options = null;
	private VideoTrackInfo videoTrackInfo = null;
	private AudioTrackInfo audioTrackInfo = null;

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

				RtspResponseParser responseParser = rtspResponseParser;
				if (responseParser != null) {
					responseParser.parseMessage(message);
				}

				return true;

			}

			@Override
			public boolean rtspInterleavedFrameReceived(RtspInterleavedFrame rtspInterleavedFrame) {

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

	public int sendOptionsRequest() {

		if (rtspUrl != null) {

			// TODO?
			authentication = null;
			session = null;

			String request = new RtspRequestBuilder(rtspUrl)
					.begin(RtspRequest.OPTIONS)
					.appendCSeq(++cSeq)
					.appendUserAgent(USER_AGENT)
					.appendAuthorization(authentication)
					.appendSession(session)
					.end()
					.getRequest();

			RtspOptionsResponseParser responseParser = new RtspOptionsResponseParser();

			sendRequest(request, responseParser);

			options = responseParser.getOptions();

			return responseParser.getReplyCode();

		}

		return 0;

	}

	public String[] getOptions() {
		return options;
	}

	public int sendDescribeRequest() {

		if (rtspUrl != null) {

			String request = new RtspRequestBuilder(rtspUrl)
					.begin(RtspRequest.DESCRIBE)
					.appendCSeq(++cSeq)
					.appendUserAgent(USER_AGENT)
					.appendAuthorization(authentication)
					.appendSession(session)
					.end()
					.getRequest();

			RtspDescribeResponseParser responseParser = new RtspDescribeResponseParser();

			sendRequest(request, responseParser);

			videoTrackInfo = responseParser.getVideoTrackInfo();
			audioTrackInfo = responseParser.getAudioTrackInfo();

			return responseParser.getReplyCode();

		}

		return 0;

	}

	public Authentication getAuthentication() {
		return authentication;
	}

	public VideoTrackInfo getVideoTrackInfo() {
		return videoTrackInfo;
	}

	public AudioTrackInfo getAudioTrackInfo() {
		return audioTrackInfo;
	}

	public int sendSetupRequest(TrackInfo trackInfo, String transport) {

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

			String request = new RtspRequestBuilder(rtspUrl)
					.begin(RtspRequest.SETUP, control, append)
					.appendTransport(transport)
					.appendCSeq(++cSeq)
					.appendUserAgent(USER_AGENT)
					.appendAuthorization(authentication)
					.appendSession(session)
					.end()
					.getRequest();

			RtspSetupResponseParser responseParser = new RtspSetupResponseParser();

			sendRequest(request, responseParser);

			return responseParser.getReplyCode();

		}

		return 0;

	}

	public boolean sendPlayRequest() {
		return sendPlayRequest("npt=0.000-");
	}

	public boolean sendPlayRequest(String range) {

		if (rtspUrl != null) {

			String request = new RtspRequestBuilder(rtspUrl)
					.begin(RtspRequest.PLAY)
					.appendCSeq(++cSeq)
					.appendUserAgent(USER_AGENT)
					.appendAuthorization(authentication)
					.appendSession(session)
					.appendRange(range)
					.end()
					.getRequest();

			RtspPlayResponseParser responseParser = new RtspPlayResponseParser() {

				@Override
				public void parseMessage(String message) {
					super.parseMessage(message);

					// TODO? Here we switch from parsing of RTSP messages to parsing of RTP packets
					streamReader.setRtspParserEnabled(false);
					streamReader.setRtpParserEnabled(true);

				}
			};

			sendRequest(request, responseParser);

			return responseParser.getReplyCode() == 200;

		} else {
			return false;
		}

	}

	public void sendTeardownRequest() {

		if (rtspUrl != null) {

			String request = new RtspRequestBuilder(rtspUrl)
					.begin(RtspRequest.TEARDOWN)
					.appendCSeq(++cSeq)
					.appendUserAgent(USER_AGENT)
					.appendAuthorization(authentication)
					.appendSession(session)
					.end()
					.getRequest();

			// No response is sent for TEARDOWN
			sendRequest(request, null);

		}

	}

	private void sendRequest(String request, RtspResponseParser responseParser) {

		if (TODO_DEBUG) {
			System.out.println("Request:");
			System.out.println(request);
		}

		if (bufferedWriter != null) {

			try {

				rtspResponseParser = responseParser;

				bufferedWriter.write(request);
				bufferedWriter.flush();

				if (rtspResponseParser != null) {

					rtspResponseParser.waitForCompletion();

					if (rtspResponseParser.getReplyCode() == 401 && rtspResponseParser.getDigestAuthentication() != null) {
						authentication = rtspResponseParser.getDigestAuthentication();
					}

					if (session == null && rtspResponseParser.getSession() != null) {
						session = rtspResponseParser.getSession();
					}

					rtspResponseParser = null;

				}

			} catch (IOException e) {
				if (TODO_DEBUG) {
					Common.LOGGER.debug("Exception while writing message", e);
				}
			}

		}

	}

}
