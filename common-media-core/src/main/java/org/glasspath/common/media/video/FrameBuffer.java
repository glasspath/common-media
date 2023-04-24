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

	private BufferedFrame<F>[] buffer;
	private final Decoder decoder;
	private final Converter converter;
	private final Filter filter;
	private final Thread decoderThread;
	private final Thread converterThread;
	private final Thread filterThread;
	private Long seekTimestamp = null;
	private boolean exit = false;

	public FrameBuffer() {
		buffer = createBuffer();
		decoder = new Decoder();
		converter = new Converter();
		filter = new Filter();
		decoderThread = new Thread(decoder);
		converterThread = new Thread(converter);
		filterThread = new Thread(filter);
	}

	public void start() {
		decoderThread.start();
		converterThread.start();
		filterThread.start();
	}

	public void reset() {

		decoder.reset = true;
		converter.reset = true;
		filter.reset = true;

		while (!decoder.resetPerformed || !converter.resetPerformed || !filter.resetPerformed) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		for (BufferedFrame<F> frame : buffer) {
			frame.reset();
		}

	}

	public void resume() {

		decoder.reset = false;
		converter.reset = false;
		filter.reset = false;

	}

	public void exit() {
		exit = true;
	}

	public boolean isExited() {
		return exit && true; // TODO?
	}

	protected abstract BufferedFrame<F>[] createBuffer();

	protected abstract boolean createDecoder();

	protected abstract long getDecoderTimestamp(F source);

	protected abstract void setDecoderTimestamp(long timestamp);

	protected abstract F decode();

	protected abstract BufferedImage convert(F source);

	protected abstract void filter(BufferedImage image);

	protected abstract void closeDecoder();

	public void seek(long timestamp) {
		seekTimestamp = timestamp;
	}

	protected class Decoder extends Worker {

		protected Decoder() {

		}

		@Override
		public void run() {

			if (createDecoder()) {

				try {

					while (!exit) {

						handleReset();

						if (buffer[i].state == BufferedFrame.CLEARED) {

							if (seekTimestamp != null) {
								setDecoderTimestamp(seekTimestamp.longValue());
								seekTimestamp = null;
							}

							buffer[i].source = decode();

							// TODO: Check if we are at the end of the video?
							if (buffer[i].source == null) {
								setDecoderTimestamp(0);
								buffer[i].source = decode();
							}

							if (buffer[i].source != null) {
								buffer[i].setTimestamp(getDecoderTimestamp(buffer[i].source));
								buffer[i].state = BufferedFrame.DECODED;
							} else {
								System.err.println("Decode failed..");
								buffer[i].state = BufferedFrame.DECODE_FAILED;
							}

							i++;
							if (i >= buffer.length) {
								i = 0;
							}

						} else {
							Thread.sleep(1);
						}

					}

				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				closeDecoder();

			}

		}

	}

	protected class Converter extends Worker {

		protected Converter() {

		}

		@Override
		public void run() {

			try {

				while (!exit) {

					handleReset();

					if (buffer[i].state == BufferedFrame.DECODED) {

						buffer[i].setImage(convert(buffer[i].source));
						buffer[i].getImage().setAccelerationPriority(1.0F);

						buffer[i].source = null;
						buffer[i].state = BufferedFrame.CONVERTED;

						i++;
						if (i >= buffer.length) {
							i = 0;
						}

					} else {
						Thread.sleep(1);
					}

				}

			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

	}

	protected class Filter extends Worker {

		protected Filter() {

		}

		@Override
		public void run() {

			try {

				while (!exit) {

					handleReset();

					if (buffer[i].state == BufferedFrame.CONVERTED) {

						filter(buffer[i].getImage());
						buffer[i].state = BufferedFrame.FILTERED;

						i++;
						if (i >= buffer.length) {
							i = 0;
						}

					} else {
						Thread.sleep(1);
					}

				}

			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

	}

	protected abstract class Worker implements Runnable {

		protected int i = 0;
		protected boolean reset = false;
		protected boolean resetPerformed = false;

		protected Worker() {

		}

		protected void handleReset() throws InterruptedException {

			if (reset) {

				i = 0;

				resetPerformed = true;

				while (reset) {
					Thread.sleep(1);
				}

				resetPerformed = false;

			}

		}

	}

	public static abstract class BufferedFrame<F> extends Frame {

		public static final int CLEARED = 0;
		public static final int DECODED = 1;
		public static final int CONVERTED = 2;
		public static final int FILTERED = 3;
		public static final int DECODE_FAILED = -1;

		protected F source = null;
		protected int state = CLEARED;

		public BufferedFrame() {

		}

		public boolean isImageReady() {
			return state == FILTERED;
		}

		public void reset() {
			source = null;
			setImage(null);
			setTimestamp(0);
			state = CLEARED;
		}

	}

}
