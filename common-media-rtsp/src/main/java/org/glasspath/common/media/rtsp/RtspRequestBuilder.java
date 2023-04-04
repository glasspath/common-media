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

import java.util.Base64;

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

	public RtspRequestBuilder(RtspUrl rtspUrl) {
		this.rtspUrl = rtspUrl;
	}

	public RtspRequestBuilder begin(RtspRequest rtspRequest) {
		request.append(rtspRequest.getText() + " rtsp://" + rtspUrl.getHost() + ":" + rtspUrl.getPort() + "/" + rtspUrl.getMovie() + " RTSP/1.0" + CRLF);
		return this;
	}

	public RtspRequestBuilder begin(RtspRequest rtspRequest, String stream) {
		return begin(rtspRequest, stream, true);
	}

	public RtspRequestBuilder begin(RtspRequest rtspRequest, String url, boolean appendUrl) {
		if (appendUrl) {
			request.append(rtspRequest.getText() + " rtsp://" + rtspUrl.getHost() + ":" + rtspUrl.getPort() + "/" + rtspUrl.getMovie() + "/" + url + " RTSP/1.0" + CRLF);
		} else {
			request.append(rtspRequest.getText() + " " + url + " RTSP/1.0" + CRLF);
		}
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

	public RtspRequestBuilder appendBasicAuthentication() {
		if (rtspUrl.getUsername() != null && rtspUrl.getPassword() != null) {
			request.append("Authorization: Basic " + new String(Base64.getEncoder().encode((rtspUrl.getUsername() + ":" + rtspUrl.getPassword()).getBytes())) + CRLF);
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

}
