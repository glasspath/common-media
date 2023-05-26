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

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.glasspath.common.media.h264.H264NalUnit;
import org.glasspath.common.media.mfsdk.EvrCanvas.PlayerBufferingState;
import org.glasspath.common.media.mfsdk.EvrCanvas.PlayerInstanceState;
import org.glasspath.common.media.mfsdk.EvrCanvasPanel;
import org.glasspath.common.media.mfsdk.MFUtils;
import org.glasspath.common.media.player.IOverlay;
import org.glasspath.common.media.player.IVideoPlayerListener;
import org.glasspath.common.media.player.IVideoPlayerListener.VideoPlayerStatistics;
import org.glasspath.common.media.player.IVideoPreviewPanel;
import org.glasspath.common.media.rtsp.H264ParameterSets;
import org.glasspath.common.media.rtsp.RtspStreamListener;

@SuppressWarnings("serial")
public abstract class MFVideoPreviewPanel extends EvrCanvasPanel implements IVideoPreviewPanel {

	public static final int TIMEOUT = 3000;

	private final RtspStreamListener streamListener;
	private boolean previewEnabled = false;
	private long playerInstance = -1;
	private PlayerState playerState = PlayerState.PLAYER_STOPPED;
	private H264ParameterSets parameterSets = null;
	private boolean parameterSetsSent = false;
	private final List<H264NalUnit> resumeNalUnits = new ArrayList<>();
	private int fpsFrameCount = 0;
	private long lastFpsMeasurement = 0;

	public MFVideoPreviewPanel(IVideoPlayerListener previewPanelListener) {
		super();

		streamListener = new RtspStreamListener() {

			@Override
			public void parameterSetsUpdated(H264ParameterSets parameterSets) {
				if (parameterSets != null) {
					MFVideoPreviewPanel.this.parameterSets = parameterSets;
					parameterSetsSent = false;
				}
			}

			@Override
			public void nalUnitReceived(H264NalUnit nalUnit) {

				previewPanelListener.timestampChanged(nalUnit.receivedAt);

				if (playerInstance >= 0 && playerState == PlayerState.PLAYER_STARTED) {

					if (!parameterSetsSent) {

						resumeNalUnits.clear();
						getResumeNalUnits(resumeNalUnits);

						if (resumeNalUnits.size() >= 3) {

							System.out.println("Sending resume nal units, size = " + resumeNalUnits.size());

							for (H264NalUnit resumeNalUnit : resumeNalUnits) {
								if (resumeNalUnit == nalUnit) {
									break;
								} else {
									evrCanvas.queueNalUnit(playerInstance, resumeNalUnit.bytes, resumeNalUnit.bytes.length, resumeNalUnit.timestamp);
								}
							}

							evrCanvas.setPaintBackgroundOnly(playerInstance, false);
							parameterSetsSent = true;

						} else if (parameterSets != null && nalUnit.isIFrame()) {

							System.out.println("Sending parameter sets");
							evrCanvas.queueNalUnit(playerInstance, parameterSets.sequenceParameterSet.bytes, parameterSets.sequenceParameterSet.bytes.length, nalUnit.timestamp);
							evrCanvas.queueNalUnit(playerInstance, parameterSets.pictureParameterSet.bytes, parameterSets.pictureParameterSet.bytes.length, nalUnit.timestamp);

							evrCanvas.setPaintBackgroundOnly(playerInstance, false);
							parameterSetsSent = true;

						}

						resumeNalUnits.clear();

					}

					evrCanvas.queueNalUnit(playerInstance, nalUnit.bytes, nalUnit.bytes.length, nalUnit.timestamp);

					if (nalUnit.isFrame()) {

						fpsFrameCount++;

						if (System.currentTimeMillis() > lastFpsMeasurement + 3000) {

							if (lastFpsMeasurement != 0) {
								double time = System.currentTimeMillis() - lastFpsMeasurement;
								double fps = fpsFrameCount / (time / 1000.0);
								previewPanelListener.statisticsUpdated(new VideoPlayerStatistics(fps));
							}

							fpsFrameCount = 0;
							lastFpsMeasurement = System.currentTimeMillis();

						}

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

	}

	protected abstract void installStreamListener(RtspStreamListener listener);

	protected abstract void getResumeNalUnits(List<H264NalUnit> resumeNalUnits);

	protected abstract void uninstallStreamListener(RtspStreamListener listener);

	@Override
	protected long getPlayerInstance() {
		return playerInstance;
	}

	@Override
	public JComponent getComponent() {
		return this;
	}

	@Override
	public void populateViewMenu(JMenu menu, JMenuItem overlayMenuItem) {

		menu.add(createZoomMenu());
		menu.addSeparator();
		if (overlayMenuItem != null) {
			menu.add(overlayMenuItem);
		}
		menu.add(createResetViewMenuItem());
		
	}
	
	@Override
	public IOverlay getOverlay() {
		return null; // TODO
	}

	@Override
	public void setOverlay(IOverlay overlay) {
		// TODO
	}

	@Override
	public boolean isPreviewEnabled() {
		return previewEnabled;
	}

	@Override
	public void setPreviewEnabled(boolean previewEnabled) {

		if (previewEnabled && !this.previewEnabled) {
			parameterSetsSent = false;
			startPlayer();
		} else if (!previewEnabled && this.previewEnabled) {
			parameterSetsSent = false;
			stopPlayer();
		}

		this.previewEnabled = previewEnabled;

	}

	@Override
	public void start() {

	}

	@Override
	public void close() {
		uninstallStreamListener(streamListener);
		setPreviewEnabled(false);
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

									if (evrCanvas.getPlayerInstanceState(playerInstance) == PlayerInstanceState.PLAYER_INSTANCE_STATE_RUNNING.getValue() && evrCanvas.getPlayerBufferingState(playerInstance) == PlayerBufferingState.PLAYER_BUFFERING_STATE_BUFFERING.getValue()) {
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

						// Blocks until stopPlayer() is called
						evrCanvas.startPlayer(playerInstance, MFUtils.MFSDK_SOURCE_TYPE_H264, "", 1.0);

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

}
