/*
 * This file is part of Glasspath Common.
 * Copyright (C) 2011 - 2024 Remco Poelstra
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
package org.glasspath.common.media.opencv;

import java.awt.Rectangle;
import java.util.List;

import org.bytedeco.javacpp.PointerScope;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;
import org.glasspath.common.Common;

public class BufferedFrameProcessor implements ObjectTracker.Listener<DetectionResult> {

	public static int TODO_MIN_UPDATE_COUNT_PRE_BUFFER = 0;

	public static final int BLUR_MODE_OFF = 0;
	public static final int BLUR_MODE_BLUR = 1;
	public static final int BLUR_MODE_PIXELATE = 2;

	public static final int DEFAULT_RETAIN = 30;

	private final DetectionModel model;
	private final DetectionResultFilter resultFilter;
	private final ObjectTracker<DetectionResult> objectTracker;
	private final int width;
	private final int height;
	private final Rectangle bounds;
	private final BufferedMat[] matBuffer;

	private int retain = DEFAULT_RETAIN;
	private int blurMode = BLUR_MODE_OFF;

	private int index = 0;

	public BufferedFrameProcessor(DetectionModel model, DetectionResultFilter resultFilter, ObjectTracker<DetectionResult> objectTracker, int width, int height, int bufferSize) {

		this.model = model;
		this.resultFilter = resultFilter;
		this.objectTracker = objectTracker;
		this.width = width;
		this.height = height;
		this.bounds = new Rectangle(0, 0, width, height);
		this.matBuffer = new BufferedMat[bufferSize];

		for (int i = 0; i < bufferSize; i++) {
			matBuffer[i] = new BufferedMat();
		}

		objectTracker.addListener(this);

	}

	public int getRetain() {
		return retain;
	}

	public void setRetain(int retain) {
		this.retain = retain;
	}

	public int getBlurMode() {
		return blurMode;
	}

	public void setBlurMode(int blurMode) {
		this.blurMode = blurMode;
	}

	public int getIndex() {
		return index;
	}

	public Frame process(Frame frame) {

		BufferedMat bufferedMat = matBuffer[index];

		if (frame != null) {

			// System.out.println("performing detection on index: " + index);

			bufferedMat.mat = bufferedMat.matConverter.convert(frame);

			try (PointerScope pointerScope = new PointerScope()) {
				bufferedMat.results = objectTracker.processObjects(model.process(bufferedMat.mat, resultFilter));
			} catch (Exception e) {
				Common.LOGGER.error("Exception while processing objects: ", e); //$NON-NLS-1$
			}

		} else {
			// System.out.println("frame is null, resetting index " + index);
			bufferedMat.results = null;
		}

		bufferedMat.processed = false;

		index++;
		if (index >= matBuffer.length) {
			index = 0;
		}

		bufferedMat = matBuffer[index];
		if (bufferedMat.mat != null && bufferedMat.results != null && !bufferedMat.processed) {

			// System.out.println("blurring index: " + index);

			if (blurMode == BLUR_MODE_BLUR) {
				try (PointerScope pointerScope = new PointerScope()) {
					blur(bufferedMat.mat, bufferedMat.results);
				} catch (Exception e) {
					Common.LOGGER.error("Exception while blurring mat: ", e); //$NON-NLS-1$
				}
			}

			bufferedMat.processed = true;

			return bufferedMat.matConverter.convert(bufferedMat.mat);

		} else {
			// System.out.println("returning null");
			return null;
		}

	}

	protected void blur(Mat mat, List<DetectionResult> results) {

		for (DetectionResult result : results) {

			Rectangle r = new Rectangle(result.x, result.y, result.width, result.height).intersection(bounds);

			if (r.width > 0 && r.height > 0 && r.x >= 0 && r.y >= 0 && r.x + r.width <= width && r.y + r.height <= height) {

				int blurRadius = 35;
				/*
				if (r.width < blurRadius) {
					blurRadius = r.width;
				}
				if (r.height < blurRadius) {
					blurRadius = r.height;
				}
				*/

				Rect region = new Rect(r.x, r.y, r.width, r.height);
				Mat roi = mat.apply(region);
				Mat dest = new Mat();
				opencv_imgproc.stackBlur(roi, dest, new Size(blurRadius, blurRadius));
				dest.copyTo(roi);

				dest.close();
				roi.close();
				region.close();

			}

		}

	}

	@Override
	public void objectAdded(DetectionResult object) {
		if (TODO_MIN_UPDATE_COUNT_PRE_BUFFER == 0) {
			preBufferObject(object);
		}
	}

	@Override
	public void objectUpdated(DetectionResult updatedObject, DetectionResult object) {
		if (TODO_MIN_UPDATE_COUNT_PRE_BUFFER > 0 && updatedObject.updateCount == TODO_MIN_UPDATE_COUNT_PRE_BUFFER) {
			preBufferObject(updatedObject);
		}
	}

	@Override
	public void objectRemoved(DetectionResult object) {

	}

	protected void preBufferObject(DetectionResult object) {

		DetectionResult copy = objectTracker.createCopy(object);
		copy.preBuffered = true;

		int i = index - 1;
		if (i < 0) {
			i = matBuffer.length - 1;
		}

		while (i != index) {

			if (matBuffer[i].results != null) {
				// System.out.println("Adding copy to index: " + i);
				matBuffer[i].results.add(copy);
			}

			i--;
			if (i < 0) {
				i = matBuffer.length - 1;
			}

		}

	}

	public static class BufferedMat {

		public final OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
		public Mat mat = null;
		public List<DetectionResult> results = null;
		// public Frame frame = null;
		public boolean processed = false;

	}

}
