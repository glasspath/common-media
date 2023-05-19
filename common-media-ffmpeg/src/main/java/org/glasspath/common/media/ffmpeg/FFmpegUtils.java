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

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecHWConfig;
import org.bytedeco.ffmpeg.avutil.AVBufferRef;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.Loader;

public class FFmpegUtils {

	public static boolean TODO_DEBUG = false;

	static {
		initLogLevel();
	}

	private FFmpegUtils() {

	}

	public static void initLogLevel() {
		if (TODO_DEBUG) {
			org.bytedeco.ffmpeg.global.avutil.av_log_set_level(avutil.AV_LOG_DEBUG);
		} else {
			org.bytedeco.ffmpeg.global.avutil.av_log_set_level(avutil.AV_LOG_ERROR);
		}
	}

	public static void listCodecs() {

		try {

			String ffmpeg = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);
			ProcessBuilder pb = new ProcessBuilder(ffmpeg, "-codecs");
			pb.inheritIO().start().waitFor();

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

}
