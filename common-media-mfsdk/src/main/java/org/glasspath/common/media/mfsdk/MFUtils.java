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

public class MFUtils {

	private static final boolean nativeLibraryLoaded;

	static {

		boolean libraryLoaded = false;

		try {
			System.loadLibrary("mfsdk");
			libraryLoaded = true;
		} catch (Error e) {

		}

		nativeLibraryLoaded = libraryLoaded;

	}

	public static final MFUtils INSTANCE = new MFUtils();

	public static final int MFSDK_SOURCE_TYPE_DEFAULT = 0;
	public static final int MFSDK_SOURCE_TYPE_H264 = 1;
	public static final int MFSDK_SOURCE_TYPE_RTSPDMP = 2; // TODO: Remove
	public static final int MFSDK_SOURCE_TYPE_DUMMY = 3;

	private MFUtils() {

	}

	public static boolean isNativeLibraryLoaded() {
		return nativeLibraryLoaded;
	}

	public native int start();

	public native int shutdown();

}
