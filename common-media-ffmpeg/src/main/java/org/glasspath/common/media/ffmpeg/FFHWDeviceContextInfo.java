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

import org.bytedeco.ffmpeg.avcodec.AVCodecHWConfig;
import org.bytedeco.ffmpeg.avutil.AVBufferRef;

import java.util.Objects;

import static org.bytedeco.ffmpeg.global.avutil.av_free;

public final class FFHWDeviceContextInfo {

	private final AVCodecHWConfig hwConfig;
	private final AVBufferRef hwDeviceContext;
	private volatile boolean released = false;

	public FFHWDeviceContextInfo(AVCodecHWConfig hwConfig, AVBufferRef hwDeviceContext) {
		this.hwConfig = hwConfig;
		this.hwDeviceContext = hwDeviceContext;
	}

	public AVCodecHWConfig getHWConfig() {
		return hwConfig;
	}

	public AVBufferRef getHWDeviceContext() {
		return hwDeviceContext;
	}

	public void release() {
		if (!released) {
			released = true;
			// TODO: Causes crashes on MacOS
			// av_free(hwConfig);
			// av_free(hwDeviceContext);
		}
	}

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		} else if (object instanceof FFHWDeviceContextInfo) {
			FFHWDeviceContextInfo hwDeviceContextInfo = (FFHWDeviceContextInfo) object;
			return Objects.equals(hwConfig, hwDeviceContextInfo.hwConfig) && Objects.equals(hwDeviceContext, hwDeviceContextInfo.hwDeviceContext);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(hwConfig, hwDeviceContext);
	}

}
