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
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import org.glasspath.common.media.player.IVideoPlayerPanel.Loop;
import org.glasspath.common.swing.color.ColorUtils;
import org.glasspath.common.swing.theme.Theme;

@SuppressWarnings("serial")
public class ControlsBar extends JPanel {

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

	private final IVideoPlayer context;
	private final VideoTimelineBar timelineBar;
	private final JButton playButton;
	private final JSpinner rateSpinner;

	private final GeneralPath gp = new GeneralPath();

	public ControlsBar(IVideoPlayer context) {

		this.context = context;

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
				if (context.getVideoPlayerPanel() != null) {
					context.getVideoPlayerPanel().previousFrame(STEP_FAST_FORWARD_REVERSE);
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
				if (context.getVideoPlayerPanel() != null) {
					context.getVideoPlayerPanel().previousFrame(1);
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

				if (context.isPlaying()) {

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
				if (context.getVideoPlayerPanel() != null) {
					context.getVideoPlayerPanel().togglePlaying();
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
				if (context.getVideoPlayerPanel() != null) {
					context.getVideoPlayerPanel().nextFrame(1);
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
				if (context.getVideoPlayerPanel() != null) {
					context.getVideoPlayerPanel().nextFrame(STEP_FAST_FORWARD_REVERSE);
				}
			}
		});

		rateSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 200, 1));
		rateSpinner.setEnabled(false);
		initSpinner(rateSpinner);
		add(rateSpinner, new GridBagConstraints(13, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

	}

	public VideoTimelineBar getTimelineBar() {
		return timelineBar;
	}

	protected void updateControls() {
		playButton.repaint();
		rateSpinner.setEnabled(!context.isPlaying());
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
					+ "; disabledBorderColor: #000000");
		} else {
			spinner.putClientProperty("FlatLaf.style", "arrowType: chevron");
		}
		spinner.setForeground(TIMELINE_BAR_FG_COLOR);

	}

	public static class VideoTimelineBar extends JComponent {

		private final IVideoPlayer context;
		private final RoundRectangle2D.Float roundRect = new RoundRectangle2D.Float();
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
						createMenu(context, e.getX()).getPopupMenu().show(VideoTimelineBar.this, e.getX(), e.getY());
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

		private JMenu createMenu(IVideoPlayer context, int x) {

			JMenu menu = new JMenu();

			IVideoPlayerPanel videoPlayerPanel = context.getVideoPlayerPanel();
			ILoopHandler loopHandler = context.getLoopHandler();
			if (videoPlayerPanel != null && loopHandler != null) {

				JMenuItem loopFromMenuItem = new JMenuItem("Loop From");
				menu.add(loopFromMenuItem);
				loopFromMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.SHIFT_DOWN_MASK));
				loopFromMenuItem.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						loopHandler.setLoopFrom(getTimestampAtLocation(x));
					}
				});

				JMenuItem loopToMenuItem = new JMenuItem("Loop To");
				menu.add(loopToMenuItem);
				loopToMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.SHIFT_DOWN_MASK));
				loopToMenuItem.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						loopHandler.setLoopTo(getTimestampAtLocation(x));
					}
				});

				JMenuItem clearLoopMarkersMenuItem = new JMenuItem("Clear Loop Markers");
				menu.add(clearLoopMarkersMenuItem);
				clearLoopMarkersMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.SHIFT_DOWN_MASK));
				clearLoopMarkersMenuItem.setEnabled(videoPlayerPanel.getLoop() != null);
				clearLoopMarkersMenuItem.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						loopHandler.clearLoopMarkers();
					}
				});

				JMenuItem exportToGifMenuItem = new JMenuItem("Export Loop to Gif");
				menu.add(exportToGifMenuItem);
				exportToGifMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.SHIFT_DOWN_MASK));
				exportToGifMenuItem.setEnabled(videoPlayerPanel.getLoop() != null && videoPlayerPanel.getLoop().fromTimestamp != null && videoPlayerPanel.getLoop().toTimestamp != null);
				exportToGifMenuItem.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						loopHandler.exportLoopToGif();
					}
				});

			}

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

			IVideoPlayerPanel videoPlayerPanel = context.getVideoPlayerPanel();
			if (videoPlayerPanel != null) {

				long duration = videoPlayerPanel.getDuration();
				if (duration > 0) {

					g2d.setColor(TIMELINE_BAR_BG_COLOR);
					roundRect.setRoundRect(0, y, w, 5, 5, 5);
					g2d.fill(roundRect);
					// g2d.fillRect(0, y, w, 5);

					Loop loop = videoPlayerPanel.getLoop();
					if (loop != null) {

						if (loop.fromTimestamp != null) {

							x = (int) (w * ((double) loop.fromTimestamp / (double) duration));
							g2d.setColor(TIMELINE_BAR_LOOP_FROM_COLOR);
							g2d.fillRect(x - 1, 3, 1, h - 6);

						}

						if (loop.toTimestamp != null) {

							x = (int) (w * ((double) loop.toTimestamp / (double) duration));
							g2d.setColor(TIMELINE_BAR_LOOP_TO_COLOR);
							g2d.fillRect(x, 3, 1, h - 6);

						}

					}

					x = (int) (w * ((double) videoPlayerPanel.getTimestamp() / (double) duration));
					y = (h / 2) - 3;

					g2d.setColor(TIMELINE_BAR_FG_COLOR);
					roundRect.setRoundRect(0, y, x, 5, 5, 5);
					g2d.fill(roundRect);
					// g2d.fillRect(0, y, x, 5);
					// g2d.fillOval(x - 4, y - 3, 11, 11);

				}

			}

		}

	}

}
