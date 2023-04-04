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

public abstract class RtspResponseParser {

	public static final int DEFAULT_TIMEOUT = 10000;
	public static final String SESSION_KEY_LOWER_CASE = "session: ";

	public static final String SESSION_TIMEOUT_KEY_LOWER_CASE = ";timeout=";

	private int timeout = DEFAULT_TIMEOUT;
	private int replyCode = -1;
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

	public String getSession() {
		return session;
	}

	public boolean isDone() {
		return done;
	}

	public void parseMessage(String message) {

		BufferedReader bufferedReader = new BufferedReader(new StringReader(message));

		try {

			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if (replyCode == -1 && parseReplyCode(line)) {
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

		if (line.startsWith("RTSP/1.0")) {

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

	private boolean parseSession(String line) {

		if (line.toLowerCase().startsWith(SESSION_KEY_LOWER_CASE)) {

			session = line.substring(SESSION_KEY_LOWER_CASE.length());

			if (session.length() > 0) {

				int indexOfTimeout = session.toLowerCase().indexOf(SESSION_TIMEOUT_KEY_LOWER_CASE);
				if (indexOfTimeout > 0) {
					session = session.substring(0, indexOfTimeout);
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
