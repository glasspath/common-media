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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import org.glasspath.common.media.rtsp.Authentication.DigestAuthentication;

public class RtspRequestBuilder {

	public static enum RtspRequest {

		OPTIONS("OPTIONS"),
		DESCRIBE("DESCRIBE"),
		SETUP("SETUP"),
		PLAY("PLAY"),
		PAUSE("PAUSE"),
		GET_PARAMETER("GET_PARAMETER"),
		SET_PARAMETER("SET_PARAMETER"),
		TEARDOWN("TEARDOWN");

		private final String text;

		private RtspRequest(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}

	}

	public static final String CRLF = "\r\n";

	private final RtspUrl rtspUrl;
	private final StringBuilder request = new StringBuilder();
	private String requestMethod = "";
	private String requestUri = "";

	public RtspRequestBuilder(RtspUrl rtspUrl) {
		this.rtspUrl = rtspUrl;
	}

	public RtspRequestBuilder begin(RtspRequest rtspRequest) {

		requestMethod = rtspRequest.getText();
		requestUri = "rtsp://" + rtspUrl.getHost() + ":" + rtspUrl.getPort() + "/" + rtspUrl.getMovie();

		request.append(requestMethod + " " + requestUri + " RTSP/1.0" + CRLF);

		return this;

	}

	public RtspRequestBuilder begin(RtspRequest rtspRequest, String stream) {
		return begin(rtspRequest, stream, true);
	}

	public RtspRequestBuilder begin(RtspRequest rtspRequest, String url, boolean appendUrl) {

		requestMethod = rtspRequest.getText();

		if (appendUrl) {
			requestUri = "rtsp://" + rtspUrl.getHost() + ":" + rtspUrl.getPort() + "/" + rtspUrl.getMovie() + "/" + url;
		} else {
			requestUri = url;
		}

		request.append(requestMethod + " " + requestUri + " RTSP/1.0" + CRLF);

		return this;

	}

	public RtspRequestBuilder appendCSeq(int cSeq) {
		request.append("CSeq: " + cSeq + CRLF);
		return this;
	}

	public RtspRequestBuilder appendUserAgent(String userAgent) {
		request.append("User-Agent: " + userAgent + CRLF);
		return this;
	}

	public RtspRequestBuilder appendAuthorization(Authentication authentication) {

		// TODO? For now we start with basic authorization by default
		if (rtspUrl.getUsername() != null && rtspUrl.getPassword() != null) {

			if (authentication instanceof DigestAuthentication) {

				DigestAuthentication auth = (DigestAuthentication) authentication;

				if (auth.getRealm() != null && auth.getNonce() != null) {

					String authHeader = "username=\"" + rtspUrl.getUsername() + "\"";
					authHeader += ", realm=\"" + auth.getRealm() + "\"";
					authHeader += ", algorithm=\"MD5\"";
					authHeader += ", nonce=\"" + auth.getNonce() + "\"";
					authHeader += ", uri=\"" + requestUri + "\"";

					String response = "";

					try {

						// https://en.wikipedia.org/wiki/Digest_access_authentication

						MessageDigest massageDigest = MessageDigest.getInstance("MD5");

						String ha1Source = rtspUrl.getUsername() + ":" + auth.getRealm() + ":" + rtspUrl.getPassword();
						String ha1 = bytesToHex(massageDigest.digest(ha1Source.getBytes(StandardCharsets.UTF_8))).toLowerCase();

						String ha2Source = requestMethod + ":" + requestUri;
						String ha2 = bytesToHex(massageDigest.digest(ha2Source.getBytes(StandardCharsets.UTF_8))).toLowerCase();

						String responseSource = ha1 + ":" + auth.getNonce() + ":" + ha2;
						response = bytesToHex(massageDigest.digest(responseSource.getBytes(StandardCharsets.UTF_8))).toLowerCase();

					} catch (Exception e) {
						e.printStackTrace();
					}

					authHeader += ", response=\"" + response + "\"";

					if (auth.getOpaque() != null) {
						authHeader += ", opaque=\"" + auth.getOpaque() + "\"";
					}

					request.append("Authorization: Digest " + authHeader + CRLF);

				} else {
					// TODO?
				}

			} else {
				request.append("Authorization: Basic " + new String(Base64.getEncoder().encode((rtspUrl.getUsername() + ":" + rtspUrl.getPassword()).getBytes())) + CRLF);
			}

		}

		return this;

	}

	public RtspRequestBuilder appendSession(String session) {
		if (session != null) {
			request.append("Session: " + session + CRLF);
		}
		return this;
	}

	public RtspRequestBuilder appendTransport(String transport) {
		request.append("Transport: " + transport + CRLF);
		return this;
	}

	public RtspRequestBuilder appendRange(String range) {
		request.append("Range: " + range + CRLF);
		return this;
	}

	public RtspRequestBuilder end() {
		request.append(CRLF);
		return this;
	}

	public String getRequest() {
		return request.toString();
	}

	// TODO
	// https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
	private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

	public static String bytesToHex(byte[] bytes) {
		byte[] hexChars = new byte[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars, StandardCharsets.UTF_8);
	}

}
