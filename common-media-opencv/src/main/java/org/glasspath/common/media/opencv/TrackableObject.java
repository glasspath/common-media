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

public abstract class TrackableObject {

	public int x = 0;
	public int y = 0;
	public int width = 0;
	public int height = 0;

	protected short lastUpdateId = 0;
	protected short updateCount = 0;
	protected short retainCount = 0;
	protected boolean preBuffered = false;

	public TrackableObject(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public boolean isRetained() {
		return retainCount > 0;
	}

	public boolean isPreBuffered() {
		return preBuffered;
	}

	public double distance(TrackableObject o) {

		double x1 = x + (width / 2.0);
		double y1 = y + (height / 2.0);
		double x2 = o.x + (o.width / 2.0);
		double y2 = o.y + (o.height / 2.0);

		return Math.hypot(x1 - x2, y1 - y2);

	}

	public void grow(int h, int v) {

		long x0 = this.x;
		long y0 = this.y;
		long x1 = this.width;
		long y1 = this.height;

		x1 += x0;
		y1 += y0;

		x0 -= h;
		y0 -= v;
		x1 += h;
		y1 += v;

		if (x1 < x0) {
			// Non-existent in X direction
			// Final width must remain negative so subtract x0 before
			// it is clipped so that we avoid the risk that the clipping
			// of x0 will reverse the ordering of x0 and x1.
			x1 -= x0;
			if (x1 < Integer.MIN_VALUE)
				x1 = Integer.MIN_VALUE;
			if (x0 < Integer.MIN_VALUE)
				x0 = Integer.MIN_VALUE;
			else if (x0 > Integer.MAX_VALUE)
				x0 = Integer.MAX_VALUE;
		} else { // (x1 >= x0)
			// Clip x0 before we subtract it from x1 in case the clipping
			// affects the representable area of the rectangle.
			if (x0 < Integer.MIN_VALUE)
				x0 = Integer.MIN_VALUE;
			else if (x0 > Integer.MAX_VALUE)
				x0 = Integer.MAX_VALUE;
			x1 -= x0;
			// The only way x1 can be negative now is if we clipped
			// x0 against MIN and x1 is less than MIN - in which case
			// we want to leave the width negative since the result
			// did not intersect the representable area.
			if (x1 < Integer.MIN_VALUE)
				x1 = Integer.MIN_VALUE;
			else if (x1 > Integer.MAX_VALUE)
				x1 = Integer.MAX_VALUE;
		}

		if (y1 < y0) {
			// Non-existent in Y direction
			y1 -= y0;
			if (y1 < Integer.MIN_VALUE)
				y1 = Integer.MIN_VALUE;
			if (y0 < Integer.MIN_VALUE)
				y0 = Integer.MIN_VALUE;
			else if (y0 > Integer.MAX_VALUE)
				y0 = Integer.MAX_VALUE;
		} else { // (y1 >= y0)
			if (y0 < Integer.MIN_VALUE)
				y0 = Integer.MIN_VALUE;
			else if (y0 > Integer.MAX_VALUE)
				y0 = Integer.MAX_VALUE;
			y1 -= y0;
			if (y1 < Integer.MIN_VALUE)
				y1 = Integer.MIN_VALUE;
			else if (y1 > Integer.MAX_VALUE)
				y1 = Integer.MAX_VALUE;
		}

		this.x = (int) x0;
		this.y = (int) y0;
		this.width = (int) x1;
		this.height = (int) y1;

	}

	public boolean intersects(TrackableObject r) {

		int tw = this.width;
		int th = this.height;
		int rw = r.width;
		int rh = r.height;

		if (rw <= 0 || rh <= 0 || tw <= 0 || th <= 0) {
			return false;
		}

		int tx = this.x;
		int ty = this.y;
		int rx = r.x;
		int ry = r.y;

		rw += rx;
		rh += ry;
		tw += tx;
		th += ty;

		// overflow || intersect
		return ((rw < rx || rw > tx) &&
				(rh < ry || rh > ty) &&
				(tw < tx || tw > rx) &&
				(th < ty || th > ry));

	}

	public void union(TrackableObject r) {

		int tx1 = this.x;
		int ty1 = this.y;
		int tx2 = this.width;
		int ty2 = this.height;

		int rx1 = r.x;
		int ry1 = r.y;
		int rx2 = r.width;
		int ry2 = r.height;

		tx2 += tx1;
		ty2 += ty1;
		rx2 += rx1;
		ry2 += ry1;

		if (tx1 > rx1)
			tx1 = rx1;
		if (ty1 > ry1)
			ty1 = ry1;
		if (tx2 < rx2)
			tx2 = rx2;
		if (ty2 < ry2)
			ty2 = ry2;

		tx2 -= tx1;
		ty2 -= ty1;

		this.x = tx1;
		this.y = ty1;
		this.width = tx2;
		this.height = ty2;

	}

}
