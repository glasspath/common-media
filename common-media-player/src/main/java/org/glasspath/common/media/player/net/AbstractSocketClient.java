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
package org.glasspath.common.media.player.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public abstract class AbstractSocketClient {

	public static final int CONNECT_TIMEOUT = 1000;
	public static final int MAX_CONNECT_RETRIES = 10;
	public static final int RECEIVE_INTERVAL = 100;

	private final Thread socketThread;
	private Socket socket = null;
	private BufferedReader bufferedReader = null;
	private PrintWriter printWriter = null;

	private String welcomeMessage = null;

	private boolean exit = false;

	public AbstractSocketClient(String ip, int port) {

		socketThread = new Thread(new Runnable() {

			@Override
			public void run() {

				int retryCount = 0;

				while (!exit) {

					try {

						if (socket != null) {
							try {
								socket.close();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}

						System.out.println("Socket client attempting to connect to ip and port: " + ip + ", " + port);
						socket = new Socket();
						socket.setKeepAlive(true);
						socket.connect(new InetSocketAddress(ip, port), CONNECT_TIMEOUT);

						System.out.println("Socket client connected to ip and port: " + ip + ", " + port);
						bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
						printWriter = new PrintWriter(socket.getOutputStream(), true);
						clientConnected();

						if (welcomeMessage != null) {
							writeLine(welcomeMessage);
						}

						try {

							while (!exit) {

								if (bufferedReader != null) {

									String line;
									while ((line = bufferedReader.readLine()) != null) {
										parseLine(line);
									}

								}

								Thread.sleep(RECEIVE_INTERVAL);

							}

						} catch (Exception e) {
							// e.printStackTrace();
							System.out.println("Socket client is no longer connected");
							exit();
						}

					} catch (Exception e) {

						if (!exit) {

							e.printStackTrace();

							System.out.println("Socket client failed to connected to ip and port: " + ip + ", " + port);

							retryCount++;
							if (retryCount > MAX_CONNECT_RETRIES) {
								exit();
							}

						}

					}

				}

				socketThreadExited();

			}
		});

		socketThread.start();

	}

	public String getWelcomeMessage() {
		return welcomeMessage;
	}

	public void setWelcomeMessage(String welcomeMessage) {
		this.welcomeMessage = welcomeMessage;
	}

	public abstract void clientConnected();

	public abstract void parseLine(String line);

	public void writeLine(String line) {

		try {

			if (printWriter != null) {
				printWriter.println(line);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public abstract void clientDisconnected();

	public abstract void socketThreadExited();

	public void exit() {

		System.out.println("Socket client, exit() called");

		exit = true;

		if (socket != null) {
			try {
				socket.close();
				socket = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
