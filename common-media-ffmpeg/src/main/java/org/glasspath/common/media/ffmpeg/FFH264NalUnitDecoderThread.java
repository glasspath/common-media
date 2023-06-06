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
package org.glasspath.common.media.ffmpeg;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.glasspath.common.media.h264.H264NalUnit;
import org.glasspath.common.media.player.IVideoPlayerListener.VideoPlayerStatistics;
import org.glasspath.common.media.rtsp.H264ParameterSets;
import org.glasspath.common.media.video.Resolution;

public abstract class FFH264NalUnitDecoderThread {

	public static boolean TODO_DEBUG = true;

	public static final int INITIAL_NAL_UNIT_QUEUE_SIZE = 50;
	public static final int MAX_DECODE_HEADER_RETRIES = 10;
	public static final int MAX_NO_FRAME_COUNT = 100;
	public static final int FPS_MEASUREMENT_INTERVAL = 3000;
	public static final int SLEEP_INTERVAL = 5;

	public static final int QUEUE_ACTION_ADD_NAL_UNIT = 1;
	public static final int QUEUE_ACTION_COPY = 2;
	public static final int QUEUE_ACTION_RESET = 3;

	private final Thread nalUnitDecoderThread;
	private FFH264NalUnitDecoder decoder = null;
	private FFVideoFrameConverter converter = null;
	private volatile H264ParameterSets parameterSets = null;
	private int skipFrames = 0;
	private List<H264NalUnit> nalUnitQueue = new ArrayList<>(INITIAL_NAL_UNIT_QUEUE_SIZE);
	private List<H264NalUnit> nalUnitQueueCopy = null;
	private H264NalUnit sps = null;
	private H264NalUnit pps = null;
	private H264NalUnit iFrame = null;
	private int skippedFrames = 0;
	private int noFrameCount = 0;
	private int fpsFrameCount = 0;
	private long lastFpsMeasurement = 0;
	private volatile boolean exit = false;

	public FFH264NalUnitDecoderThread(Resolution resolution) {

		nalUnitDecoderThread = new Thread(new Runnable() {

			@Override
			public void run() {

				createDecoder();

				converter = new FFVideoFrameConverter();

				while (!exit) {

					if (nalUnitQueue.size() > 0) {

						performSynchronizedAction(QUEUE_ACTION_COPY, null);

						if (sps == null && pps == null && parameterSets != null && parameterSets.sequenceParameterSet != null && parameterSets.pictureParameterSet != null) {
							sps = parameterSets.sequenceParameterSet;
							pps = parameterSets.pictureParameterSet;
						}

						for (H264NalUnit nalUnit : nalUnitQueueCopy) {

							boolean frameAvailable = false;

							if (sps == null) {

								if (nalUnit.isSequenceParameterSet()) {
									sps = nalUnit;
								}

							} else if (pps == null) {

								if (nalUnit.isPictureParameterSet()) {
									pps = nalUnit;
								}

							} else if (iFrame == null) {

								if (nalUnit.isIFrame()) {

									iFrame = nalUnit;

									byte[] bytes = new byte[sps.bytes.length + pps.bytes.length + iFrame.bytes.length];
									System.arraycopy(sps.bytes, 0, bytes, 0, sps.bytes.length);
									System.arraycopy(pps.bytes, 0, bytes, sps.bytes.length, pps.bytes.length);
									System.arraycopy(iFrame.bytes, 0, bytes, sps.bytes.length + pps.bytes.length, iFrame.bytes.length);

									decoder.flush();
									frameAvailable = decoder.decodeNalUnit(bytes, iFrame.timestamp, true);

									int retries = 0;
									while (!frameAvailable && retries < MAX_DECODE_HEADER_RETRIES) {
										if (TODO_DEBUG) {
											System.err.println("FFH264NalUnitDecoderThread, performing decode header retry, sending sps + pps + iFrame");
										}
										frameAvailable = decoder.decodeNalUnit(bytes, iFrame.timestamp, true);
										retries++;
									}

								}

							} else if (nalUnit.isFrame()) {

								if (skippedFrames >= skipFrames) {
									frameAvailable = decoder.decodeNalUnit(nalUnit.bytes, nalUnit.timestamp, true);
								} else {
									decoder.decodeNalUnit(nalUnit.bytes, nalUnit.timestamp, false);
									skippedFrames++;
								}

							}

							if (frameAvailable) {

								imageDecoded(converter.createBufferedImage(decoder.getFrame()), nalUnit.timestamp);

								skippedFrames = 0;
								noFrameCount = 0;
								fpsFrameCount++;

								if (System.currentTimeMillis() >= lastFpsMeasurement + FPS_MEASUREMENT_INTERVAL) {

									if (lastFpsMeasurement != 0) {
										double interval = System.currentTimeMillis() - lastFpsMeasurement;
										double fps = fpsFrameCount / (interval / 1000.0);
										statisticsUpdated(new VideoPlayerStatistics(fps));
									}

									fpsFrameCount = 0;
									lastFpsMeasurement = System.currentTimeMillis();

								}

							} else {

								noFrameCount++;
								if (noFrameCount >= MAX_NO_FRAME_COUNT) {
									if (TODO_DEBUG) {
										System.err.println("FFH264NalUnitDecoderThread, no frames received from decoder, creating new decoder");
									}
									reset();
									createDecoder();
								}

							}

						}

					} else {

						try {
							Thread.sleep(SLEEP_INTERVAL);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

					}

				}

				if (TODO_DEBUG) {
					System.out.println("FFH264NalUnitDecoderThread, exiting nal unit decoder thread");
				}

				decoder = null;
				converter = null;

			}

			private void createDecoder() {

				if (decoder != null) {
					decoder.close();
				}

				decoder = new FFH264NalUnitDecoder();

				if (resolution != null) {
					decoder.setImageWidth(resolution.getWidth());
					decoder.setImageHeight(resolution.getHeight());
				}

				decoder.start();

			}

		});

	}

	public H264ParameterSets getParameterSets() {
		return parameterSets;
	}

	public void setParameterSets(H264ParameterSets parameterSets) {
		this.parameterSets = parameterSets;
	}

	public int getSkipFrames() {
		return skipFrames;
	}

	public void setSkipFrames(int skipFrames) {
		this.skipFrames = skipFrames;
	}

	public void start() {
		nalUnitDecoderThread.start();
	}

	public synchronized void performSynchronizedAction(int action, H264NalUnit nalUnit) {
		if (action == QUEUE_ACTION_ADD_NAL_UNIT) {
			nalUnitQueue.add(nalUnit);
		} else if (action == QUEUE_ACTION_COPY) {
			nalUnitQueueCopy = nalUnitQueue;
			nalUnitQueue = new ArrayList<>(INITIAL_NAL_UNIT_QUEUE_SIZE);
		} else if (action == QUEUE_ACTION_RESET) {
			reset();
		}
	}

	public void reset() {

		sps = null;
		pps = null;
		iFrame = null;

		skippedFrames = 0;
		noFrameCount = 0;

		fpsFrameCount = 0;
		lastFpsMeasurement = System.currentTimeMillis();

	}

	public abstract void imageDecoded(BufferedImage image, long timestamp);

	public abstract void statisticsUpdated(VideoPlayerStatistics statistics);

	public void exit() {
		exit = true;
	}

}
