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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.glasspath.common.media.rtsp.Authentication.DigestAuthentication;

public abstract class RtspResponseParser {

	public static final int DEFAULT_TIMEOUT = 10000;
	public static final String RTSP_1_0_KEY_LOWER_CASE = "rtsp/1.0";
	public static final String C_SEQ_KEY_LOWER_CASE = "cseq: ";
	public static final String AUTH_REALM_START_KEY_LOWER_CASE = "realm=\"";
	public static final String AUTH_REALM_END_KEY_LOWER_CASE = "\"";
	public static final String AUTH_DIGEST_KEY_LOWER_CASE = "www-authenticate: digest";
	public static final String AUTH_DIGEST_NONCE_START_KEY_LOWER_CASE = "nonce=\"";
	public static final String AUTH_DIGEST_NONCE_END_KEY_LOWER_CASE = "\"";
	public static final String AUTH_DIGEST_OPAQUE_START_KEY_LOWER_CASE = "opaque=\"";
	public static final String AUTH_DIGEST_OPAQUE_END_KEY_LOWER_CASE = "\"";
	public static final String SESSION_KEY_LOWER_CASE = "session: ";
	public static final String SESSION_TIMEOUT_KEY_LOWER_CASE = ";timeout=";

	private int timeout = DEFAULT_TIMEOUT;
	private int replyCode = -1;
	private int cSeq = -1;
	private DigestAuthentication digestAuthentication = null;
	private String session = null;
	private boolean done = false;

	public RtspResponseParser() {

	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getReplyCode() {
		return replyCode;
	}

	public DigestAuthentication getDigestAuthentication() {
		return digestAuthentication;
	}

	public String getSession() {
		return session;
	}

	public boolean isDone() {
		return done;
	}

	public void parseMessage(String message) {

		if (RtspClient.TODO_DEBUG) {
			System.out.println("Response:");
			System.out.println(message);
		}

		BufferedReader bufferedReader = new BufferedReader(new StringReader(message));

		try {

			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if (replyCode == -1 && parseReplyCode(line)) {
					continue;
				} else if (cSeq == -1 && parseCSeq(line)) {
					continue;
				} else if (replyCode == 401 && digestAuthentication == null && parseDigestAuthentication(line)) {
					continue;
				} else if (session == null && parseSession(line)) {
					continue;
				} else {
					parseMessageLine(line);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		done = true;

	}

	public abstract void parseMessageLine(String line);

	private boolean parseReplyCode(String line) {

		if (line.toLowerCase().startsWith(RTSP_1_0_KEY_LOWER_CASE)) {

			int indexOfFirstSpace = line.indexOf(" ");
			if (indexOfFirstSpace > 0) {

				int indexOfSecondSpace = line.indexOf(" ", indexOfFirstSpace + 1);
				if (indexOfSecondSpace > indexOfFirstSpace) {
					try {
						replyCode = Integer.parseInt(line.substring(indexOfFirstSpace + 1, indexOfSecondSpace));
						return true;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			}

		}

		return false;

	}

	private boolean parseCSeq(String line) {

		if (line.toLowerCase().startsWith(C_SEQ_KEY_LOWER_CASE)) {

			try {
				cSeq = Integer.parseInt(line.substring(C_SEQ_KEY_LOWER_CASE.length()));
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		return false;

	}

	private boolean parseDigestAuthentication(String line) {

		String lineLowerCase = line.toLowerCase();

		if (lineLowerCase.startsWith(AUTH_DIGEST_KEY_LOWER_CASE)) {

			int startIndex = lineLowerCase.indexOf(AUTH_DIGEST_NONCE_START_KEY_LOWER_CASE, AUTH_DIGEST_KEY_LOWER_CASE.length());
			if (startIndex > 0) {

				int fromIndex = startIndex + AUTH_DIGEST_NONCE_START_KEY_LOWER_CASE.length();
				int endIndex = lineLowerCase.indexOf(AUTH_DIGEST_NONCE_END_KEY_LOWER_CASE, fromIndex);
				if (endIndex > fromIndex) {
					digestAuthentication = new DigestAuthentication();
					digestAuthentication.setNonce(line.substring(fromIndex, endIndex));
				}

			}

			if (digestAuthentication != null) {

				// TODO: Realm should always be available
				startIndex = lineLowerCase.indexOf(AUTH_REALM_START_KEY_LOWER_CASE, AUTH_DIGEST_KEY_LOWER_CASE.length());
				if (startIndex > 0) {

					int fromIndex = startIndex + AUTH_REALM_START_KEY_LOWER_CASE.length();
					int endIndex = lineLowerCase.indexOf(AUTH_REALM_END_KEY_LOWER_CASE, fromIndex);
					if (endIndex > fromIndex) {
						digestAuthentication.setRealm(line.substring(fromIndex, endIndex));
					}

				}

				startIndex = lineLowerCase.indexOf(AUTH_DIGEST_OPAQUE_START_KEY_LOWER_CASE, AUTH_DIGEST_KEY_LOWER_CASE.length());
				if (startIndex > 0) {

					int fromIndex = startIndex + AUTH_DIGEST_OPAQUE_START_KEY_LOWER_CASE.length();
					int endIndex = lineLowerCase.indexOf(AUTH_DIGEST_OPAQUE_END_KEY_LOWER_CASE, fromIndex);
					if (endIndex > fromIndex) {
						digestAuthentication.setOpaque(line.substring(fromIndex, endIndex));
					}

				}

				return true;

			}

		}

		return false;

	}

	private boolean parseSession(String line) {

		if (line.toLowerCase().startsWith(SESSION_KEY_LOWER_CASE)) {

			session = line.substring(SESSION_KEY_LOWER_CASE.length());

			if (session.length() > 0) {

				int indexOfTimeout = session.toLowerCase().indexOf(SESSION_TIMEOUT_KEY_LOWER_CASE);
				if (indexOfTimeout > 0) {

					session = session.substring(0, indexOfTimeout);

					// TODO: Parse/store timeout?

				}

				return true;

			}

		}

		return false;

	}

	public void waitForCompletion() {

		long start = System.currentTimeMillis();

		while (!done) {

			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (timeout > 0 && System.currentTimeMillis() > start + timeout) {
				break;
			}

		}

	}

}
