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

public class RtspUrl {

	public static final String DEFAULT_RTSP_HOST = "127.0.0.1";
	public static final int DEFAULT_RTSP_PORT = 554;
	public static final String RTSP_PROTOCOL_PREFIX = "rtsp://";
	public static final String RTSPS_PROTOCOL_PREFIX = "rtsps://";

	private String host = DEFAULT_RTSP_HOST;
	private int port = DEFAULT_RTSP_PORT;
	private String username = null;
	private String password = null;
	private String movie = null;

	public RtspUrl() {

	}

	public RtspUrl(String url) {
		parseUrl(url);
	}

	public void parseUrl(String url) {

		if (url != null) {

			String urlLowerCase = url.toLowerCase();

			String usernamePasswordHostPortMovie;

			if (urlLowerCase.startsWith(RTSP_PROTOCOL_PREFIX)) {
				usernamePasswordHostPortMovie = url.substring(RTSP_PROTOCOL_PREFIX.length());
			} else if (urlLowerCase.startsWith(RTSPS_PROTOCOL_PREFIX)) {
				usernamePasswordHostPortMovie = url.substring(RTSPS_PROTOCOL_PREFIX.length());
			} else {
				usernamePasswordHostPortMovie = null;
			}

			if (usernamePasswordHostPortMovie != null) {

				int indexOfSlash = usernamePasswordHostPortMovie.indexOf("/");

				String usernamePasswordHostPort;

				if (indexOfSlash > 0) {
					usernamePasswordHostPort = usernamePasswordHostPortMovie.substring(0, indexOfSlash);
					movie = usernamePasswordHostPortMovie.substring(indexOfSlash + 1);
				} else {
					usernamePasswordHostPort = null;
				}

				if (usernamePasswordHostPort != null) {

					int indexOfColon = usernamePasswordHostPort.indexOf(":");
					int indexOfAt = usernamePasswordHostPort.indexOf("@");

					String hostPort;

					if (indexOfColon > 0 && indexOfAt > indexOfColon) {
						username = usernamePasswordHostPort.substring(0, indexOfColon);
						password = usernamePasswordHostPort.substring(indexOfColon + 1, indexOfAt);
						hostPort = usernamePasswordHostPort.substring(indexOfAt + 1, usernamePasswordHostPort.length());
					} else {
						hostPort = usernamePasswordHostPort;
					}

					indexOfColon = hostPort.indexOf(":");
					if (indexOfColon > 0) {
						host = hostPort.substring(0, indexOfColon);
						try {
							port = Integer.parseInt(hostPort.substring(indexOfColon + 1, hostPort.length()));
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						host = hostPort;
					}

				}

			}

		}

	}

	// TODO
	public String getUrl() {
		if (username != null && password != null) {
			return RTSP_PROTOCOL_PREFIX + username + ":" + password + "@" + host + ":" + port + "/" + movie;
		} else {
			return RTSP_PROTOCOL_PREFIX + host + ":" + port + "/" + movie;
		}
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getMovie() {
		return movie;
	}

	public void setMovie(String movie) {
		this.movie = movie;
	}

	@Override
	public String toString() {
		return "host:" + host + " port:" + port + " username:" + username + " password:" + password + " movie:" + movie;
	}

}
