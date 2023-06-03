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
package org.glasspath.common.media.mfsdk;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.glasspath.common.media.video.Resolution;

@SuppressWarnings("serial")
public abstract class EvrCanvasPanel extends JPanel {

	public static final double MIN_SCALE = 1.0;
	public static final double MAX_SCALE = 25.0;

	protected final EvrCanvas evrCanvas;
	private Resolution resolution = Resolution.W1280_H720;
	private int translateX = 0;
	private int translateY = 0;
	private double scale = 1.0;
	private Double customScale = null;

	public EvrCanvasPanel() {

		setBackground(Color.black);
		setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		setLayout(new BorderLayout());

		evrCanvas = new EvrCanvas();
		evrCanvas.setMinimumSize(new Dimension(100, 100));
		add(evrCanvas, BorderLayout.CENTER);
		// evrCanvas.setIgnoreRepaint(true);
		// evrCanvas.setMixingCutoutShape(new Rectangle());
		evrCanvas.setVisible(false);

		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {
				updateVideoPosition();
			}
		});

		MouseAdapter mouseListener = new MouseAdapter() {

			private Point mouseDragStart = null;
			private int translateXStart = 0;
			private int translateYStart = 0;
			private boolean ctrlDown;

			private long lastEvent = 0;

			@Override
			public void mousePressed(MouseEvent e) {

				if (SwingUtilities.isLeftMouseButton(e)) {

					mouseDragStart = e.getPoint();
					translateXStart = translateX;
					translateYStart = translateY;

					setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

				}

			}

			@Override
			public void mouseDragged(MouseEvent e) {

				// TODO: This is a quick hack to reduce the event rate
				if (System.currentTimeMillis() < lastEvent + 25) {
					return;
				}
				lastEvent = System.currentTimeMillis();

				if (mouseDragStart != null) {

					translateX = translateXStart + (e.getPoint().x - mouseDragStart.x);
					translateY = translateYStart + (e.getPoint().y - mouseDragStart.y);

					updateVideoPosition();

				}

			}

			@Override
			public void mouseReleased(MouseEvent e) {

				if (mouseDragStart != null) {

					mouseDragStart = null;
					translateXStart = 0;
					translateYStart = 0;

					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					updateVideoPosition();

				}

			}

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {

				// TODO: This is a quick hack to reduce the event rate
				if (System.currentTimeMillis() < lastEvent + 25) {
					return;
				}
				lastEvent = System.currentTimeMillis();

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

		evrCanvas.addMouseListener(mouseListener);
		evrCanvas.addMouseMotionListener(mouseListener);
		evrCanvas.addMouseWheelListener(mouseListener);

	}

	public Resolution getResolution() {
		return resolution;
	}

	public void setResolution(Resolution resolution) {
		this.resolution = resolution;
	}

	protected abstract long getPlayerInstance();

	public Double getCustomScale() {
		return customScale;
	}

	public void setCustomScale(Double customScale) {
		this.customScale = customScale;
		updateVideoPosition();
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

		updateVideoPosition();

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

		updateVideoPosition();

	}

	protected void updateVideoPosition() {

		if (getPlayerInstance() >= 0) {

			double applicationScale = 1.0;
			if (getGraphics() instanceof Graphics2D) {
				applicationScale = ((Graphics2D) getGraphics()).getTransform().getScaleX();
			}

			if (customScale != null) {

				Resolution resolution = getResolution();

				double defaultFactorX = (double) evrCanvas.getWidth() / (double) resolution.getWidth();
				double defaultFactorY = (double) evrCanvas.getHeight() / (double) resolution.getHeight();

				double defaultFactor = defaultFactorX;
				if (defaultFactorY < defaultFactor) {
					defaultFactor = defaultFactorY;
				}
				if (defaultFactor <= 0.0) { // Should not be possible?
					defaultFactor = 1.0;
				}

				double srcWidth = (evrCanvas.getWidth() / defaultFactor) / customScale;
				double srcHeight = (evrCanvas.getHeight() / defaultFactor) / customScale;

				double scaleFactorX = srcWidth / resolution.getWidth();
				double scaleFactorY = srcHeight / resolution.getHeight();

				float left, right, top, bottom;

				if (scaleFactorX < 1.0) {
					left = (float) ((1.0 - scaleFactorX) / 2.0);
					right = 1.0F - (float) ((1.0 - scaleFactorX) / 2.0);
				} else {
					left = 0.0F;
					right = 1.0F;
				}

				if (scaleFactorY < 1.0) {
					top = (float) ((1.0 - scaleFactorY) / 2.0);
					bottom = 1.0F - (float) ((1.0 - scaleFactorY) / 2.0);
				} else {
					top = 0.0F;
					bottom = 1.0F;
				}

				final double translateXScaled = (translateX / defaultFactor) / customScale;
				final double translateYScaled = (translateY / defaultFactor) / customScale;

				float translateFactorX = (float) (translateXScaled / resolution.getWidth());
				float translateFactorY = (float) (translateYScaled / resolution.getHeight());

				if (translateFactorX > left) {
					translateFactorX = left;
				} else if (translateFactorX < -left) {
					translateFactorX = -left;
				}

				if (translateFactorY > top) {
					translateFactorY = top;
				} else if (translateFactorY < -top) {
					translateFactorY = -top;
				}

				left -= translateFactorX;
				right -= translateFactorX;
				top -= translateFactorY;
				bottom -= translateFactorY;

				/*
				// TODO!
				float scaleOffset = (float)(customScale * 0.1);
				//float scaleOffset = 0.25F;
				float left = 0.0F + scaleOffset;
				float top = 0.0F + scaleOffset;
				float right = 1.0F - scaleOffset;
				float bottom = 1.0F - scaleOffset;
				 */

				evrCanvas.setVideoPosition(getPlayerInstance(), true, left, top, right, bottom, 0, 0, (int) (evrCanvas.getWidth() * applicationScale), (int) (evrCanvas.getHeight() * applicationScale));

			} else {
				evrCanvas.setVideoPosition(getPlayerInstance(), true, 0.0F, 0.0F, 1.0F, 1.0F, 0, 0, (int) (evrCanvas.getWidth() * applicationScale), (int) (evrCanvas.getHeight() * applicationScale));
			}

		}

	}

	public JMenu createZoomMenu() {

		JMenu zoomMenu = new JMenu("Zoom");

		ButtonGroup buttonGroup = new ButtonGroup();

		JRadioButtonMenuItem automaticMenuItem = new JRadioButtonMenuItem("Automatic");
		buttonGroup.add(automaticMenuItem);
		zoomMenu.add(automaticMenuItem);

		JRadioButtonMenuItem percentage25MenuItem = new JRadioButtonMenuItem("25%");
		buttonGroup.add(percentage25MenuItem);
		// zoomMenu.add(percentage25MenuItem);

		JRadioButtonMenuItem percentage50MenuItem = new JRadioButtonMenuItem("50%");
		buttonGroup.add(percentage50MenuItem);
		// zoomMenu.add(percentage50MenuItem);

		JRadioButtonMenuItem percentage75MenuItem = new JRadioButtonMenuItem("75%");
		buttonGroup.add(percentage75MenuItem);
		// zoomMenu.add(percentage75MenuItem);

		JRadioButtonMenuItem percentage100MenuItem = new JRadioButtonMenuItem("100%");
		buttonGroup.add(percentage100MenuItem);
		zoomMenu.add(percentage100MenuItem);

		JRadioButtonMenuItem percentage125MenuItem = new JRadioButtonMenuItem("125%");
		buttonGroup.add(percentage125MenuItem);
		zoomMenu.add(percentage125MenuItem);

		JRadioButtonMenuItem percentage150MenuItem = new JRadioButtonMenuItem("150%");
		buttonGroup.add(percentage150MenuItem);
		zoomMenu.add(percentage150MenuItem);

		JRadioButtonMenuItem percentage175MenuItem = new JRadioButtonMenuItem("175%");
		buttonGroup.add(percentage175MenuItem);
		zoomMenu.add(percentage175MenuItem);

		JRadioButtonMenuItem percentage200MenuItem = new JRadioButtonMenuItem("200%");
		buttonGroup.add(percentage200MenuItem);
		zoomMenu.add(percentage200MenuItem);

		class CustomScaleUpdater {

			private boolean updating = false;

			public void setCustomScale(Double customScale) {

				if (!updating) {

					updating = true;

					EvrCanvasPanel.this.setCustomScale(customScale);

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
		CustomScaleUpdater customScaleUpdater = new CustomScaleUpdater();

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

	public void resetView() {

		customScale = null;
		translateX = 0;
		translateY = 0;

		updateVideoPosition();

	}

	public JMenuItem createResetViewMenuItem() {

		JMenuItem resetViewMenuItem = new JMenuItem("Reset View"); // TODO
		resetViewMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				resetView();
			}
		});

		return resetViewMenuItem;

	}

}
