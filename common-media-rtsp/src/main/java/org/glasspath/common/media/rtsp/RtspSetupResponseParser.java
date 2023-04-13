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

public class RtspSetupResponseParser extends RtspResponseParser {

	public static final String TRANSPORT_KEY_LOWER_CASE = "transport: ";
	public static final String SERVER_PORT_KEY_LOWER_CASE = "server_port=";

	private int serverPortFrom = -1;
	private int serverPortTo = -1;

	public RtspSetupResponseParser() {

	}

	public int getServerPortFrom() {
		return serverPortFrom;
	}

	public int getServerPortTo() {
		return serverPortTo;
	}

	@Override
	public void parseMessageLine(String line) {

		if (line.toLowerCase().startsWith(TRANSPORT_KEY_LOWER_CASE)) {

			String[] entries = line.substring(TRANSPORT_KEY_LOWER_CASE.length()).split(";");

			for (String entry : entries) {

				if (entry.toLowerCase().startsWith(SERVER_PORT_KEY_LOWER_CASE)) {

					String serverPort = entry.substring(SERVER_PORT_KEY_LOWER_CASE.length());
					if (serverPort.contains("-")) {

						String[] serverPorts = serverPort.split("-");
						if (serverPorts.length >= 2) {

							try {
								serverPortFrom = Integer.parseInt(serverPorts[0]);
								serverPortTo = Integer.parseInt(serverPorts[1]);
							} catch (Exception e) {
								e.printStackTrace();
							}

						}

					} else {

						try {
							serverPortFrom = Integer.parseInt(serverPort);
							serverPortTo = serverPortFrom;
						} catch (Exception e) {
							e.printStackTrace();
						}

					}

				}

			}

		}

	}

}
