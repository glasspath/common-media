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
import java.util.ArrayList;
import java.util.List;

public abstract class FrameBuffer<F> {

	private BufferedFrame<F>[] buffer;
	private final Thread[] threads;
	private final List<Decoder> decoders;
	private final List<Converter> converters;
	private final List<Filter> filters;
	private Long seekTimestamp = null;
	private boolean exit = false;

	public FrameBuffer() {
		this(1, 1, 1);
	}

	public FrameBuffer(int decoderThreads, int converterThreads, int filterThreads) {

		buffer = createBuffer();

		threads = new Thread[decoderThreads + converterThreads + filterThreads];

		decoders = new ArrayList<>();
		for (int i = 0; i < decoderThreads; i++) {

			Decoder decoder = new Decoder(decoderThreads, i);

			decoders.add(decoder);
			threads[i] = new Thread(decoder);

		}

		converters = new ArrayList<>();
		for (int i = 0; i < converterThreads; i++) {

			Converter converter = new Converter(converterThreads, i);

			converters.add(converter);
			threads[i + decoderThreads] = new Thread(converter);

		}

		filters = new ArrayList<>();
		for (int i = 0; i < filterThreads; i++) {

			Filter filter = new Filter(filterThreads, i);

			filters.add(filter);
			threads[i + decoderThreads + converterThreads] = new Thread(filter);

		}

	}

	public void start() {
		for (Thread thread : threads) {
			thread.start();
		}
	}

	public void reset() {

		for (Decoder decoder : decoders) {
			decoder.reset = true;
		}

		for (Converter converter : converters) {
			converter.reset = true;
		}

		for (Filter filter : filters) {
			filter.reset = true;
		}

		while (true) {

			boolean resetPerformed = true;

			for (Decoder decoder : decoders) {
				if (!decoder.resetPerformed) {
					resetPerformed = false;
					break;
				}
			}

			if (resetPerformed) {

				for (Converter converter : converters) {
					if (!converter.resetPerformed) {
						resetPerformed = false;
						break;
					}
				}

				if (resetPerformed) {

					for (Filter filter : filters) {
						if (!filter.resetPerformed) {
							resetPerformed = false;
							break;
						}
					}

				}

			}

			if (resetPerformed) {
				break;
			} else {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}

		for (BufferedFrame<F> frame : buffer) {
			frame.reset();
		}

	}

	public void resume() {

		for (Decoder decoder : decoders) {
			decoder.reset = false;
		}

		for (Converter converter : converters) {
			converter.reset = false;
		}

		for (Filter filter : filters) {
			filter.reset = false;
		}

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

		protected Decoder(int workerCount, int workerIndex) {
			super(workerCount, workerIndex);
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

							if (buffer[i].source != null) {
								buffer[i].setTimestamp(getDecoderTimestamp(buffer[i].source));
								buffer[i].state = BufferedFrame.DECODED;
							} else {
								System.err.println("Decode failed, end of video reached?");
								buffer[i].state = BufferedFrame.DECODE_FAILED;
							}

							next();

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

		protected Converter(int workerCount, int workerIndex) {
			super(workerCount, workerIndex);
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

						next();

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

		protected Filter(int workerCount, int workerIndex) {
			super(workerCount, workerIndex);
		}

		@Override
		public void run() {

			try {

				while (!exit) {

					handleReset();

					if (buffer[i].state == BufferedFrame.CONVERTED) {

						filter(buffer[i].getImage());
						buffer[i].state = BufferedFrame.FILTERED;

						next();

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

		protected final int workerCount;
		protected final int workerIndex;
		protected int i = 0;
		protected boolean reset = false;
		protected boolean resetPerformed = false;

		protected Worker(int workerCount, int workerIndex) {
			this.workerCount = workerCount;
			this.workerIndex = workerIndex;
			this.i = workerIndex;
		}

		protected void handleReset() throws InterruptedException {

			if (reset) {

				i = workerIndex;

				resetPerformed = true;

				while (reset) {
					Thread.sleep(1);
				}

				resetPerformed = false;

			}

		}

		protected void next() {
			i += workerCount;
			if (i >= buffer.length) {
				i = workerIndex;
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

		public int getState() {
			return state;
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
