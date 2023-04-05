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

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class FrameGenerator {

	static {
		if (!MFUtils.isNativeLibraryLoaded()) {
			System.err.println("EvrCanvas: mfsdk lib is not loaded..");
		}
	}

	public FrameGenerator() {

	}

	public int requestFrames(long instance, int requestId, long timestamp, long interval, int count, int width, int height) {
		return requestFrames(instance, requestId, timestamp, interval, count, width, height, false);
	}

	public native long createInstance();

	public native int openFile(long instance, String path, int sourceType);

	public native long getDuration(long instance);

	public native int getVideoWidth(long instance);

	public native int getVideoHeight(long instance);

	public native boolean isResizingEnabled(long instance);

	public native long getCurrentTimestamp(long instance);

	public native int requestFrames(long instance, int requestId, long timestamp, long interval, int count, int width, int height, boolean returnFirstFrame);

	public native int cancelRequest(long instance, int requestId);

	public native int close(long instance);

	public void frameGenerated(long instance, int requestId, long requestedTimestamp, long actualTimestamp, int width, int height, byte[] bitmapData) {

		System.out.println("Frame generated, instance = " + instance + ", id = " + requestId + ", actualTimestamp = " + actualTimestamp + ", width = " + width + ", height = " + height + ", bitmapData.length() = " + bitmapData.length);

		// for (byte b : bitmapData) {
		// System.out.print(b + ", ");
		// }
		// System.out.println("");

	}

	public static BufferedImage createBufferedImage(byte[] bitmapData, int width, int height) {

		try {

			// ByteBuffer buffer = ByteBuffer.wrap(bitmapData);

			// BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
			byte[] bufferPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

			// if (bitmapData.length == bufferPixels.length) {
			if (bitmapData.length >= bufferPixels.length) { // TODO

				// System.out.println("Copying bytes!");
				// buffer.get(bufferPixels);

				// A R
				// B G
				// G B
				// R A

				int j = 2;
				for (int i = 3; i < bitmapData.length && j < bufferPixels.length; i += 4) {
					bufferPixels[j - 0] = bitmapData[i - 1];
					bufferPixels[j - 1] = bitmapData[i - 2];
					bufferPixels[j - 2] = bitmapData[i - 3];
					// bufferPixels[i - 3] = bitmapData[i - 0];
					// bufferPixels[i - 3] = (byte)255;
					j += 3;
				}

				return image;

			} else {
				System.out.println("bitmapData.length = " + bitmapData.length + ", expected length = " + bufferPixels.length);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;

	}

}
