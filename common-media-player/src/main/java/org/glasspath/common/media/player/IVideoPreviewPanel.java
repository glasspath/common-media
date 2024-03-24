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
package org.glasspath.common.media.player;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

public interface IVideoPreviewPanel {

	public static enum PlayerState {
		PLAYER_STOPPED,
		PLAYER_STARTING,
		PLAYER_STARTED,
		PLAYER_STOPPING
	}

	public JComponent getComponent();

	public void populateViewMenu(JMenu menu, JMenuItem overlayMenuItem);

	public int getDefaultCursor();

	public void setDefaultCursor(int defaultCursor);

	public IOverlay getOverlay();

	public void setOverlay(IOverlay overlay);

	public boolean isPreviewEnabled();

	public void setPreviewEnabled(boolean previewEnabled);

	public void start();

	public void setSelected(boolean selected);

	public void close();

}
