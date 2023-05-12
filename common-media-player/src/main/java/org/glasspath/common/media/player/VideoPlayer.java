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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;

import org.glasspath.common.media.player.IVideoPlayerListener.VideoPlayerStatistics;
import org.glasspath.common.media.player.IVideoPlayerPanel.GifExportRequest;
import org.glasspath.common.media.player.IVideoPlayerPanel.Loop;
import org.glasspath.common.media.player.icons.Icons;
import org.glasspath.common.media.player.net.VideoPlayerSocketClient;
import org.glasspath.common.media.player.net.VideoPlayerSocketServer;
import org.glasspath.common.media.player.tools.FileTools;
import org.glasspath.common.media.player.tools.PlaybackTools;
import org.glasspath.common.media.player.tools.ViewTools;
import org.glasspath.common.media.video.Video;
import org.glasspath.common.swing.file.chooser.FileChooser;
import org.glasspath.common.swing.frame.FrameUtils;
import org.glasspath.common.swing.theme.Theme;

public abstract class VideoPlayer implements IVideoPlayer, ILoopHandler {

	private final boolean exitOnClose;
	private final VideoPlayerSocketClient socketClient;
	private final Preferences preferences;
	private final JFrame frame;
	private final FileTools fileTools;
	private final PlaybackTools playbackTools;
	private final ViewTools viewTools;
	private final ControlsBar controlsBar;

	private IOverlay overlay = null;
	private IVideoPlayerPanel videoPlayerPanel = null;

	private final List<IVideoPlayerListener> listeners = new ArrayList<>();

	public VideoPlayer(Video video) {
		this(video, false, false, VideoPlayerSocketServer.DEFAULT_SOCKET_SERVER_PORT_FROM);
	}

	public VideoPlayer(String path) {
		this(loadFromPath(path), true, true, VideoPlayerSocketServer.DEFAULT_SOCKET_SERVER_PORT_FROM);
	}

	public VideoPlayer(String path, int port) {
		this(loadFromPath(path), true, true, port);
	}

	public VideoPlayer(Video video, boolean exitOnClose, boolean startSocketClient, int port) {

		this.exitOnClose = exitOnClose;

		// TODO: Video player should also be capable of being the server
		if (startSocketClient) {
			socketClient = new VideoPlayerSocketClient(this, port);
		} else {
			socketClient = null;
		}

		preferences = Preferences.userNodeForPackage(this.getClass());

		frame = new JFrame();
		fileTools = new FileTools(this);
		playbackTools = new PlaybackTools(this);
		viewTools = new ViewTools(this);

		frame.setDefaultCloseOperation(exitOnClose ? JFrame.DO_NOTHING_ON_CLOSE : JFrame.DISPOSE_ON_CLOSE);
		frame.setTitle(video != null ? video.getPath() : "");
		frame.setIconImages(Icons.appIcon);
		frame.setAlwaysOnTop(ViewTools.ALWAYS_ON_TOP_PREF.get(preferences));
		frame.getContentPane().setBackground(Color.black);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.addWindowListener(new WindowAdapter() {

			private boolean inited = false;

			@Override
			public void windowActivated(WindowEvent e) {

				if (!inited) {

					frame.toFront();

					FrameUtils.loadExtendedState(frame, preferences);

					frame.addComponentListener(new ComponentAdapter() {

						@Override
						public void componentResized(ComponentEvent e) {
							FrameUtils.saveFrameDimensions(frame, preferences);
						}

						@Override
						public void componentMoved(ComponentEvent e) {
							FrameUtils.saveFrameDimensions(frame, preferences);
						}
					});

					IVideoPlayerPanel videoPlayerPanel = createVideoPlayerPanel(VideoPlayer.this, video);
					frame.getContentPane().add(videoPlayerPanel.getComponent(), BorderLayout.CENTER);
					VideoPlayer.this.videoPlayerPanel = videoPlayerPanel;
					initVideoPlayerPanel();

					videoPlayerPanel.videoPlayerShown();

					// TODO: On MacOS we need to validate to get the 1st frame to display, why?
					frame.validate();
					controlsBar.validate();

					inited = true;

				}

			}

			@Override
			public void windowClosing(WindowEvent event) {
				exit();
			}
		});

		if (Theme.isDark()) {
			frame.getRootPane().setBackground(ControlsBar.CONTROLS_BAR_BG_COLOR);
		}

		FrameUtils.loadFrameDimensions(frame, preferences, false);

		JMenuBar menuBar = new JMenuBar();
		menuBar.add(fileTools.getMenu());
		menuBar.add(playbackTools.getMenu());
		menuBar.add(viewTools.getMenu());
		frame.setJMenuBar(menuBar);

		controlsBar = new ControlsBar(this);
		frame.getContentPane().add(controlsBar, BorderLayout.SOUTH);

		addVideoPlayerListener(new VideoPlayerAdapter() {

			@Override
			public void videoOpened(String path) {
				frame.setTitle(path);
			}

			@Override
			public void playbackStateChanged(boolean playing) {
				controlsBar.updateControls();
			}

			@Override
			public void timestampChanged(long timestamp) {
				controlsBar.repaintTimelineBar();
			}

			@Override
			public void videoClosed(String path) {
				frame.setTitle("");
				clearLoopMarkers();
			}
		});

		frame.setVisible(true);

	}

	protected abstract IVideoPlayerPanel createVideoPlayerPanel(IVideoPlayer videoPlayer, Video video);

	private void initVideoPlayerPanel() {

		if (videoPlayerPanel != null) {

			videoPlayerPanel.setRepeatEnabled(PlaybackTools.REPEAT_ENABLED_PREF.get(preferences));

			videoPlayerPanel.setOverlay(overlay);
			videoPlayerPanel.setOverlayVisible(ViewTools.SHOW_OVERLAY_PREF.get(preferences));

			// TODO: Make editOverlayAction configurable?
			videoPlayerPanel.populateViewMenu(viewTools.getMenu(), null);

			videoPlayerPanel.getComponent().addMouseListener(new MouseAdapter() {

				@Override
				public void mousePressed(MouseEvent e) {

					if (SwingUtilities.isRightMouseButton(e)) {

						JMenu menu = new JMenu();
						menu.add(ViewTools.createAlwaysOnTopMenuItem(VideoPlayer.this));

						// TODO: Make editOverlayAction configurable?
						videoPlayerPanel.populateViewMenu(menu, null);

						menu.getPopupMenu().show(videoPlayerPanel.getComponent(), e.getPoint().x, e.getPoint().y);

					}

				}
			});

		}

	}

	@Override
	public JFrame getFrame() {
		return frame;
	}

	@Override
	public void setContentChanged(boolean changed) {
		// TODO?
	}

	@Override
	public IVideoPlayerPanel getVideoPlayerPanel() {
		return videoPlayerPanel;
	}

	@Override
	public ControlsBar getControlsBar() {
		return controlsBar;
	}

	@Override
	public Preferences getPreferences() {
		return preferences;
	}

	@Override
	public void setOverlay(IOverlay overlay) {
		this.overlay = overlay;
		if (videoPlayerPanel != null) {
			videoPlayerPanel.setOverlay(overlay);
		}
	}

	@Override
	public void open(Video video) {
		if (videoPlayerPanel != null) {
			videoPlayerPanel.open(video);
		}
	}

	@Override
	public void close() {
		if (videoPlayerPanel != null) {
			videoPlayerPanel.close();
		}
	}

	public boolean isExitOnClose() {
		return exitOnClose;
	}

	public void addVideoPlayerListener(IVideoPlayerListener listener) {
		listeners.add(listener);
	}

	public void removeVideoPlayerListener(IVideoPlayerListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void fireVideoOpened(String path) {
		for (IVideoPlayerListener listener : listeners) {
			listener.videoOpened(path);
		}
	}

	@Override
	public void firePlaybackStateChanged(boolean playing) {
		for (IVideoPlayerListener listener : listeners) {
			listener.playbackStateChanged(playing);
		}
	}

	@Override
	public void fireTimestampChanged(long timestamp) {
		for (IVideoPlayerListener listener : listeners) {
			listener.timestampChanged(timestamp);
		}
	}

	@Override
	public void fireLoopChanged(Loop loop) {
		for (IVideoPlayerListener listener : listeners) {
			listener.loopChanged(loop);
		}
	}

	@Override
	public void fireRecordingStateChanged(boolean recording) {
		for (IVideoPlayerListener listener : listeners) {
			listener.recordingStateChanged(recording);
		}
	}

	@Override
	public void fireStatisticsUpdated(VideoPlayerStatistics statistics) {
		for (IVideoPlayerListener listener : listeners) {
			listener.statisticsUpdated(statistics);
		}
	}

	@Override
	public void fireVideoClosed(String path) {
		for (IVideoPlayerListener listener : listeners) {
			listener.videoClosed(path);
		}
	}

	@Override
	public void fireVideoPlayerClosed() {
		for (IVideoPlayerListener listener : listeners) {
			listener.videoPlayerClosed();
		}
	}

	@Override
	public boolean isPlaying() {
		return videoPlayerPanel != null && videoPlayerPanel.isPlaying();
	}

	@Override
	public ILoopHandler getLoopHandler() {
		return this;
	}

	@Override
	public int getRate() {
		return controlsBar.getRate();
	}

	@Override
	public void setLoopFrom(long timestamp) {

		if (videoPlayerPanel != null) {

			Loop loop = videoPlayerPanel.getLoop();
			if (loop == null) {
				loop = new Loop();
				videoPlayerPanel.setLoop(loop);
			}

			loop.fromTimestamp = timestamp;

			controlsBar.getTimelineBar().repaint();
			fireLoopChanged(loop);

		}

	}

	@Override
	public void setLoopTo(long timestamp) {

		if (videoPlayerPanel != null) {

			Loop loop = videoPlayerPanel.getLoop();
			if (loop == null) {
				loop = new Loop();
				videoPlayerPanel.setLoop(loop);
			}

			loop.toTimestamp = timestamp;

			controlsBar.getTimelineBar().repaint();
			fireLoopChanged(loop);

		}

	}

	@Override
	public void clearLoopMarkers() {
		if (videoPlayerPanel != null) {
			videoPlayerPanel.setLoop(null);
			controlsBar.getTimelineBar().repaint();
			fireLoopChanged(null);
		}
	}

	@Override
	public void exportLoopToGif() {

		if (videoPlayerPanel != null && videoPlayerPanel.getLoop() != null && videoPlayerPanel.getLoop().fromTimestamp != null && videoPlayerPanel.getLoop().toTimestamp != null) {

			// TODO: Icon
			String selectedFile = FileChooser.browseForFile("gif", Icons.motionPlayBlue, true, frame, preferences, "gifExportLocation", "export.gif");
			if (selectedFile != null && selectedFile.length() > 0) {

				GifExportRequest request = new GifExportRequest(new File(selectedFile)) {

					public void progressUpdate(int progress, int total) {

					}
				};

				videoPlayerPanel.exportLoopToGif(request);

			}

		}

	}

	@Override
	public void exit() {

		fireVideoPlayerClosed();

		if (socketClient != null) {
			socketClient.exit();
		}

		FrameUtils.saveFrameDimensions(frame, preferences);

		if (videoPlayerPanel != null) {
			videoPlayerPanel.exit();
		}

		if (exitOnClose) {

			new Thread(new Runnable() {

				@Override
				public void run() {

					int waitCount = 0;
					while (videoPlayerPanel != null && !videoPlayerPanel.isExited() && waitCount < 30) {

						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						waitCount++;

					}

					shutdown();

					System.out.println("Video Player Exit");
					System.exit(0);

				}
			}).start();

		} else {
			frame.setVisible(false);
		}

	}

	protected abstract void shutdown();

	public static Video loadFromPath(String path) {

		final File file = new File(path);
		if (file.exists()) {
			return new Video(file.getName(), file.getAbsolutePath());
		} else {
			return null;
		}

	}

}
