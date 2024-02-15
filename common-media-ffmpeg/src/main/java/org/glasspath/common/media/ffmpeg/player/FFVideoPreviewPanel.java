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
package org.glasspath.common.media.ffmpeg.player;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.glasspath.common.media.ffmpeg.FFH264NalUnitDecoderThread;
import org.glasspath.common.media.h264.H264NalUnit;
import org.glasspath.common.media.player.FramePanel;
import org.glasspath.common.media.player.IVideoPlayerListener;
import org.glasspath.common.media.player.IVideoPlayerListener.VideoPlayerStatistics;
import org.glasspath.common.media.player.IVideoPreviewPanel;
import org.glasspath.common.media.rtsp.H264ParameterSets;
import org.glasspath.common.media.rtsp.RtspStreamListener;
import org.glasspath.common.media.video.Frame;
import org.glasspath.common.media.video.Resolution;

@SuppressWarnings("serial")
public abstract class FFVideoPreviewPanel extends FramePanel implements IVideoPreviewPanel {

	public static boolean TODO_DEBUG = true;

	public static final int DEFAULT_SKIP_FRAMES = 0;

	private final RtspStreamListener streamListener;
	private final FFH264NalUnitDecoderThread decoderThread;
	private final List<H264NalUnit> resumeNalUnits = new ArrayList<>();
	private final Frame previewFrame = new Frame();
	private H264ParameterSets parameterSets = null;
	private volatile boolean parameterSetsSent = false;
	private volatile boolean previewEnabled = true;
	private volatile boolean selected = false;

	public FFVideoPreviewPanel(IVideoPlayerListener previewPanelListener, Resolution resolution) {
		this(previewPanelListener, resolution, DEFAULT_SKIP_FRAMES);
	}

	public FFVideoPreviewPanel(IVideoPlayerListener previewPanelListener, Resolution resolution, int skipFrames) {

		streamListener = new RtspStreamListener() {

			@Override
			public void parameterSetsUpdated(H264ParameterSets parameterSets) {
				if (parameterSets != null) {
					FFVideoPreviewPanel.this.parameterSets = parameterSets;
					parameterSetsSent = false;
				}
			}

			@Override
			public void nalUnitReceived(H264NalUnit nalUnit) {

				if (selected) {

					previewPanelListener.timestampChanged(nalUnit.receivedAt);

					if (decoderThread != null) {

						if (!parameterSetsSent) {

							resumeNalUnits.clear();
							getResumeNalUnits(resumeNalUnits);

							if (resumeNalUnits.size() >= 3) {

								if (TODO_DEBUG) {
									System.out.println("FFVideoPreviewPanel, sending resume nal units, size = " + resumeNalUnits.size());
								}

								decoderThread.reset();

								for (H264NalUnit resumeNalUnit : resumeNalUnits) {
									if (resumeNalUnit == nalUnit) {
										break;
									} else {
										decoderThread.addNalUnit(resumeNalUnit);
									}
								}

								parameterSetsSent = true;

							} else if (parameterSets != null && nalUnit.isIFrame()) {

								if (TODO_DEBUG) {
									System.out.println("FFVideoPreviewPanel, sending parameter sets");
								}

								decoderThread.reset();
								decoderThread.addNalUnit(parameterSets.sequenceParameterSet);
								decoderThread.addNalUnit(parameterSets.pictureParameterSet);

								parameterSetsSent = true;

							}

							resumeNalUnits.clear();

						}

						decoderThread.addNalUnit(nalUnit);

					}

				}

			}

			@Override
			public void recordingStateChanged(boolean recording, String path) {
				previewPanelListener.recordingStateChanged(recording);
			}
		};

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				installStreamListener(streamListener);
			}
		});

		decoderThread = new FFH264NalUnitDecoderThread(resolution) {

			@Override
			public void imageDecoded(BufferedImage image, long timestamp) {

				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {

						previewFrame.setImage(image);
						repaint();

					}
				});

			}

			@Override
			public void statisticsUpdated(VideoPlayerStatistics statistics) {
				previewPanelListener.statisticsUpdated(statistics);
			}
		};
		decoderThread.setSkipFrames(skipFrames);

		setFrame(previewFrame);

	}

	public FFH264NalUnitDecoderThread getDecoderThread() {
		return decoderThread;
	}

	protected abstract void installStreamListener(RtspStreamListener listener);

	protected abstract void getResumeNalUnits(List<H264NalUnit> resumeNalUnits);

	protected abstract void uninstallStreamListener(RtspStreamListener listener);

	@Override
	public JComponent getComponent() {
		return this;
	}

	@Override
	public void populateViewMenu(JMenu menu, JMenuItem overlayMenuItem) {

		menu.add(createZoomMenu());
		menu.add(createRotateMenu());
		menu.add(createFlipMenu());
		menu.addSeparator();
		if (overlayMenuItem != null) {
			menu.add(overlayMenuItem);
		}
		menu.add(createShowOverlayMenuItem(null)); // TODO: Pass preferences
		menu.add(createResetViewMenuItem());

	}

	@Override
	public boolean isPreviewEnabled() {
		return previewEnabled;
	}

	@Override
	public void setPreviewEnabled(boolean previewEnabled) {
		this.previewEnabled = previewEnabled;
	}

	@Override
	public void start() {
		decoderThread.start();
	}

	@Override
	public void setSelected(boolean selected) {
		this.selected = selected;
		parameterSetsSent = false;
	}

	@Override
	public void close() {
		uninstallStreamListener(streamListener);
		decoderThread.exit();
	}

}
