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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.GeneralPath;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import org.glasspath.common.icons.Icons;
import org.glasspath.common.media.player.IVideoPlayerPanel.GifExportRequest;
import org.glasspath.common.media.player.IVideoPlayerPanel.Loop;
import org.glasspath.common.media.video.Video;
import org.glasspath.common.os.OsUtils;
import org.glasspath.common.swing.color.ColorUtils;
import org.glasspath.common.swing.file.chooser.FileChooser;
import org.glasspath.common.swing.frame.FrameUtils;
import org.glasspath.common.swing.theme.Theme;

@SuppressWarnings("serial")
public abstract class VideoPlayer implements IVideoPlayer {

	public static boolean TODO_TEST_GLASS_MODE = false;

	public static final int STEP_FAST_FORWARD_REVERSE = 20;
	public static final Color CONTROLS_BAR_BG_COLOR;
	public static final Color CONTROLS_BAR_FG_COLOR;
	public static final Stroke CONTROLS_BAR_FG_STROKE;
	public static final Color TIMELINE_BAR_BG_COLOR;
	public static final Color TIMELINE_BAR_FG_COLOR;
	public static final Color TIMELINE_BAR_LOOP_FROM_COLOR;
	public static final Color TIMELINE_BAR_LOOP_TO_COLOR;
	static {
		if (Theme.isDark()) {
			CONTROLS_BAR_BG_COLOR = new Color(30, 30, 30);
			CONTROLS_BAR_FG_COLOR = new Color(150, 150, 150);
			CONTROLS_BAR_FG_STROKE = new BasicStroke(1.0F);
			TIMELINE_BAR_BG_COLOR = Color.black;
			TIMELINE_BAR_FG_COLOR = new Color(60, 150, 225);
			TIMELINE_BAR_LOOP_FROM_COLOR = new Color(110, 200, 255);
			TIMELINE_BAR_LOOP_TO_COLOR = new Color(110, 200, 255);
		} else {
			CONTROLS_BAR_BG_COLOR = new Color(242, 242, 242);
			CONTROLS_BAR_FG_COLOR = new Color(125, 125, 125);
			CONTROLS_BAR_FG_STROKE = new BasicStroke(1.0F);
			TIMELINE_BAR_BG_COLOR = new Color(210, 210, 210);
			TIMELINE_BAR_FG_COLOR = new Color(60, 150, 225);
			TIMELINE_BAR_LOOP_FROM_COLOR = new Color(110, 200, 255);
			TIMELINE_BAR_LOOP_TO_COLOR = new Color(110, 200, 255);
		}
	}

	private final boolean exitOnClose;
	private final VideoPlayerSocketClient socketClient;
	private final Preferences preferences;
	private final JFrame frame;
	private final JCheckBoxMenuItem repeatEnabledMenuItem;
	private final JMenuItem playPauseMenuItem;
	private final JMenuItem clearLoopMarkersMenuItem;
	private final JMenuItem exportLoopToGifMenuItem;
	private final ControlsBar controlsBar;

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

		if (startSocketClient) {
			socketClient = new VideoPlayerSocketClient(this, port);
		} else {
			socketClient = null;
		}

		preferences = Preferences.userNodeForPackage(this.getClass());

		frame = new JFrame();
		frame.setDefaultCloseOperation(exitOnClose ? JFrame.DO_NOTHING_ON_CLOSE : JFrame.DISPOSE_ON_CLOSE);
		frame.setTitle(video.getPath());
		frame.setIconImage(Icons.squareEditOutline.getImage()); // TODO
		frame.setAlwaysOnTop(preferences.getBoolean("alwaysOnTop", false));
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
					if (TODO_TEST_GLASS_MODE) {

						frame.setGlassPane(videoPlayerPanel.getComponent());
						frame.getGlassPane().setVisible(true);

					} else {
						frame.getContentPane().add(videoPlayerPanel.getComponent(), BorderLayout.CENTER);
					}
					VideoPlayer.this.videoPlayerPanel = videoPlayerPanel;
					initVideoPlayerPanel();

					videoPlayerPanel.videoPlayerShown();

					updateLoopMenuItems();

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

		System.out.println("Constructor, dark: " + Theme.isDark());
		if (Theme.isDark()) {
			frame.getRootPane().setBackground(CONTROLS_BAR_BG_COLOR);
		}

		FrameUtils.loadFrameDimensions(frame, preferences, false);

		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);

		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);

		JMenuItem exitMenuItem = new JMenuItem("Exit");
		fileMenu.add(exitMenuItem);
		exitMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				exit();
			}
		});

		JMenu playbackMenu = new JMenu("Playback");
		menuBar.add(playbackMenu);

		repeatEnabledMenuItem = new JCheckBoxMenuItem("Repeat");
		playbackMenu.add(repeatEnabledMenuItem);
		repeatEnabledMenuItem.setSelected(preferences.getBoolean("repeatEnabled", true));
		repeatEnabledMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (videoPlayerPanel != null) {
					videoPlayerPanel.setRepeatEnabled(repeatEnabledMenuItem.isSelected());
				}
				preferences.putBoolean("repeatEnabled", repeatEnabledMenuItem.isSelected());
			}
		});

		playPauseMenuItem = new JMenuItem("Play");
		playbackMenu.add(playPauseMenuItem);
		playPauseMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
		playPauseMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (videoPlayerPanel != null) {
					videoPlayerPanel.togglePlaying();
				}
			}
		});

		playbackMenu.addSeparator();

		JMenuItem loopFromMenuItem = new JMenuItem("Loop From"); // TODO
		playbackMenu.add(loopFromMenuItem);
		loopFromMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.SHIFT_DOWN_MASK));
		loopFromMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (videoPlayerPanel != null) {
					setLoopFrom(videoPlayerPanel.getTimestamp());
				}
			}
		});

		JMenuItem loopToMenuItem = new JMenuItem("Loop To"); // TODO
		playbackMenu.add(loopToMenuItem);
		loopToMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.SHIFT_DOWN_MASK));
		loopToMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (videoPlayerPanel != null) {
					setLoopTo(videoPlayerPanel.getTimestamp());
				}
			}
		});

		clearLoopMarkersMenuItem = new JMenuItem("Clear Loop Markers"); // TODO
		playbackMenu.add(clearLoopMarkersMenuItem);
		clearLoopMarkersMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.SHIFT_DOWN_MASK));
		clearLoopMarkersMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				clearLoopMarkers();
			}
		});

		exportLoopToGifMenuItem = new JMenuItem("Export Loop to Gif"); // TODO
		playbackMenu.add(exportLoopToGifMenuItem);
		exportLoopToGifMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.SHIFT_DOWN_MASK));
		exportLoopToGifMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				exportLoopToGif();
			}
		});

		JMenu viewMenu = new JMenu("View");
		menuBar.add(viewMenu);

		JCheckBoxMenuItem alwaysOnTopMenuItem = new JCheckBoxMenuItem("Always on Top"); // TODO
		viewMenu.add(alwaysOnTopMenuItem);
		alwaysOnTopMenuItem.setSelected(preferences.getBoolean("alwaysOnTop", false));
		alwaysOnTopMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				frame.setAlwaysOnTop(alwaysOnTopMenuItem.isSelected());
				preferences.putBoolean("alwaysOnTop", alwaysOnTopMenuItem.isSelected());
			}
		});
		frame.setAlwaysOnTop(alwaysOnTopMenuItem.isSelected());

		viewMenu.addSeparator();

		JMenuItem resetViewMenuItem = new JMenuItem("Reset View");
		viewMenu.add(resetViewMenuItem);
		resetViewMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, OsUtils.CTRL_OR_CMD_MASK));
		resetViewMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (videoPlayerPanel != null) {
					videoPlayerPanel.resetView();
				}
			}
		});

		// TODO
		JCheckBoxMenuItem overlayMenuItem = new JCheckBoxMenuItem("Show overlay");
		viewMenu.add(overlayMenuItem);
		overlayMenuItem.setSelected(FramePanel.TODO_TEST_OVERLAY);
		overlayMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_9, OsUtils.CTRL_OR_CMD_MASK));
		overlayMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				FramePanel.TODO_TEST_OVERLAY = overlayMenuItem.isSelected();
				if (videoPlayerPanel != null) {
					videoPlayerPanel.getComponent().repaint();
				}
			}
		});

		controlsBar = new ControlsBar(this);
		frame.getContentPane().add(controlsBar, BorderLayout.SOUTH);

		updateLoopMenuItems();

		frame.setVisible(true);

	}

	protected abstract IVideoPlayerPanel createVideoPlayerPanel(IVideoPlayer videoPlayer, Video video);

	private void initVideoPlayerPanel() {
		if (videoPlayerPanel != null) {
			videoPlayerPanel.setRepeatEnabled(repeatEnabledMenuItem.isSelected());
		}
	}

	@Override
	public JFrame getFrame() {
		return frame;
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
	public void fireVideoClosed(String path) {
		for (IVideoPlayerListener listener : listeners) {
			listener.videoClosed(path);
		}
	}

	@Override
	public void fireTimestampChanged(long timestamp) {
		for (IVideoPlayerListener listener : listeners) {
			listener.timestampChanged(timestamp);
		}
	}

	@Override
	public void firePlaybackEnded() {
		for (IVideoPlayerListener listener : listeners) {
			listener.playbackEnded();
		}
	}

	@Override
	public void fireVideoPlayerClosed() {
		for (IVideoPlayerListener listener : listeners) {
			listener.videoPlayerClosed();
		}
	}

	@Override
	public void updateControls() {

		if (videoPlayerPanel != null) {

			playPauseMenuItem.setText(videoPlayerPanel.isPlaying() ? "Pause" : "Play"); // TODO

		}

		controlsBar.updateControls();

	}

	@Override
	public int getRate() {
		return controlsBar.getRate();
	}

	@Override
	public void repaintTimelineBar() {
		controlsBar.repaintTimelineBar();
	}

	private void updateLoopMenuItems() {
		clearLoopMarkersMenuItem.setEnabled(videoPlayerPanel != null && videoPlayerPanel.getLoop() != null);
		exportLoopToGifMenuItem.setEnabled(videoPlayerPanel != null && videoPlayerPanel.getLoop() != null && videoPlayerPanel.getLoop().fromTimestamp != null && videoPlayerPanel.getLoop().toTimestamp != null);
	}

	private void setLoopFrom(long timestamp) {

		if (videoPlayerPanel != null) {

			Loop loop = videoPlayerPanel.getLoop();
			if (loop == null) {
				loop = new Loop();
				videoPlayerPanel.setLoop(loop);
			}

			loop.fromTimestamp = timestamp;

			controlsBar.timelineBar.repaint();

		}

		updateLoopMenuItems();

	}

	private void setLoopTo(long timestamp) {

		if (videoPlayerPanel != null) {

			Loop loop = videoPlayerPanel.getLoop();
			if (loop == null) {
				loop = new Loop();
				videoPlayerPanel.setLoop(loop);
			}

			loop.toTimestamp = timestamp;

			controlsBar.timelineBar.repaint();

		}

		updateLoopMenuItems();

	}

	private void clearLoopMarkers() {

		if (videoPlayerPanel != null) {
			videoPlayerPanel.setLoop(null);
			controlsBar.timelineBar.repaint();
		}

		updateLoopMenuItems();

	}

	private void exportLoopToGif() {

		if (videoPlayerPanel != null && videoPlayerPanel.getLoop() != null && videoPlayerPanel.getLoop().fromTimestamp != null && videoPlayerPanel.getLoop().toTimestamp != null) {

			// TODO: Icon
			String selectedFile = FileChooser.browseForFile("gif", Icons.squareEditOutline, true, frame, preferences, "gifExportLocation", "export.gif");
			if (selectedFile != null && selectedFile.length() > 0) {

				GifExportRequest request = new GifExportRequest(new File(selectedFile)) {

					public void progressUpdate(int progress, int total) {

					}
				};

				videoPlayerPanel.exportLoopToGif(request);

			}

		}

	}

	protected void exit() {

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

	public class ControlsBar extends JPanel {

		private final VideoTimelineBar timelineBar;
		private final JButton playButton;
		private final JSpinner rateSpinner;

		private final GeneralPath gp = new GeneralPath();

		public ControlsBar(IVideoPlayer context) {

			// setDoubleBuffered(false);
			setBackground(CONTROLS_BAR_BG_COLOR);

			GridBagLayout layout = new GridBagLayout();
			layout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0 };
			layout.rowHeights = new int[] { 1, 4, 25, 5 };
			layout.columnWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.1, 0.0 };
			layout.columnWidths = new int[] { 10, 0, 5, 25, 1, 25, 1, 25, 1, 25, 1, 25, 8, 50, 10, 25, 15 };
			setLayout(layout);

			timelineBar = new VideoTimelineBar(context);
			add(timelineBar, new GridBagConstraints(15, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

			JButton fastReverseButton = new JButton() {

				@Override
				public void paint(Graphics g) {
					super.paint(g);

					Graphics2D g2d = (Graphics2D) g;
					g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

					if (isBorderPainted()) {
						g2d.setColor(TIMELINE_BAR_FG_COLOR);
					} else {
						g2d.setColor(CONTROLS_BAR_FG_COLOR);
					}
					g2d.setStroke(CONTROLS_BAR_FG_STROKE);

					gp.reset();
					gp.moveTo(17, 8);
					gp.lineTo(12, 12);
					gp.lineTo(17, 16);
					g2d.draw(gp);

					gp.reset();
					gp.moveTo(12, 8);
					gp.lineTo(7, 12);
					gp.lineTo(12, 16);
					g2d.draw(gp);

				}
			};
			initButton(fastReverseButton);
			add(fastReverseButton, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(1, 0, 0, 0), 0, 0));
			fastReverseButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (videoPlayerPanel != null) {
						videoPlayerPanel.previousFrame(STEP_FAST_FORWARD_REVERSE);
					}
				}
			});

			JButton reverseButton = new JButton() {

				@Override
				public void paint(Graphics g) {
					super.paint(g);

					Graphics2D g2d = (Graphics2D) g;
					g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

					if (isBorderPainted()) {
						g2d.setColor(TIMELINE_BAR_FG_COLOR);
					} else {
						g2d.setColor(CONTROLS_BAR_FG_COLOR);
					}
					g2d.setStroke(CONTROLS_BAR_FG_STROKE);

					gp.reset();
					gp.moveTo(14, 8);
					gp.lineTo(9, 12);
					gp.lineTo(14, 16);
					g2d.draw(gp);

				}
			};
			initButton(reverseButton);
			add(reverseButton, new GridBagConstraints(5, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(1, 0, 0, 0), 0, 0));
			reverseButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (videoPlayerPanel != null) {
						videoPlayerPanel.previousFrame(1);
					}
				}
			});

			playButton = new JButton() {

				@Override
				public void paint(Graphics g) {
					super.paint(g);

					Graphics2D g2d = (Graphics2D) g;
					g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

					if (isBorderPainted()) {
						g2d.setColor(TIMELINE_BAR_FG_COLOR);
					} else {
						g2d.setColor(CONTROLS_BAR_FG_COLOR);
					}
					g2d.setStroke(CONTROLS_BAR_FG_STROKE);

					if (videoPlayerPanel != null && videoPlayerPanel.isPlaying()) {

						gp.reset();
						gp.moveTo(9, 8);
						gp.lineTo(9, 16);
						g2d.draw(gp);

						gp.reset();
						gp.moveTo(14, 8);
						gp.lineTo(14, 16);
						g2d.draw(gp);

					} else {

						gp.reset();
						gp.moveTo(9, 8);
						gp.lineTo(14, 12);
						gp.lineTo(9, 16);
						gp.closePath();
						g2d.draw(gp);

					}

				}
			};
			initButton(playButton);
			add(playButton, new GridBagConstraints(7, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(1, 0, 0, 0), 0, 0));
			playButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (videoPlayerPanel != null) {
						videoPlayerPanel.togglePlaying();
					}
				}
			});

			JButton forwardButton = new JButton() {

				@Override
				public void paint(Graphics g) {
					super.paint(g);

					Graphics2D g2d = (Graphics2D) g;
					g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

					if (isBorderPainted()) {
						g2d.setColor(TIMELINE_BAR_FG_COLOR);
					} else {
						g2d.setColor(CONTROLS_BAR_FG_COLOR);
					}
					g2d.setStroke(CONTROLS_BAR_FG_STROKE);

					gp.reset();
					gp.moveTo(9, 8);
					gp.lineTo(14, 12);
					gp.lineTo(9, 16);
					g2d.draw(gp);

				}
			};
			initButton(forwardButton);
			add(forwardButton, new GridBagConstraints(9, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(1, 0, 0, 0), 0, 0));
			forwardButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (videoPlayerPanel != null) {
						videoPlayerPanel.nextFrame(1);
					}
				}
			});

			JButton fastForwardButton = new JButton() {

				@Override
				public void paint(Graphics g) {
					super.paint(g);

					Graphics2D g2d = (Graphics2D) g;
					g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

					if (isBorderPainted()) {
						g2d.setColor(TIMELINE_BAR_FG_COLOR);
					} else {
						g2d.setColor(CONTROLS_BAR_FG_COLOR);
					}
					g2d.setStroke(CONTROLS_BAR_FG_STROKE);

					gp.reset();
					gp.moveTo(12, 8);
					gp.lineTo(17, 12);
					gp.lineTo(12, 16);
					g2d.draw(gp);

					gp.reset();
					gp.moveTo(7, 8);
					gp.lineTo(12, 12);
					gp.lineTo(7, 16);
					g2d.draw(gp);

				}
			};
			initButton(fastForwardButton);
			add(fastForwardButton, new GridBagConstraints(11, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(1, 0, 0, 0), 0, 0));
			fastForwardButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (videoPlayerPanel != null) {
						videoPlayerPanel.nextFrame(STEP_FAST_FORWARD_REVERSE);
					}
				}
			});

			rateSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 200, 1));
			rateSpinner.setEnabled(false);
			initSpinner(rateSpinner);
			add(rateSpinner, new GridBagConstraints(13, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		}

		private void updateControls() {
			playButton.repaint();
			rateSpinner.setEnabled(videoPlayerPanel != null && !videoPlayerPanel.isPlaying());
		}

		public int getRate() {
			return ((Number) rateSpinner.getValue()).intValue();
		}

		public void repaintTimelineBar() {
			timelineBar.repaint();
		}

		private void initButton(JButton button) {

			button.setFocusable(false);
			// button.setBorder(BorderFactory.createEmptyBorder());
			button.setMargin(new Insets(0, 0, 0, 0));
			button.setIconTextGap(0);
			button.setHorizontalAlignment(JButton.CENTER);
			button.setBorderPainted(false);
			button.setFocusPainted(false);
			button.setContentAreaFilled(false);

			button.addMouseListener(new MouseAdapter() {

				@Override
				public void mousePressed(MouseEvent e) {
					timelineBar.requestFocusInWindow();
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					button.setBorderPainted(true);
					button.setContentAreaFilled(true);
					button.repaint();
				}

				@Override
				public void mouseExited(MouseEvent e) {
					button.setBorderPainted(false);
					button.setContentAreaFilled(false);
					button.repaint();
				}
			});

		}

		private void initSpinner(JSpinner spinner) {

			// spinner.putClientProperty("FlatLaf.style", "buttonSeparatorWidth: 0");
			if (Theme.isDark()) {
				spinner.putClientProperty("FlatLaf.style", "arrowType: chevron"
				+ "; background: " + ColorUtils.toHex(CONTROLS_BAR_BG_COLOR)
				+ "; disabledBackground: " + ColorUtils.toHex(CONTROLS_BAR_BG_COLOR)
				+ "; buttonSeparatorColor: #444444"
				+ "; buttonDisabledSeparatorColor: #000000"
				+ "; buttonBackground: " + ColorUtils.toHex(CONTROLS_BAR_BG_COLOR)
				+ "; borderColor: #444444"
				+ "; disabledBorderColor: #000000"
				);
			} else {
				spinner.putClientProperty("FlatLaf.style", "arrowType: chevron");
			}
			spinner.setForeground(TIMELINE_BAR_FG_COLOR);

		}

	}

	public class VideoTimelineBar extends JComponent {

		private final IVideoPlayer context;
		private int x, y, w, h;

		public VideoTimelineBar(IVideoPlayer context) {

			this.context = context;

			setFocusable(true);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // TODO
			addMouseListener(new MouseAdapter() {

				@Override
				public void mousePressed(MouseEvent e) {

					requestFocusInWindow();

					if (SwingUtilities.isRightMouseButton(e)) {
						createMenu(e.getX()).getPopupMenu().show(VideoTimelineBar.this, e.getX(), e.getY());
					} else {

						if (context.getVideoPlayerPanel() != null) {

							boolean playing = context.getVideoPlayerPanel().isPlaying();

							if (playing) {
								context.getVideoPlayerPanel().pause();
							}

							context.getVideoPlayerPanel().setTimestamp(getTimestampAtLocation(e.getX()), playing);

						}

					}

				}
			});

		}

		private long getTimestampAtLocation(int x) {
			if (context.getVideoPlayerPanel() != null) {
				return (long) (((double) x / (double) getWidth()) * (double) context.getVideoPlayerPanel().getDuration());
			} else {
				return -1;
			}
		}

		private JMenu createMenu(int x) {

			JMenu menu = new JMenu();

			JMenuItem loopFromMenuItem = new JMenuItem("Loop From"); // TODO
			menu.add(loopFromMenuItem);
			loopFromMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.SHIFT_DOWN_MASK));
			loopFromMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					setLoopFrom(getTimestampAtLocation(x));
				}
			});

			JMenuItem loopToMenuItem = new JMenuItem("Loop To"); // TODO
			menu.add(loopToMenuItem);
			loopToMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.SHIFT_DOWN_MASK));
			loopToMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					setLoopTo(getTimestampAtLocation(x));
				}
			});

			JMenuItem clearLoopMarkersMenuItem = new JMenuItem("Clear Loop Markers"); // TODO
			menu.add(clearLoopMarkersMenuItem);
			clearLoopMarkersMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.SHIFT_DOWN_MASK));
			clearLoopMarkersMenuItem.setEnabled(videoPlayerPanel != null && videoPlayerPanel.getLoop() != null);
			clearLoopMarkersMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					clearLoopMarkers();
				}
			});

			JMenuItem exportToGifMenuItem = new JMenuItem("Export Loop to Gif"); // TODO
			menu.add(exportToGifMenuItem);
			exportToGifMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.SHIFT_DOWN_MASK));
			exportToGifMenuItem.setEnabled(videoPlayerPanel != null && videoPlayerPanel.getLoop() != null && videoPlayerPanel.getLoop().fromTimestamp != null && videoPlayerPanel.getLoop().toTimestamp != null);
			exportToGifMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					exportLoopToGif();
				}
			});

			return menu;

		}

		@Override
		public void paint(Graphics g) {
			// super.paint(g);

			Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			w = getWidth();
			h = getHeight();

			g2d.setColor(CONTROLS_BAR_BG_COLOR);
			g2d.fillRect(0, 0, w, h);

			if (videoPlayerPanel != null) {

				long duration = videoPlayerPanel.getDuration();
				if (duration > 0) {

					g2d.setColor(TIMELINE_BAR_BG_COLOR);
					g2d.fillRect(0, y, w, 5);

					Loop loop = videoPlayerPanel.getLoop();
					if (loop != null) {

						if (loop.fromTimestamp != null) {

							x = (int) (w * ((double) loop.fromTimestamp / (double) duration));
							g2d.setColor(TIMELINE_BAR_LOOP_FROM_COLOR);
							g2d.fillRect(x - 3, 3, 3, h - 6);

						}

						if (loop.toTimestamp != null) {

							x = (int) (w * ((double) loop.toTimestamp / (double) duration));
							g2d.setColor(TIMELINE_BAR_LOOP_TO_COLOR);
							g2d.fillRect(x, 3, 3, h - 6);

						}

					}

					x = (int) (w * ((double) videoPlayerPanel.getTimestamp() / (double) duration));
					y = (h / 2) - 3;

					g2d.setColor(TIMELINE_BAR_FG_COLOR);
					g2d.fillRect(0, y, x, 5);
					// g2d.fillOval(x - 4, y - 3, 11, 11);

				}

			}

		}

	}

}
