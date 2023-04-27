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
package org.glasspath.common.media.player;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.glasspath.common.media.video.Frame;
import org.glasspath.common.media.video.Video;
import org.glasspath.common.swing.SwingUtils;

public abstract class VideoFramePlayerPanel implements IVideoPlayerPanel {

	public static final int STEP_FAST_FORWARD_REVERSE = 20;

	protected final IVideoPlayer context;
	protected final Video video;
	protected final FramePanel framePanel;
	private final Thread playbackThread;
	private boolean playing = IVideoPlayer.DEFAULT_PLAYBACK_STATE_PLAYING;
	private boolean repeatEnabled = true;
	private long timestamp = 0 * 1000L;
	private boolean showSingleFrame = false;
	private Loop loop = null;
	private boolean exit = false;

	public VideoFramePlayerPanel(IVideoPlayer context, Video video) {

		this.context = context;
		this.video = video;

		framePanel = new FramePanel(context.getFrame(), context.getPreferences());

		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {

			public boolean dispatchKeyEvent(KeyEvent e) {

				if (context.getFrame().isActive() && e.getID() == KeyEvent.KEY_PRESSED) {

					switch (e.getKeyCode()) {

					case KeyEvent.VK_SPACE:
						togglePlaying();
						e.consume();
						break;

					case KeyEvent.VK_LEFT:
						previousFrame(SwingUtils.isControlOrCmdDown(e) ? STEP_FAST_FORWARD_REVERSE : 1);
						break;

					case KeyEvent.VK_RIGHT:
						nextFrame(SwingUtils.isControlOrCmdDown(e) ? STEP_FAST_FORWARD_REVERSE : 1);
						break;

					case KeyEvent.VK_UP:
						if (SwingUtils.isControlOrCmdDown(e)) {
							framePanel.zoomIn();
						}
						break;

					case KeyEvent.VK_DOWN:
						if (SwingUtils.isControlOrCmdDown(e)) {
							framePanel.zoomOut();
						}
						break;

					default:
						break;
					}

				}

				return false;

			}
		});

		playbackThread = new Thread(new Runnable() {

			@Override
			public void run() {

				try {

					while (!exit) {

						if (playing) {

							Frame frame = getFrame();
							if (frame != null) {

								double speedFactor = 1.0 + (1.0 - (context.getRate() / 100.0));

								// TODO: Calculate remaining time
								int interval = (int) (33 * speedFactor);

								Thread.sleep(interval);

								SwingUtilities.invokeLater(new Runnable() {

									@Override
									public void run() {
										showFrame(frame);
									}
								});

							} else {
								Thread.sleep(10);
							}

						} else {

							if (showSingleFrame) {

								Frame frame = getFrame();
								if (frame != null) {

									showSingleFrame = false;

									SwingUtilities.invokeLater(new Runnable() {

										@Override
										public void run() {
											showFrame(frame);
										}
									});

								}

							}

							Thread.sleep(10);

						}

					}

				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		});

	}

	protected abstract Frame getFrame();

	private void showFrame(Frame frame) {

		framePanel.setFrame(frame);
		framePanel.repaint();

		timestamp = frame.getTimestamp().longValue();

		if (playing && loop != null && loop.fromTimestamp != null && loop.toTimestamp != null && timestamp >= loop.toTimestamp) {
			setTimestamp(loop.fromTimestamp, playing);
		}

		context.fireTimestampChanged(timestamp);

	}

	@Override
	public JComponent getComponent() {
		return framePanel;
	}

	@Override
	public void videoPlayerShown() {
		playbackThread.start();
	}

	@Override
	public boolean isRepeatEnabled() {
		return repeatEnabled;
	}

	@Override
	public void setRepeatEnabled(boolean repeatEnabled) {
		this.repeatEnabled = repeatEnabled;
	}

	@Override
	public void play() {
		setPlaying(true);
	}

	@Override
	public void pause() {
		setPlaying(false);
	}

	@Override
	public void togglePlaying() {
		setPlaying(!isPlaying());
	}

	@Override
	public boolean isPlaying() {
		return playing;
	}

	public void setPlaying(boolean playing) {
		this.playing = playing;
		context.firePlaybackStateChanged(playing);
	}

	@Override
	public void nextFrame(int step) {

		if (isPlaying()) {
			pause();
		}

		if (step == 1) {

			Frame frame = getFrame();
			if (frame != null) {
				showFrame(frame);
			}

		} else {

			long t = timestamp + (step * 50000);
			if (t > getDuration()) {
				t = 0;
			}

			setTimestamp(t, false);

		}

	}

	@Override
	public void previousFrame(int step) {

		if (isPlaying()) {
			pause();
		}

		long t = timestamp - (step * 50000);
		if (t < 0) {
			t = 0;
		}

		setTimestamp(t, false);

	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public void setTimestamp(long timestamp, boolean play) {
		this.timestamp = timestamp;

		if (!play) {
			showSingleFrame = true;
		}

		// TODO? This is not yet the actual time-stamp of the visible frame,
		// the application 'feels' more responsive if the time-line is updated immediately
		context.fireTimestampChanged(timestamp);

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
		// TODO
	}

	@Override
	public void resetView() {
		framePanel.resetView();
	}

	@Override
	public void exit() {
		exit = true;
	}

	@Override
	public boolean isExited() {
		return true;
	}

}
