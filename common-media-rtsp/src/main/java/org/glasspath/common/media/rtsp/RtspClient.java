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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.glasspath.common.Common;
import org.glasspath.common.media.rtsp.RtspRequestBuilder.RtspRequest;
import org.glasspath.common.media.rtsp.TrackInfo.AudioTrackInfo;
import org.glasspath.common.media.rtsp.TrackInfo.VideoTrackInfo;

public class RtspClient {

	public static enum Transport {
		TCP,
		UDP
	}

	public static boolean TODO_DEBUG = true;

	public static final String DEFAULT_USER_AGENT = "Lavf58.29.100";
	public static final String DEFAULT_PLAY_REQUEST_RANGE = "npt=0.000-";
	public static final int DEFAULT_TIMEOUT = 2000;

	private final Transport transport;
	private RtspUrl rtspUrl = null;
	private String userAgent = DEFAULT_USER_AGENT;
	private int timeout = DEFAULT_TIMEOUT;
	private boolean disconnected = true;
	private Socket socket = null;
	private BufferedWriter writer = null;
	private RtspStreamReader streamReader = null;
	private MulticastSocket multicastSocket = null;
	private RtspResponseParser rtspResponseParser = null;
	private int cSeq = 0;
	private Authentication authentication = null;
	private String session = null;
	private String[] options = null;
	private VideoTrackInfo videoTrackInfo = null;
	private AudioTrackInfo audioTrackInfo = null;
	private String multicastAddress = "234.5.6.7";
	private int clientPortFrom = 5075;
	private int clientPortTo = 5076;
	private int serverPortFrom = -1;
	private int serverPortTo = -1;
	private long ssrc = 0;

	public RtspClient() {
		this(false);
	}

	public RtspClient(boolean udp) {
		transport = udp ? Transport.UDP : Transport.TCP;
	}

	public RtspUrl getRtspUrl() {
		return rtspUrl;
	}

	public void setRtspUrl(RtspUrl rtspUrl) {
		this.rtspUrl = rtspUrl;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public String getMulticastAddress() {
		return multicastAddress;
	}

	public void setMulticastAddress(String multicastAddress) {
		this.multicastAddress = multicastAddress;
	}

	public int getClientPortFrom() {
		return clientPortFrom;
	}

	public void setClientPortFrom(int clientPortFrom) {
		this.clientPortFrom = clientPortFrom;
	}

	public int getClientPortTo() {
		return clientPortTo;
	}

	public void setClientPortTo(int clientPortTo) {
		this.clientPortTo = clientPortTo;
	}

	public boolean connect() {

		if (rtspUrl != null) {

			try {

				socket = new Socket();
				socket.connect(new InetSocketAddress(rtspUrl.getHost(), rtspUrl.getPort()), timeout);

				// Create a BufferedWriter for sending requests
				writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

				// Install a RtspStreamReader for receiving responses and interleaved frames
				createRtspStreamReader(new DataInputStream(socket.getInputStream()));

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

	private void createRtspStreamReader(DataInputStream dataInputStream) {

		if (streamReader != null) {
			streamReader.stop();
		}

		streamReader = new RtspStreamReader() {

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
			public void rtspInterleavedFrameReceived(RtspInterleavedFrame rtspInterleavedFrame) {

			}

			@Override
			public void rtpPacketReceived(RtpPacket rtpPacket) {
				if (rtpPacket.getSsrc() == ssrc) {
					RtspClient.this.rtpPacketReceived(rtpPacket);
				} else {
					if (TODO_DEBUG) {
						System.err.println("ssrc mismatch: " + rtpPacket.getSsrc() + " != " + ssrc);
					}
				}
			}
		};

		streamReader.setRtspParserEnabled(true);
		streamReader.setRtpParserEnabled(false);
		streamReader.startReading(dataInputStream);

	}

	private void createMulticastSocket() {

		if (streamReader != null) {

			if (multicastSocket != null) {

				try {
					multicastSocket.close();
				} catch (Exception e) {
					if (TODO_DEBUG) {
						Common.LOGGER.debug("Exception while closing MulticastSocket", e);
					}
				}

			}

			try {

				multicastSocket = new MulticastSocket(clientPortFrom);
				multicastSocket.joinGroup(InetAddress.getByName(multicastAddress));

				streamReader.startReading(multicastSocket);

			} catch (Exception e) {
				if (TODO_DEBUG) {
					Common.LOGGER.debug("Exception while creating MulticastSocket", e);
				}
			}

		}

	}

	public void rtpPacketReceived(RtpPacket rtpPacket) {

	}

	public void disconnect() {

		if (streamReader != null) {

			streamReader.stop();

			int count = 0;
			while (!streamReader.isStopped() && count < 100) {

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

		if (multicastSocket != null) {

			try {
				multicastSocket.close();
			} catch (Exception e) {
				if (TODO_DEBUG) {
					Common.LOGGER.debug("Exception while disconnecting", e);
				}
			}

			multicastSocket = null;

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
					.appendUserAgent(userAgent)
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
					.appendUserAgent(userAgent)
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

	public int sendSetupRequest() {

		String t;

		switch (transport) {
		case UDP:
			// t = "RTP/AVP;unicast;client_port=" + clientPortFrom + "-" + clientPortTo;
			t = "RTP/AVP/UDP;unicast;client_port=" + clientPortFrom + "-" + clientPortTo + ";mode=receive"; // TODO?
			break;

		default:
			t = "RTP/AVP/TCP;unicast;interleaved=0-1";
			break;
		}

		return sendSetupRequest(videoTrackInfo, t);

	}

	public int sendSetupRequest(TrackInfo trackInfo, String transport) {

		if (rtspUrl != null && trackInfo != null) {

			String control;
			if (trackInfo.getTrackIdentifier().identifier != null && trackInfo.getTrackId() >= 0) {
				control = trackInfo.getTrackIdentifier().identifier + trackInfo.getTrackId();
			} else {
				control = trackInfo.getControl();
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
					.appendUserAgent(userAgent)
					.appendAuthorization(authentication)
					.appendSession(session)
					.end()
					.getRequest();

			RtspSetupResponseParser responseParser = new RtspSetupResponseParser();

			sendRequest(request, responseParser);

			serverPortFrom = responseParser.getServerPortFrom();
			serverPortTo = responseParser.getServerPortTo();
			ssrc = responseParser.getSsrc();

			if (this.transport == Transport.UDP) {
				createMulticastSocket();
			}

			return responseParser.getReplyCode();

		}

		return 0;

	}

	public int getServerPortFrom() {
		return serverPortFrom;
	}

	public int getServerPortTo() {
		return serverPortTo;
	}

	public long getSsrc() {
		return ssrc;
	}

	public boolean sendPlayRequest() {
		return sendPlayRequest(DEFAULT_PLAY_REQUEST_RANGE);
	}

	public boolean sendPlayRequest(String range) {

		if (rtspUrl != null) {

			String request = new RtspRequestBuilder(rtspUrl)
					.begin(RtspRequest.PLAY)
					.appendCSeq(++cSeq)
					.appendUserAgent(userAgent)
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
					.appendUserAgent(userAgent)
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

		if (writer != null) {

			try {

				rtspResponseParser = responseParser;

				writer.write(request);
				writer.flush();

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
