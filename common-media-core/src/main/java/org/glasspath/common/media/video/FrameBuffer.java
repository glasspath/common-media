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
package org.glasspath.common.media.video;

import java.awt.image.BufferedImage;

public abstract class FrameBuffer<F> {

	private BufferedFrame<F>[] frames;
	private final Thread decoderThread;
	private final Thread converterThread;
	private final Thread filterThread;

	private boolean exit = false;

	public FrameBuffer() {

		frames = createBuffer();

		decoderThread = new Thread(new Runnable() {

			@Override
			public void run() {

				while (!exit) {

					BufferedFrame<F> frame = null;

					for (BufferedFrame<F> f : frames) {
						if (f.source == null && !f.decodeFailed) {
							frame = f;
							break;
						}
					}

					if (frame != null) {
						frame.source = decode();
						if (frame.source != null) {
							frame.setTimestamp(getDecoderTimestamp(frame.source));
						} else {
							System.err.println("Decode failed..");
							frame.decodeFailed = true;
						}
					} else {
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

				}

			}
		});

		converterThread = new Thread(new Runnable() {

			@Override
			public void run() {

				while (!exit) {

					BufferedFrame<F> frame = null;

					for (BufferedFrame<F> f : frames) {
						if (f.source != null && f.getImage() == null) {
							frame = f;
							break;
						}
					}

					if (frame != null) {

						frame.filtered = false;
						frame.setImage(convert(frame.source));
						frame.getImage().setAccelerationPriority(1.0F);

						frame.source = null;

					} else {
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

				}

			}
		});

		filterThread = new Thread(new Runnable() {

			@Override
			public void run() {

				while (!exit) {

					BufferedFrame<F> frame = null;

					for (BufferedFrame<F> f : frames) {
						if (f.getImage() != null && !f.filtered) {
							frame = f;
							break;
						}
					}

					if (frame != null) {
						filter(frame.getImage());
						frame.filtered = true;
					} else {
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

				}

			}
		});

	}

	public void start() {
		decoderThread.start();
		converterThread.start();
		filterThread.start();
	}

	public void exit() {
		exit = true;
	}

	protected abstract BufferedFrame<F>[] createBuffer();

	protected abstract long getDecoderTimestamp(F source);

	protected abstract void setDecoderTimestamp();

	protected abstract F decode();

	protected abstract BufferedImage convert(F source);

	protected abstract void filter(BufferedImage image);

	public static class BufferedFrame<F> extends Frame {

		private F source = null;
		private boolean decodeFailed = false;
		private boolean filtered = false;

		public BufferedFrame() {

		}

		public boolean isReady() {
			return getTimestamp() != null && getImage() != null && filtered;
		}

		public void reset() {

			// source = null;
			// decodeFailed = false;

			setImage(null);
			setTimestamp(0);
			filtered = false;

		}

	}

}
