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
package org.glasspath.common.media.mfsdk.player;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.imageio.IIOException;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.Timer;

import org.glasspath.common.media.image.GifExporter;
import org.glasspath.common.media.mfsdk.EvrCanvas;
import org.glasspath.common.media.mfsdk.EvrCanvas.PlayerInstanceState;
import org.glasspath.common.media.mfsdk.EvrCanvasPanel;
import org.glasspath.common.media.mfsdk.FrameGenerator;
import org.glasspath.common.media.mfsdk.MFUtils;
import org.glasspath.common.media.player.IVideoPlayer;
import org.glasspath.common.media.player.IVideoPlayerPanel;
import org.glasspath.common.media.video.Video;

@SuppressWarnings("serial")
public class MFVideoPlayerPanel extends EvrCanvasPanel implements IVideoPlayerPanel {

	private enum PlayerState {
		PLAYER_STOPPED,
		PLAYER_STARTING,
		PLAYER_STARTED,
		PLAYER_STOPPING
	}

	public static final int TIMEOUT = 3000;
	public static final int STEP_FAST_FORWARD_REVERSE = 20;
	public static final int EVENT_INTERVAL = 10;
	public static final int MIN_SEEK_INTERVAL = 100;

	protected final IVideoPlayer context;
	protected final Video video;
	private final KeyEventDispatcher keyListener;
	private long playerInstance = -1;
	private PlayerState playerState = PlayerState.PLAYER_STOPPED;
	private boolean repeatEnabled = false;
	private int frameInterval = 333333; // TODO
	private long duration = 0;
	private long timestamp = 0;
	private boolean playing = true;
	private boolean rateZero = false;
	private int lastRate = 100;
	private long lastSeek = 0;
	private QueuedSeekCommand queuedSeekCommand = null;
	private Loop loop = null;
	private boolean performSeekHack1 = false; // TODO
	private boolean performSeekHack2 = false; // TODO
	private long lastSeekJump = 0;
	private long lastUpdateVideoPosition = 0;

	public MFVideoPlayerPanel(IVideoPlayer context, Video video) {

		super();

		this.context = context;
		this.video = video;

		keyListener = new KeyEventDispatcher() {

			@Override
			public boolean dispatchKeyEvent(KeyEvent e) {

				if (context.getFrame().isActive() && e.getID() == KeyEvent.KEY_PRESSED) {

					switch (e.getKeyCode()) {

					/*
					case KeyEvent.VK_SPACE:
						togglePlaying();
						e.consume();
						break;
					 */

					case KeyEvent.VK_LEFT:
						previousFrame(e.isControlDown() ? STEP_FAST_FORWARD_REVERSE : 1);
						break;

					case KeyEvent.VK_RIGHT:
						nextFrame(e.isControlDown() ? STEP_FAST_FORWARD_REVERSE : 1);
						break;

					case KeyEvent.VK_UP:
						if (e.isControlDown()) {
							zoomIn();
						}
						break;

					case KeyEvent.VK_DOWN:
						if (e.isControlDown()) {
							zoomOut();
						}
						break;

					default:
						break;
					}

				}

				return false;

			}
		};

		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyListener);

		setBorder(BorderFactory.createEmptyBorder());

		if (context.getFrame() != null) {

			context.getFrame().addWindowListener(new WindowAdapter() {

				@Override
				public void windowActivated(WindowEvent e) {

					if (!playing) {
						performSeekHack1 = true;
					}

				}
			});

		}

		Timer eventTimer = new Timer(EVENT_INTERVAL, new ActionListener() {

			private long previousTimestamp = 0;

			@Override
			public void actionPerformed(ActionEvent e) {

				if (playerInstance >= 0) {

					if (playing) {

						// Detect if playback ended
						if (evrCanvas.getPlayerState(playerInstance) == EvrCanvas.PlayerState.PLAYER_STATE_PAUSED.getValue()) {

							playing = false;

							context.firePlaybackStateChanged(playing);
							context.updateControls();

							if (repeatEnabled) {
								lastSeekJump = System.currentTimeMillis();
								play();
							}

						}

						if (duration <= 0) {
							duration = evrCanvas.getDuration(playerInstance);
						}

						timestamp = evrCanvas.getTimestamp(playerInstance);
						if (timestamp != previousTimestamp) {

							context.fireTimestampChanged(timestamp);
							context.repaintTimelineBar();

							if (playing && loop != null && loop.fromTimestamp != null && loop.toTimestamp != null && timestamp >= loop.toTimestamp) {

								lastSeekJump = System.currentTimeMillis();

								if (System.currentTimeMillis() <= lastUpdateVideoPosition + 25) {
									try {
										Thread.sleep(25);
									} catch (InterruptedException e1) {
										e1.printStackTrace();
									}
								}

								pause();
								boolean pausedOk = true;
								final long pauseStart = System.currentTimeMillis();
								while (evrCanvas.getPlayerState(playerInstance) != EvrCanvas.PlayerState.PLAYER_STATE_PAUSED.getValue()) {
									try {
										Thread.sleep(2);
										if (System.currentTimeMillis() > pauseStart + 750) {
											System.err.println("Timeout while waiting for player to pause at end of loop");
											pausedOk = false;
											break;
										}
									} catch (InterruptedException e1) {
										e1.printStackTrace();
									}
								}

								if (pausedOk) {
									seek(loop.fromTimestamp);
									play();
								}

							}

							previousTimestamp = timestamp;

						}

					} else {

						if (System.currentTimeMillis() > lastSeek + MIN_SEEK_INTERVAL) {

							if (queuedSeekCommand != null) {

								seek(queuedSeekCommand.timestamp);
								if (queuedSeekCommand.play && !isPlaying()) {
									play();
								}

								queuedSeekCommand = null;

							} else if (performSeekHack1) {
								seek(timestamp);
								performSeekHack1 = false;
							}

							// } else {
							// evrCanvas.repaintPlayer(playerInstance); //TODO: Quick test..
						}

					}

				}

			}
		});
		eventTimer.start();

	}

	@Override
	protected long getPlayerInstance() {
		return playerInstance;
	}

	@Override
	public JComponent getComponent() {
		return this;
	}

	@Override
	public void videoPlayerShown() {
		startPlayer();
	}

	@Override
	public void setRepeatEnabled(boolean repeatEnabled) {
		this.repeatEnabled = repeatEnabled;
	}

	@Override
	public boolean isRepeatEnabled() {
		return repeatEnabled;
	}

	@Override
	public void play() {

		if (playerInstance >= 0 && !playing) {

			duration = evrCanvas.getDuration(playerInstance);

			if (performSeekHack2) {
				System.out.println("Performing seek hack 2");
				seek(timestamp);
				performSeekHack2 = false;
			}

			int rate = context.getRate();
			if (rateZero || rate != lastRate) {

				if (evrCanvas.setRate(playerInstance, rate / 100.0F) < 0) {
					System.out.println("setRate failed..");
				}

				rateZero = false;

			}

			evrCanvas.play(playerInstance);
			playing = true;

			context.updateControls();

		}

	}

	@Override
	public void pause() {

		if (playerInstance >= 0 && playing) {

			evrCanvas.pause(playerInstance);
			playing = false;

			context.updateControls();

		}

	}

	@Override
	public void togglePlaying() {

		if (playing) {
			pause();
		} else {
			play();
		}

	}

	@Override
	public boolean isPlaying() {
		return playing;
	}

	@Override
	public void nextFrame(int step) {
		seek(step, false);
	}

	@Override
	public void previousFrame(int step) {
		seek(step, true);
	}

	private void seek(int step, boolean reverse) {

		if (playerInstance >= 0 && queuedSeekCommand == null) {

			if (playing) {
				pause();
			}

			if (System.currentTimeMillis() > lastSeek + MIN_SEEK_INTERVAL) {

				duration = evrCanvas.getDuration(playerInstance);
				timestamp = evrCanvas.getTimestamp(playerInstance);
				// System.out.println("Duration: " + duration + ", current timestamp: " + timestamp);

				if (!rateZero) {
					if (evrCanvas.setRate(playerInstance, 0.0F) < 0) {
						System.out.println("setRate failed..");
					}
					rateZero = true;
					timestamp += 2 * frameInterval; // TODO: This is a quick hack
				}

				if (reverse) {
					timestamp -= step * frameInterval;
				} else {
					timestamp += step * frameInterval;
				}

				if (timestamp > duration) {
					timestamp = duration;
				} else if (timestamp < 0) {
					timestamp = 0;
				}

				if (evrCanvas.seek(playerInstance, timestamp) < 0) {
					System.out.println("Seek failed..");
				}

				context.fireTimestampChanged(timestamp);
				context.repaintTimelineBar();

				lastSeek = System.currentTimeMillis();

			} else {

				if (reverse) {
					queuedSeekCommand = new QueuedSeekCommand(timestamp - (step * frameInterval), false);
				} else {
					queuedSeekCommand = new QueuedSeekCommand(timestamp + (step * frameInterval), false);
				}

			}

		}

	}

	private void seek(long seekTimestamp) {

		duration = evrCanvas.getDuration(playerInstance);
		timestamp = seekTimestamp;

		if (!rateZero) {
			if (evrCanvas.setRate(playerInstance, 0.0F) < 0) {
				System.out.println("setRate failed..");
			}
			rateZero = true;
		}

		if (timestamp > duration) {
			timestamp = duration;
		} else if (timestamp < 0) {
			timestamp = 0;
		}

		if (evrCanvas.seek(playerInstance, timestamp) < 0) {
			System.out.println("Seek failed..");
		}

		context.fireTimestampChanged(timestamp);
		context.repaintTimelineBar();

		lastSeek = System.currentTimeMillis();

	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public void setTimestamp(long timestamp, boolean play) {
		queuedSeekCommand = new QueuedSeekCommand(timestamp, play);
	}

	@Override
	public long getDuration() {
		return duration;
	}

	@Override
	public Loop getLoop() {
		return loop;
	}

	@Override
	public void setLoop(Loop loop) {
		this.loop = loop;
	}

	@Override
	public void exportLoopToGif(GifExportRequest request) {

		if (loop != null && loop.fromTimestamp != null && loop.toTimestamp != null) {

			new Thread(new Runnable() {

				private ImageOutputStream output = null;
				private GifExporter gifExporter = null;
				private boolean writerInited = false;
				private int imageCount = 0;
				private int totalImageCount = 0;

				@Override
				public void run() {

					final int callbackId = 1;

					final FrameGenerator frameGenerator = new FrameGenerator() {

						@Override
						public void frameGenerated(long instance, int requestId, long requestedTimestamp, long actualTimestamp, int width, int height, byte[] bitmapData) {

							if (callbackId == requestId) {

								final BufferedImage image = FrameGenerator.createBufferedImage(bitmapData, width, height);
								if (image != null) {

									/*
									if (width != activeCallback.width || height != activeCallback.height) {
										image = scaleImage(image, activeCallback.width, activeCallback.height);
									}
									 */

									if (!writerInited) {

										System.out.println("Creating gif writer");
										writerInited = true;

										try {
											output = new FileImageOutputStream(request.getFile());
										} catch (FileNotFoundException e) {
											e.printStackTrace();
										} catch (IOException e) {
											e.printStackTrace();
										}

										try {
											gifExporter = new GifExporter(output, image.getType(), 100, true);
										} catch (IIOException e) {
											e.printStackTrace();
										} catch (IOException e) {
											e.printStackTrace();
										}

									}

									if (gifExporter != null) {

										try {
											System.out.println("Writing image to gif");
											gifExporter.writeImage(image);
										} catch (IOException e) {
											e.printStackTrace();
										}

									}

									imageCount++;
									request.progressUpdate(imageCount, totalImageCount);

								}

							}

						}
					};

					if (loop.toTimestamp > loop.fromTimestamp) {

						int width = 640; // TODO
						int height = 480; // TODO
						long interval = 100 * 10000; // TODO: Make fps configurable (currently 10fps)
						totalImageCount = (int) ((loop.toTimestamp - loop.fromTimestamp) / interval) + 1; // TODO: Why + 1?

						final long generatorInstance = frameGenerator.createInstance();
						if (generatorInstance >= 0) {

							if (frameGenerator.openFile(generatorInstance, video.getPath(), MFUtils.MFSDK_SOURCE_TYPE_DEFAULT) >= 0) {
								System.out.println("Requesting frames, timestamp = " + loop.fromTimestamp + ", interval = " + interval + ", totalImageCount = " + totalImageCount);
								frameGenerator.requestFrames(generatorInstance, callbackId, loop.fromTimestamp, interval, totalImageCount, width, height);
							}

							System.out.println("Closing files and streams");

							if (gifExporter != null) {

								try {
									gifExporter.close();
								} catch (IOException e) {
									e.printStackTrace();
								}

							}

							if (output != null) {

								try {
									output.close();
								} catch (IOException e) {
									e.printStackTrace();
								}

							}

							frameGenerator.close(generatorInstance);

						}

					}

				}
			}).start();

		}

	}

	@Override
	public void exit() {
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyListener);
		stopPlayer();
	}

	@Override
	public boolean isExited() {
		return playerState == PlayerState.PLAYER_STOPPED;
	}

	@Override
	protected void updateVideoPosition() {

		// TODO: This is a quick hack..
		if (System.currentTimeMillis() > lastSeekJump + 100) {

			lastUpdateVideoPosition = System.currentTimeMillis();

			super.updateVideoPosition();

			rateZero = false;

			if (!playing) {
				if (rateZero) {
					// performSeekHack1 = true;
				} else {
					performSeekHack2 = true;
				}
			}

		}

	}

	private synchronized void startPlayer() {

		if (playerState == PlayerState.PLAYER_STOPPED) {

			playerState = PlayerState.PLAYER_STARTING;

			playerInstance = evrCanvas.createPlayer();

			if (playerInstance >= 0) {

				evrCanvas.setVisible(true);

				new Thread(new Runnable() {

					@Override
					public void run() {

						new Thread(new Runnable() {

							@Override
							public void run() {

								long waitUntil = System.currentTimeMillis() + TIMEOUT;
								while (true) {

									if (evrCanvas.getPlayerInstanceState(playerInstance) == PlayerInstanceState.PLAYER_INSTANCE_STATE_RUNNING.getValue()) {
										evrCanvas.setPaintBackgroundOnly(playerInstance, false);
										playerState = PlayerState.PLAYER_STARTED;
										break;
									} else if (System.currentTimeMillis() >= waitUntil) {
										System.out.println("Timeout while waiting for player started..");
										break;
									}

									try {
										Thread.sleep(10);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}

								}

							}
						}).start();

						int sourceType;
						if (!video.isPathValid()) {
							sourceType = MFUtils.MFSDK_SOURCE_TYPE_DUMMY;
						} else {
							sourceType = MFUtils.MFSDK_SOURCE_TYPE_DEFAULT;
						}

						// Blocks until stopPlayer() is called
						evrCanvas.startPlayer(playerInstance, sourceType, video.getPath(), 1.0);

						playerState = PlayerState.PLAYER_STOPPED;

					}
				}).start();

			} else {
				playerState = PlayerState.PLAYER_STOPPED;
			}

		}

	}

	private synchronized void stopPlayer() {

		long waitUntil = System.currentTimeMillis() + TIMEOUT;
		while (playerState == PlayerState.PLAYER_STARTING) {
			try {
				Thread.sleep(10);
				if (System.currentTimeMillis() >= waitUntil) {
					break;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		if (playerInstance >= 0) {
			playerState = PlayerState.PLAYER_STOPPING;
			evrCanvas.stopPlayer(playerInstance);
			playerInstance = -1;
		}

		waitUntil = System.currentTimeMillis() + TIMEOUT;
		while (playerState != PlayerState.PLAYER_STOPPED) {
			try {
				Thread.sleep(10);
				if (System.currentTimeMillis() >= waitUntil) {
					playerState = PlayerState.PLAYER_STOPPED;
					break;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		evrCanvas.setVisible(false);

	}

	@Override
	public JMenu prepareMenu() {

		JMenu menu = super.prepareMenu();

		// TODO?

		return menu;

	}

	private static class QueuedSeekCommand {

		private final long timestamp;
		private final boolean play;

		private QueuedSeekCommand(long timestamp, boolean play) {
			this.timestamp = timestamp;
			this.play = play;
		}

	}

}
