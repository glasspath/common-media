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

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import org.glasspath.common.media.video.DefaultVideo;
import org.glasspath.common.media.video.Frame;
import org.glasspath.common.media.video.FrameLoader;
import org.glasspath.common.media.video.FrameLoaderCallback;

public class MFFrameLoader extends FrameLoader {

	public static boolean TODO_DEBUG = true;

	private final FrameGenerator frameGenerator;
	private int sourceType = MFUtils.MFSDK_SOURCE_TYPE_DEFAULT;
	private long generatorInstance = -1;
	private FrameLoaderCallback activeCallback = null;

	public MFFrameLoader() {

		frameGenerator = new FrameGenerator() {

			@Override
			public void frameGenerated(long instance, int requestId, long requestedTimestamp, long actualTimestamp, int width, int height, byte[] bitmapData) {
				// super.frameGenerated(instance, requestId, timestamp, width, height, bitmapData);

				// System.out.println("Frame generated, actualTimestamp = " + (actualTimestamp / 10000));
				if (activeCallback != null && activeCallback.getId() == requestId && !activeCallback.isCanceled()) {

					BufferedImage image = FrameGenerator.createBufferedImage(bitmapData, width, height);
					if (image != null && !activeCallback.isCanceled()) {

						if (width != activeCallback.width || height != activeCallback.height) {
							image = scaleImage(image, activeCallback.width, activeCallback.height);
						}

						// activeCallback.fireFrameLoaded(video, new Frame(video.getTimestamp() + (requestedTimestamp / 10000) + video.getStartTimeCorrectionOffset(), image));
						activeCallback.fireFrameLoaded(video, new Frame(requestedTimestamp / 10000, image));

					}

				}

			}
		};

	}

	@Override
	public void installOnVideo(DefaultVideo video, int width, int height, boolean closeFile) {
		super.installOnVideo(video, width, height, closeFile);

		if (!video.isPathValid()) {
			sourceType = MFUtils.MFSDK_SOURCE_TYPE_DUMMY;
		} else {
			sourceType = MFUtils.MFSDK_SOURCE_TYPE_DEFAULT;
		}

		generatorInstance = frameGenerator.createInstance();
		if (generatorInstance >= 0) {

			if (frameGenerator.openFile(generatorInstance, video.getPath(), sourceType) >= 0) {

				long duration = frameGenerator.getDuration(generatorInstance);
				if (duration > 0) {
					video.setDuration(duration / 10000);
				}

				int w = frameGenerator.getVideoWidth(generatorInstance);
				int h = frameGenerator.getVideoHeight(generatorInstance);
				if (w > 0 && h > 0) {
					video.setWidth(w);
					video.setHeight(h);
				}

				// TODO: Get frame rate
				video.setFrameRate(30.0);

			} else {
				System.out.println("Opening video file failed..");
			}

			if (closeFile) {
				frameGenerator.close(generatorInstance);
				generatorInstance = -1;
			}

		}

	}

	@Override
	public synchronized boolean loadFrame(FrameLoaderCallback callback, long timestamp, int width, int height, BufferedImage image, boolean returnFirstFrame) {

		int result = 0;

		timestamp -= (video.getTimestamp() + video.getStartTimeCorrectionOffset()); // Video starts at 0
		if (timestamp >= 0 && timestamp <= video.getDuration()) {

			if (generatorInstance < 0) {
				generatorInstance = frameGenerator.createInstance();
			}

			if (generatorInstance >= 0) {

				if (frameGenerator.openFile(generatorInstance, video.getPath(), sourceType) >= 0) {

					int frameHeight = height;
					int frameWidth;
					if (video.getHeight() != 0 && video.getWidth() != 0) {
						frameWidth = (int) (((double) frameHeight / (double) video.getHeight()) * video.getWidth());
					} else {
						frameWidth = frameHeight;
					}

					activeCallback = callback;
					activeCallback.width = frameWidth;
					activeCallback.height = frameHeight;

					if (!callback.isCanceled()) {
						result = frameGenerator.requestFrames(generatorInstance, callback.getId(), timestamp * 10000, 0, 1, frameWidth, frameHeight, returnFirstFrame);
					}

					activeCallback = null;

				}

				frameGenerator.close(generatorInstance);
				generatorInstance = -1;

			}

		}

		return result == 0;

	}

	@Override
	public synchronized void loadFrames(FrameLoaderCallback callback, long from, long to, int frameHeight, int totalWidth, int frameSpacing) {

		long videoCorrectionOffset = video.getStartTimeCorrectionOffset();
		long fromTimestamp = from - (video.getTimestamp() + videoCorrectionOffset);
		long toTimestamp = to - (video.getTimestamp() + videoCorrectionOffset);

		// System.out.println("from, end: " + from + " -> " + end);
		if (fromTimestamp < 0) {
			fromTimestamp = 0;
		}

		if (fromTimestamp < toTimestamp && fromTimestamp < video.getDuration()) {

			int frameWidth;
			if (video.getHeight() != 0 && video.getWidth() != 0) {
				frameWidth = (int) (((double) frameHeight / (double) video.getHeight()) * video.getWidth());
			} else {
				frameWidth = frameHeight;
			}

			double count = (double) totalWidth / (double) (frameWidth + frameSpacing);
			if (count > 0) {

				long interval = (long) ((double) (to - from) / count);

				if (generatorInstance < 0) {
					generatorInstance = frameGenerator.createInstance();
				}

				if (generatorInstance >= 0) {

					if (frameGenerator.openFile(generatorInstance, video.getPath(), sourceType) >= 0) {

						activeCallback = callback;
						activeCallback.width = frameWidth;
						activeCallback.height = frameHeight;

						if (!callback.isCanceled()) {
							// System.out.println("Requesting frames, interval = " + interval + ", count = " + (int)count);
							frameGenerator.requestFrames(generatorInstance, callback.getId(), fromTimestamp * 10000, interval * 10000, (int) count + 1, frameWidth, frameHeight);
							// System.out.println("Requesting frames done (" + count + ")");
						}

						activeCallback = null;

					}

					frameGenerator.close(generatorInstance);
					generatorInstance = -1;

				}

			}

		}

	}

	@Override
	public void frameLoaderCallbackCanceled(FrameLoaderCallback callback) {

		long generatorInstance = this.generatorInstance; // We create a copy because this method is not thread safe..
		if (generatorInstance >= 0 && callback == activeCallback) {
			// System.out.println("Canceling request " + callback.getId());
			frameGenerator.cancelRequest(generatorInstance, callback.getId());
		}

	}

	public static BufferedImage scaleImage(BufferedImage image, int width, int height) {

		Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);

		if (scaledImage instanceof BufferedImage) {
			return (BufferedImage) scaledImage;
		} else {

			BufferedImage scaledBufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

			Graphics2D g2d = scaledBufferedImage.createGraphics();
			g2d.drawImage(scaledImage, 0, 0, null);
			g2d.dispose();

			return scaledBufferedImage;

		}

	}

	@Override
	public void close() {

		/*
		if (generatorInstance >= 0) {
			frameGenerator.close(generatorInstance);
		}
		generatorInstance = -1;
		
		fileOpen = false;
		 */

	}

}
