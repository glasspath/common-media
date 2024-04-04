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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecHWConfig;
import org.bytedeco.ffmpeg.avutil.AVBufferRef;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.LongPointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.ShortPointer;
import org.bytedeco.javacv.Frame;
import org.glasspath.common.os.OsUtils;

public class FFmpegUtils {

	public static boolean TODO_DEBUG = false;

	public static final int MIN_TRANSCODED_FILE_LENGTH = 100; // TODO: Checking for length > 0 also worked, what is the minimal file size we expect?
	
	private FFmpegUtils() {

	}

	public static void setup() {

		if (TODO_DEBUG) {
			System.setProperty("org.bytedeco.javacpp.logger.debug", "true");
		}

		// Call first to make sure javacpp library loading mechanism works ok..
		Loader.load(Loader.class);

		if (TODO_DEBUG) {
			org.bytedeco.ffmpeg.global.avutil.av_log_set_level(avutil.AV_LOG_DEBUG);
		} else {
			org.bytedeco.ffmpeg.global.avutil.av_log_set_level(avutil.AV_LOG_ERROR);
		}

	}

	public static void listCodecs() {

		try {

			String ffmpeg = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);

			List<String> commands = new ArrayList<>();
			commands.add(ffmpeg);
			commands.add("-codecs");

			ProcessBuilder pb = new ProcessBuilder(commands);
			pb.inheritIO().start().waitFor();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void printFileInfo(File inputFile) {

		try {

			String ffmpeg = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);

			List<String> commands = new ArrayList<>();
			commands.add(ffmpeg);
			commands.add("-i");
			commands.add(inputFile.getAbsolutePath());

			ProcessBuilder pb = new ProcessBuilder(commands);
			pb.inheritIO().start().waitFor();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void transcode(File inputFile, File outputFile) {
		transcode(inputFile, null, outputFile);
	}

	public static void transcode(File inputFile, List<String> args, File outputFile) {
		transcode(inputFile, null, outputFile, null);
	}

	public static void transcode(File inputFile, List<String> args, File outputFile, TranscodingListener listener) {

		try {

			if (outputFile.exists()) {

				if (listener != null) {
					listener.println("Deleting " + outputFile.getAbsolutePath());
				}

				if (!OsUtils.deleteFile(outputFile)) {
					if (listener != null) {
						listener.println("File could not be deleted..");
					}
					return;
				}

			}

			String ffmpeg = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);

			List<String> commands = new ArrayList<>();
			commands.add(ffmpeg);
			commands.add("-loglevel");
			commands.add("info");
			commands.add("-y"); // Overwrite (yes)
			commands.add("-i");
			commands.add(inputFile.getAbsolutePath());
			if (args != null) {
				commands.addAll(args);
			}
			commands.add(outputFile.getAbsolutePath());

			ProcessBuilder processBuilder = new ProcessBuilder(commands);

			if (listener != null) {

				Process process = processBuilder.start();

				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

				String line = null;
				while ((line = reader.readLine()) != null) {
					listener.println(line);
				}

				process.waitFor();

				listener.println("");
				if (outputFile.exists() && outputFile.length() >= MIN_TRANSCODED_FILE_LENGTH) {
					listener.println("Video exported to " + outputFile.getAbsolutePath());
				} else {
					listener.println("Exporting failed..");
				}

			} else {
				Process process = processBuilder.inheritIO().start();
				process.waitFor();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static FFHWDeviceContextInfo createHWDeviceContextInfo(AVCodec avCodec, int hwDeviceType) {

		FFHWDeviceContextInfo hwDeviceContextInfo = null;

		for (int i = 0;; i++) {

			AVCodecHWConfig hwConfig = avcodec.avcodec_get_hw_config(avCodec, i);
			if (hwConfig == null) {
				break;
			}

			int deviceType = hwConfig.device_type();
			if (TODO_DEBUG) {
				System.out.println("FFmpegUtils.createHWDeviceContextInfo() deviceType = " + deviceType);
			}

			if ((hwConfig.methods() & avcodec.AV_CODEC_HW_CONFIG_METHOD_HW_DEVICE_CTX) < 0) {
				continue;
			} else if (deviceType != hwDeviceType) {
				continue;
			}

			AVBufferRef hwDeviceContext = avutil.av_hwdevice_ctx_alloc(deviceType);
			if (hwDeviceContext == null || avutil.av_hwdevice_ctx_create(hwDeviceContext, deviceType, (String) null, null, 0) < 0) {
				System.err.println("HW accel not supported for type " + deviceType);
				// TODO: Causes crashes on MacOS
				// avutil.av_free(hwConfig);
				// avutil.av_free(hwDeviceContext);
			} else {
				System.out.println("HW accel created for type " + deviceType);
				hwDeviceContextInfo = new FFHWDeviceContextInfo(hwConfig, hwDeviceContext);
			}

			break;

		}

		return hwDeviceContextInfo;

	}

	public static void copyFrame(Frame from, Frame to) {

		if (from.image != null && to.image != null && from.image.length == to.image.length) {

			/*
			to.imageWidth = from.imageWidth;
			to.imageHeight = from.imageHeight;
			to.imageDepth = from.imageDepth;
			to.imageChannels = from.imageChannels;
			to.imageStride = from.imageStride;
			to.keyFrame = from.keyFrame;
			to.pictType = from.pictType;
			to.streamIndex = from.streamIndex;
			to.type = from.type;
			to.opaque = new Pointer[3];
			*/

			to.keyFrame = from.keyFrame;
			to.streamIndex = from.streamIndex;
			to.timestamp = from.timestamp;

			/*
			for (int i = 0; i < from.image.length; i++) {
				to.image[i] = from.image[i];
			}
			*/
			((Pointer[]) to.opaque)[0] = cloneBufferArray(from.image, to.image);

		}

	}

	// Copied from org.bytedeco.javacv.Frame.java
	@SuppressWarnings("resource")
	private static Pointer cloneBufferArray(Buffer[] srcBuffers, Buffer[] clonedBuffers) {
		Pointer opaque = null;

		if (srcBuffers != null && srcBuffers.length > 0) {
			int totalCapacity = 0;
			for (int i = 0; i < srcBuffers.length; i++) {
				srcBuffers[i].rewind();
				totalCapacity += srcBuffers[i].capacity();
			}

			/*
			 * In order to optimize the transfer we need a type check.
			 *
			 * Most CPUs support hardware memory transfer for different data
			 * types, so it's faster to copy more bytes at once rather
			 * than one byte per iteration as in case of ByteBuffer.
			 *
			 * For example, Intel CPUs support MOVSB (byte transfer), MOVSW
			 * (word transfer), MOVSD (double word transfer), MOVSS (32 bit
			 * scalar single precision floating point), MOVSQ (quad word
			 * transfer) and so on...
			 *
			 * Type checking may be improved by changing the order in
			 * which a buffer is checked against. If it's likely that the
			 * expected buffer is of type "ShortBuffer", then it should be
			 * checked at first place.
			 *
			 */

			if (srcBuffers[0] instanceof ByteBuffer) {
				BytePointer pointer = new BytePointer(totalCapacity);
				for (int i = 0; i < srcBuffers.length; i++) {
					clonedBuffers[i] = pointer.limit(pointer.position() + srcBuffers[i].limit())
							.asBuffer().put((ByteBuffer) srcBuffers[i]);
					pointer.position(pointer.limit());
				}
				opaque = pointer;
			} else if (srcBuffers[0] instanceof ShortBuffer) {
				ShortPointer pointer = new ShortPointer(totalCapacity);
				for (int i = 0; i < srcBuffers.length; i++) {
					clonedBuffers[i] = pointer.limit(pointer.position() + srcBuffers[i].limit())
							.asBuffer().put((ShortBuffer) srcBuffers[i]);
					pointer.position(pointer.limit());
				}
				opaque = pointer;
			} else if (srcBuffers[0] instanceof IntBuffer) {
				IntPointer pointer = new IntPointer(totalCapacity);
				for (int i = 0; i < srcBuffers.length; i++) {
					clonedBuffers[i] = pointer.limit(pointer.position() + srcBuffers[i].limit())
							.asBuffer().put((IntBuffer) srcBuffers[i]);
					pointer.position(pointer.limit());
				}
				opaque = pointer;
			} else if (srcBuffers[0] instanceof LongBuffer) {
				LongPointer pointer = new LongPointer(totalCapacity);
				for (int i = 0; i < srcBuffers.length; i++) {
					clonedBuffers[i] = pointer.limit(pointer.position() + srcBuffers[i].limit())
							.asBuffer().put((LongBuffer) srcBuffers[i]);
					pointer.position(pointer.limit());
				}
				opaque = pointer;
			} else if (srcBuffers[0] instanceof FloatBuffer) {
				FloatPointer pointer = new FloatPointer(totalCapacity);
				for (int i = 0; i < srcBuffers.length; i++) {
					clonedBuffers[i] = pointer.limit(pointer.position() + srcBuffers[i].limit())
							.asBuffer().put((FloatBuffer) srcBuffers[i]);
					pointer.position(pointer.limit());
				}
				opaque = pointer;
			} else if (srcBuffers[0] instanceof DoubleBuffer) {
				DoublePointer pointer = new DoublePointer(totalCapacity);
				for (int i = 0; i < srcBuffers.length; i++) {
					clonedBuffers[i] = pointer.limit(pointer.position() + srcBuffers[i].limit())
							.asBuffer().put((DoubleBuffer) srcBuffers[i]);
					pointer.position(pointer.limit());
				}
				opaque = pointer;
			}

			for (int i = 0; i < srcBuffers.length; i++) {
				srcBuffers[i].rewind();
				clonedBuffers[i].rewind();
			}
		}

		if (opaque != null) {
			opaque.retainReference();
		}
		return opaque;
	}

	public static interface TranscodingListener {

		public void println(String line);

	}

}
