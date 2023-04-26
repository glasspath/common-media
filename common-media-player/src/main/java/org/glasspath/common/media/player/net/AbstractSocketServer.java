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
import java.net.ServerSocket;
import java.net.Socket;

public abstract class AbstractSocketServer {

	public static final int DEFAULT_SOCKET_SERVER_PORT_FROM = 7500;
	public static final int DEFAULT_SOCKET_SERVER_PORT_TO = 7525;
	public static final int SOCKET_SO_TIMEOUT = 15000;
	public static final int RECEIVE_INTERVAL = 100;
	public static final int CHECK_ALIVE_INTERVAL = 500;

	private final Thread socketThread;
	private final Thread receiveThread;
	private ServerSocket serverSocket = null;
	private Socket socket = null;
	private BufferedReader bufferedReader = null;
	private PrintWriter printWriter = null;
	private int port = -1;

	private String welcomeMessage = null;

	private boolean exit = false;

	public AbstractSocketServer(int portFrom, int portTo) {

		socketThread = new Thread(new Runnable() {

			@Override
			public void run() {

				int portToTry = portFrom;

				while (!exit) {

					try {

						System.out.println("Starting socket server on port " + portToTry);
						serverSocket = new ServerSocket(portToTry);

						port = portToTry;
						socketServerStarted(port);

						while (!exit) {

							try {

								if (socket != null) {
									try {
										socket.close();
									} catch (Exception e) {
										e.printStackTrace();
									}
								}

								System.out.println("Socket server waiting for client on port " + port);
								serverSocket.setSoTimeout(SOCKET_SO_TIMEOUT);
								socket = serverSocket.accept();

								System.out.println("Socket server accepted client on port " + port);
								bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
								printWriter = new PrintWriter(socket.getOutputStream(), true);

								if (welcomeMessage != null) {
									writeLine(welcomeMessage);
								}

								try {

									while (!exit) {

										Thread.sleep(CHECK_ALIVE_INTERVAL);

										if (socket.getOutputStream() != null) {
											socket.getOutputStream().write(new byte[0]);
										} else {
											clientDisconnected();
											break;
										}

									}

								} catch (Exception e) {
									// e.printStackTrace();
									clientDisconnected();
								}

							} catch (Exception e) {
								e.printStackTrace();
							}

						}

					} catch (Exception e) {

						if (!exit) {

							e.printStackTrace();

							if (port == -1 && portToTry < portTo) {
								System.out.println("Failed to start socket server on port " + portToTry);
								portToTry++;
							} else {
								exit();
							}

						}

					}

				}

				socketThreadExited();

			}
		});

		receiveThread = new Thread(new Runnable() {

			@Override
			public void run() {

				while (!exit) {

					try {

						if (socket != null && !socket.isClosed() && bufferedReader != null) {

							String line;
							while ((line = bufferedReader.readLine()) != null) {
								parseLine(line);
							}

						}

					} catch (Exception e) {
						// e.printStackTrace();
						clientDisconnected();
					}

					try {
						Thread.sleep(RECEIVE_INTERVAL);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				}

			}
		});

		socketThread.start();
		receiveThread.start();

	}

	public int getPort() {
		return port;
	}

	public String getWelcomeMessage() {
		return welcomeMessage;
	}

	public void setWelcomeMessage(String welcomeMessage) {
		this.welcomeMessage = welcomeMessage;
	}

	public abstract void socketServerStarted(int port);

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

		exit = true;

		if (socket != null) {
			try {
				socket.close();
				socket = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (serverSocket != null) {
			try {
				serverSocket.close();
				serverSocket = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
