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
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

public class FFVideoFrameConverter {

	public static boolean TODO_DEBUG = true;

	private boolean optimized = true;

	public FFVideoFrameConverter() {
		this(true);
	}

	public FFVideoFrameConverter(boolean optimized) {
		this.optimized = optimized;
	}

	public boolean isOptimized() {
		return optimized;
	}

	public void setOptimized(boolean optimized) {
		this.optimized = optimized;
	}

	public BufferedImage createBufferedImage(Frame frame) {
		return createBufferedImage(frame, null);
	}

	public BufferedImage createBufferedImage(Frame frame, BufferedImage image) {

		if (optimized) {

			try {

				if (image == null) {
					image = new BufferedImage(frame.imageWidth, frame.imageHeight, Java2DFrameConverter.getBufferedImageType(frame));
				}
				
				byte[] bufferPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
				ByteBuffer buffer = (ByteBuffer) frame.image[0].position(0);

				if (bufferPixels.length == buffer.limit()) {
					buffer.get(bufferPixels);
					return image;
				} else {
					if (TODO_DEBUG) {
						System.err.println("VideoFrameConverter (optimized), buffer lengths don't match.. (" + bufferPixels.length + " != " + buffer.limit() + ")");
					}
					optimized = false;
					return createBufferedImage(frame);
				}

			} catch (Exception e) {
				e.printStackTrace();
				optimized = false;
				return createBufferedImage(frame);
			}

		} else {

			// TODO? Java2DFrameConverter cannot be used for creating a list of images,
			// it always returns the same BufferedImage instance but with new image data..
			Java2DFrameConverter converter = new Java2DFrameConverter();
			image = converter.convert(frame);
			converter.close();

			return image;

		}

	}

}
