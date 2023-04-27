/*
 * This file is part of Glasspath Aerialist.
 * Copyright (C) 2011 - 2022 Remco Poelstra
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
package org.glasspath.common.media.player.icons;

import java.net.URL;

import org.glasspath.common.swing.icon.SvgIcon;

public class Icons {

	public static final Icons INSTANCE = new Icons();
	public static final ClassLoader CLASS_LOADER = INSTANCE.getClass().getClassLoader();

	private Icons() {

	}

	private static URL getSvg(String name) {
		return CLASS_LOADER.getResource("org/glasspath/common/media/player/icons/svg/" + name);
	}

	public static final SvgIcon motionPlayBlue = new SvgIcon(getSvg("motion-play.svg"));

	static {
		motionPlayBlue.setColorFilter(SvgIcon.BLUE);
	}

}
