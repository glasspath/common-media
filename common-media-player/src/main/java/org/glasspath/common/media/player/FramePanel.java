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
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.prefs.Preferences;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.glasspath.common.icons.Icons;
import org.glasspath.common.media.video.Frame;
import org.glasspath.common.swing.SwingUtils;

@SuppressWarnings("serial")
public class FramePanel extends JPanel {

	public static boolean TODO_DEBUG = false;

	public static boolean TODO_TEST_OVERLAY = true;

	public static final double MIN_SCALE = 0.1;
	public static final double MAX_SCALE = 25.0;

	private final JFrame parentFrame;
	private final Preferences preferences;
	private Frame frame = null;
	private Double customScale = null;
	private int translateX = 0;
	private int translateY = 0;
	private double scale = 1.0;
	private boolean scaleInited = false;
	private Integer rotate = null;
	private boolean flipHorizontal = false;
	private boolean flipVertical = false;

	private int x, y, w, h;
	private int wPrevious = 0, hPrevious = 0;
	private int fpsRepaintCount = 0;
	private long lastFpsMeasurement = 0;

	public FramePanel() {
		this(null, null);
	}

	public FramePanel(Preferences preferences) {
		this(null, preferences);
	}

	public FramePanel(JFrame parentFrame, Preferences preferences) {

		this.parentFrame = parentFrame;
		this.preferences = preferences;

		final MouseAdapter mouseListener = new MouseAdapter() {

			private Point mouseDragStart = null;
			private int translateXStart = 0;
			private int translateYStart = 0;
			private boolean ctrlDown;

			@Override
			public void mousePressed(MouseEvent e) {

				if (SwingUtilities.isLeftMouseButton(e)) {

					mouseDragStart = e.getPoint();
					translateXStart = translateX;
					translateYStart = translateY;

					setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

				} else if (SwingUtilities.isRightMouseButton(e)) {
					createMenu().getPopupMenu().show(FramePanel.this, e.getPoint().x, e.getPoint().y);
				}

			}

			@Override
			public void mouseDragged(MouseEvent e) {

				if (mouseDragStart != null) {

					translateX = translateXStart + (e.getPoint().x - mouseDragStart.x);
					translateY = translateYStart + (e.getPoint().y - mouseDragStart.y);

					repaint();

				}

			}

			@Override
			public void mouseReleased(MouseEvent e) {

				if (mouseDragStart != null) {

					mouseDragStart = null;
					translateXStart = 0;
					translateYStart = 0;

					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					repaint();

				}

			}

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {

				ctrlDown = e.isControlDown();
				if (ctrlDown) {
					if (e.getWheelRotation() < 0) {
						zoomIn();
					} else if (e.getWheelRotation() > 0) {
						zoomOut();
					}
				}

			}
		};

		addMouseListener(mouseListener);
		addMouseMotionListener(mouseListener);
		addMouseWheelListener(mouseListener);

	}

	public Frame getFrame() {
		return frame;
	}

	public void setFrame(Frame frame) {
		this.frame = frame;
	}

	public Double getCustomScale() {
		return customScale;
	}

	public void setCustomScale(Double customScale) {
		this.customScale = customScale;
		scaleInited = false;
		repaint();
	}

	private void initScale(int imageWidth, int imageHeight) {

		if (imageWidth > 0 && imageHeight > 0) {

			w = getWidth();
			h = getHeight();

			scale = (double) w / (double) imageWidth;
			if ((double) h / (double) imageHeight < scale) {
				scale = (double) h / (double) imageHeight;
			}

			if (scale < MIN_SCALE) {
				scale = MIN_SCALE;
			} else if (scale > MAX_SCALE) {
				scale = MAX_SCALE;
			}

		}

	}

	public void zoomIn() {

		if (customScale == null) {
			customScale = scale;
		}

		if (customScale < 2.0) {
			customScale += 0.1;
			if (customScale > 1.0 && customScale < 1.1) {
				customScale = 1.0;
			}
		} else {
			customScale += 0.25;
			if (customScale > MAX_SCALE) {
				customScale = MAX_SCALE;
			}
		}

		repaint();

	}

	public void zoomOut() {

		if (customScale == null) {
			customScale = scale;
		}

		if (customScale > 2.0) {
			customScale -= 0.25;
		} else {
			customScale -= 0.1;
			if (customScale < 1.0 && customScale > 0.9) {
				customScale = 1.0;
			}
			if (customScale < MIN_SCALE) {
				customScale = MIN_SCALE;
			}
		}

		repaint();

	}

	public void rotateLeft() {

		if (rotate == null) {
			rotate = 270;
		} else if (rotate == 270) {
			rotate = 180;
		} else if (rotate == 180) {
			rotate = 90;
		} else {
			rotate = null;
		}

		repaint();

	}

	public void rotateRight() {

		if (rotate == null) {
			rotate = 90;
		} else if (rotate == 90) {
			rotate = 180;
		} else if (rotate == 180) {
			rotate = 270;
		} else {
			rotate = null;
		}

		repaint();

	}

	public void resetView() {

		customScale = null;
		translateX = 0;
		translateY = 0;
		rotate = null;
		flipHorizontal = false;
		flipVertical = false;
		scaleInited = false;

		repaint();

	}

	@Override
	public void paint(Graphics g) {
		// super.paint(g);

		w = getWidth();
		h = getHeight();

		if (w != wPrevious || h != hPrevious) {
			if (customScale == null) {
				scaleInited = false;
			}
			wPrevious = w;
			hPrevious = h;
		}

		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

		// g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		// g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		// g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

		g2d.setClip(0, 0, w, h);

		g2d.setColor(Color.black);
		g2d.fillRect(0, 0, w, h);

		BufferedImage image;
		if (frame != null) {
			image = frame.getImage();
		} else {
			image = null;
		}

		if (image != null) {

			if (customScale != null) {
				scale = customScale;
			} else if (!scaleInited) {
				initScale(image.getWidth(), image.getHeight());
				scaleInited = true;
			}

			g2d.translate(w / 2, h / 2);
			g2d.translate(translateX, translateY);
			g2d.scale(scale * (flipHorizontal ? -1.0 : 1.0), scale * (flipVertical ? -1.0 : 1.0));
			if (rotate != null) {
				g2d.rotate(Math.toRadians(rotate));
			}

			x = -(image.getWidth() / 2);
			y = -(image.getHeight() / 2);

			g2d.drawImage(image, x, y, null);

			if (TODO_TEST_OVERLAY) {

				g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				g2d.setColor(new Color(0, 0, 0, 75));
				g2d.fill(new RoundRectangle2D.Double(x + 50, y + 50, 500, 500, 15, 15));
				g2d.setColor(Color.white);
				g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10.0F, 4.0F }, 0.0f));
				g2d.draw(new RoundRectangle2D.Double(x + 50, y + 50, 500, 500, 15, 15));

				g2d.setFont(g2d.getFont().deriveFont(35.0F));
				g2d.setStroke(new BasicStroke(2.0f));
				SwingUtils.drawString(this, g2d, "Video Player overlay test", x + 100, y + 125);

				Icons.contentCopyXLarge.paintIcon(this, g2d, x + 100, y + 175);

			}

			// g2d.scale(1.0 / scale, 1.0 / scale);
			// g2d.translate(-translateX, -translateY);
			// g2d.translate(-(w / 2), -(h / 2));

			fpsRepaintCount++;

		} else if (TODO_DEBUG) {
			System.out.println("Frame image not yet ready..");
		}

		if (TODO_DEBUG && System.currentTimeMillis() > lastFpsMeasurement + 3000) {

			if (lastFpsMeasurement != 0) {
				final double time = System.currentTimeMillis() - lastFpsMeasurement;
				final double fps = fpsRepaintCount / (time / 1000.0);
				System.out.println(fpsRepaintCount + " frames painted in " + time + " ms (" + fps + " fps)");
			}

			fpsRepaintCount = 0;
			lastFpsMeasurement = System.currentTimeMillis();

		}

	}

	public JMenu createMenu() {

		final JMenu menu = prepareMenu();

		if (parentFrame != null && preferences != null) {
			menu.add(createAlwaysOnTopMenuItem());
			menu.addSeparator();
		}

		menu.add(createZoomMenu());
		menu.add(createRotateMenu());
		menu.add(createFlipMenu());
		menu.addSeparator();
		menu.add(createResetViewMenuItem());

		return menu;

	}

	public JMenu prepareMenu() {
		return new JMenu();
	}

	public JMenuItem createAlwaysOnTopMenuItem() {

		JCheckBoxMenuItem alwaysOnTopMenuItem = new JCheckBoxMenuItem("Always on Top");
		alwaysOnTopMenuItem.setSelected(preferences.getBoolean("alwaysOnTop", false));
		alwaysOnTopMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				parentFrame.setAlwaysOnTop(!parentFrame.isAlwaysOnTop());
				preferences.putBoolean("alwaysOnTop", parentFrame.isAlwaysOnTop());
				alwaysOnTopMenuItem.setSelected(parentFrame.isAlwaysOnTop());
			}
		});

		return alwaysOnTopMenuItem;

	}

	public JMenu createZoomMenu() {

		final JMenu zoomMenu = new JMenu("Zoom");

		final ButtonGroup buttonGroup = new ButtonGroup();

		final JRadioButtonMenuItem automaticMenuItem = new JRadioButtonMenuItem("Automatic");
		buttonGroup.add(automaticMenuItem);
		zoomMenu.add(automaticMenuItem);

		final JRadioButtonMenuItem percentage25MenuItem = new JRadioButtonMenuItem("25%");
		buttonGroup.add(percentage25MenuItem);
		zoomMenu.add(percentage25MenuItem);

		final JRadioButtonMenuItem percentage50MenuItem = new JRadioButtonMenuItem("50%");
		buttonGroup.add(percentage50MenuItem);
		zoomMenu.add(percentage50MenuItem);

		final JRadioButtonMenuItem percentage75MenuItem = new JRadioButtonMenuItem("75%");
		buttonGroup.add(percentage75MenuItem);
		zoomMenu.add(percentage75MenuItem);

		final JRadioButtonMenuItem percentage100MenuItem = new JRadioButtonMenuItem("100%");
		buttonGroup.add(percentage100MenuItem);
		zoomMenu.add(percentage100MenuItem);

		final JRadioButtonMenuItem percentage125MenuItem = new JRadioButtonMenuItem("125%");
		buttonGroup.add(percentage125MenuItem);
		zoomMenu.add(percentage125MenuItem);

		final JRadioButtonMenuItem percentage150MenuItem = new JRadioButtonMenuItem("150%");
		buttonGroup.add(percentage150MenuItem);
		zoomMenu.add(percentage150MenuItem);

		final JRadioButtonMenuItem percentage175MenuItem = new JRadioButtonMenuItem("175%");
		buttonGroup.add(percentage175MenuItem);
		zoomMenu.add(percentage175MenuItem);

		final JRadioButtonMenuItem percentage200MenuItem = new JRadioButtonMenuItem("200%");
		buttonGroup.add(percentage200MenuItem);
		zoomMenu.add(percentage200MenuItem);

		final class CustomScaleUpdater {

			private boolean updating = false;

			public void setCustomScale(Double customScale) {

				if (!updating) {

					updating = true;

					FramePanel.this.setCustomScale(customScale);

					if (customScale == null) {
						automaticMenuItem.setSelected(true);
					} else {
						percentage25MenuItem.setSelected(customScale == 0.25);
						percentage50MenuItem.setSelected(customScale == 0.5);
						percentage75MenuItem.setSelected(customScale == 0.75);
						percentage100MenuItem.setSelected(customScale == 1.0);
						percentage125MenuItem.setSelected(customScale == 1.25);
						percentage150MenuItem.setSelected(customScale == 1.5);
						percentage175MenuItem.setSelected(customScale == 1.75);
						percentage200MenuItem.setSelected(customScale == 2.0);
					}

					updating = false;

				}

			}
		}
		final CustomScaleUpdater customScaleUpdater = new CustomScaleUpdater();

		automaticMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				customScaleUpdater.setCustomScale(null);
			}
		});
		percentage25MenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				customScaleUpdater.setCustomScale(0.25);
			}
		});
		percentage50MenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				customScaleUpdater.setCustomScale(0.5);
			}
		});
		percentage75MenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				customScaleUpdater.setCustomScale(0.75);
			}
		});
		percentage100MenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				customScaleUpdater.setCustomScale(1.0);
			}
		});
		percentage125MenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				customScaleUpdater.setCustomScale(1.25);
			}
		});
		percentage150MenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				customScaleUpdater.setCustomScale(1.5);
			}
		});
		percentage175MenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				customScaleUpdater.setCustomScale(1.75);
			}
		});
		percentage200MenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				customScaleUpdater.setCustomScale(2.0);
			}
		});

		zoomMenu.addMenuListener(new MenuListener() {

			@Override
			public void menuSelected(MenuEvent e) {
				customScaleUpdater.setCustomScale(getCustomScale());
			}

			@Override
			public void menuDeselected(MenuEvent e) {

			}

			@Override
			public void menuCanceled(MenuEvent e) {

			}
		});

		return zoomMenu;

	}

	public JMenu createRotateMenu() {

		final JMenu rotateMenu = new JMenu("Rotate");

		final JMenuItem rotateLeftMenuItem = new JMenuItem("90° Left");
		rotateMenu.add(rotateLeftMenuItem);
		rotateLeftMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				rotateLeft();
			}
		});

		final JMenuItem rotateRightMenuItem = new JMenuItem("90° Right");
		rotateMenu.add(rotateRightMenuItem);
		rotateRightMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				rotateRight();
			}
		});

		return rotateMenu;

	}

	public JMenu createFlipMenu() {

		final JMenu flipMenu = new JMenu("Flip");

		final JCheckBoxMenuItem flipHorizontalMenuItem = new JCheckBoxMenuItem("Horizontal");
		flipHorizontalMenuItem.setSelected(flipHorizontal);
		flipMenu.add(flipHorizontalMenuItem);
		flipHorizontalMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				flipHorizontal = !flipHorizontal;
				flipHorizontalMenuItem.setSelected(flipHorizontal);
				repaint();
			}
		});

		final JCheckBoxMenuItem flipVerticalMenuItem = new JCheckBoxMenuItem("Vertical");
		flipVerticalMenuItem.setSelected(flipVertical);
		flipMenu.add(flipVerticalMenuItem);
		flipVerticalMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				flipVertical = !flipVertical;
				flipVerticalMenuItem.setSelected(flipVertical);
				repaint();
			}
		});

		return flipMenu;

	}

	public JMenuItem createResetViewMenuItem() {

		JMenuItem resetViewMenuItem = new JMenuItem("Reset View");
		resetViewMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, KeyEvent.CTRL_DOWN_MASK));
		resetViewMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				resetView();
			}
		});

		return resetViewMenuItem;

	}

}
