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
package org.glasspath.media.recorder;

import java.util.List;

import org.glasspath.common.media.h264.H264NalUnit;
import org.glasspath.common.media.rtsp.H264ParameterSets;
import org.glasspath.common.media.video.Resolution;

public abstract class H264NalUnitRecorder<T extends Recording> {

	public static enum PtsMode {
		FIXED_FRAME_RATE,
		NAL_UNIT_TIMESTAMPS_CORRECTED,
		NAL_UNIT_TIMESTAMPS_NOT_CORRECTED
	}

	public static boolean TODO_DEBUG = false;

	private String name = "H264NalUnitRecorder";
	private int timeScale = 100000;
	private PtsMode ptsMode = PtsMode.FIXED_FRAME_RATE;
	private double fixedFrameRate = 30.0;
	private long ptsOffset = 0;
	private String path = null;
	private boolean recording = false;
	private H264NalUnit nalUnit = null;
	private long fixedFrameRateDuration = 3333;
	private long ptsCorrection = 0;
	private long pts = 0;

	public H264NalUnitRecorder() {

	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getTimeScale() {
		return timeScale;
	}

	public void setTimeScale(int timeScale) {
		this.timeScale = timeScale;
		updateFixedFrameRateDuration();
	}

	public PtsMode getPtsMode() {
		return ptsMode;
	}

	public void setPtsMode(PtsMode ptsMode) {
		this.ptsMode = ptsMode;
	}

	public double getFixedFrameRate() {
		return fixedFrameRate;
	}

	public void setFixedFrameRate(double fixedFrameRate) {
		this.fixedFrameRate = fixedFrameRate;
		updateFixedFrameRateDuration();
	}

	public long getPtsOffset() {
		return ptsOffset;
	}

	public void setPtsOffset(long ptsOffset) {
		this.ptsOffset = ptsOffset;
	}

	public String getPath() {
		return path;
	}

	private void updateFixedFrameRateDuration() {
		if (fixedFrameRate > 0) {
			fixedFrameRateDuration = (long) (timeScale / fixedFrameRate);
		}
	}

	protected abstract String getNextPath(long timestamp);

	protected abstract boolean createFile(String recordPath, Resolution resolution, H264ParameterSets parameterSets, long pts, long created);

	protected abstract void fileCreated(String filePath);

	protected abstract T getRecording();

	protected abstract boolean writeNalUnit(H264NalUnit nalUnit, long pts, long duration);

	protected abstract long getBytesWritten();

	protected abstract long getFileSizeLimit();

	protected abstract Resolution getResolution();

	protected abstract void recordingEnded(long timestamp);

	protected abstract boolean closeFile(T recording);

	protected abstract void fileClosed(String filePath);

	public void writeNalUnits(List<H264NalUnit> nalUnits, H264ParameterSets parameterSets) {

		if (nalUnits != null && nalUnits.size() > 0 && parameterSets != null && parameterSets.sequenceParameterSet != null && parameterSets.pictureParameterSet != null) {

			try {

				for (H264NalUnit nextNalUnit : nalUnits) {

					// The NalUnit from a previous iteration is used because we need to determine the duration between NalUnits (frames only)
					if (nalUnit != null) {

						if (!recording) {

							if (nalUnit.isIFrame()) {

								// Get the path of the file to record to, use the time-stamp of the NalUnit because it might have been buffered for a while
								path = getNextPath(nalUnit.receivedAt);

								if (ptsMode == PtsMode.FIXED_FRAME_RATE) {
									ptsCorrection = 0;
									pts = ptsOffset;
								} else if (ptsMode == PtsMode.NAL_UNIT_TIMESTAMPS_CORRECTED) {
									ptsCorrection = nalUnit.timestamp;
									pts = ptsOffset;
								} else {
									ptsCorrection = 0;
									pts = nalUnit.timestamp + ptsOffset;
								}

								if (createFile(path, getResolution(), parameterSets, pts, nalUnit.receivedAt)) {

									fileCreated(path);

									recording = true;

								} else if (TODO_DEBUG) {
									System.err.println(name + " file not created");
								}

							}

						}

						if (recording) {

							if (ptsMode != PtsMode.FIXED_FRAME_RATE) {
								pts = (nalUnit.timestamp - ptsCorrection) + ptsOffset;
							}

							long duration;
							if (ptsMode == PtsMode.FIXED_FRAME_RATE) {
								duration = fixedFrameRateDuration;
							} else {
								duration = nextNalUnit.timestamp - nalUnit.timestamp;
							}

							writeNalUnit(nalUnit, pts, duration);

							if (ptsMode == PtsMode.FIXED_FRAME_RATE) {
								pts += fixedFrameRateDuration;
							}

							if (nextNalUnit.isIFrame() && getFileSizeLimit() > 0 && getBytesWritten() >= getFileSizeLimit()) {

								if (TODO_DEBUG) {
									System.out.println(name + " file size limit reached, closing MP4 file..");
								}

								close(false);

							}

						}

					}

					if (nextNalUnit.isFrame()) {
						nalUnit = nextNalUnit;
					}

				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	public void close() {
		close(true);
	}

	protected void close(boolean reset) {

		recordingEnded(System.currentTimeMillis());

		if (closeFile(getRecording())) {
			fileClosed(path);
		} else if (TODO_DEBUG) {
			System.out.println(name + " warning, file not closed");
		}

		path = null;
		recording = false;
		ptsCorrection = 0;
		pts = 0;

		if (reset) {
			nalUnit = null;
		}

	}

}
