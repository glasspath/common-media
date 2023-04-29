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

	private final int preProcessorThreads;
	private final int postProcessorThreads;
	private BufferedFrame<F>[] buffer;
	private final List<Worker> workers;
	private final Thread[] threads;
	private Long seekTimestamp = null;
	private boolean exit = false;

	public FrameBuffer() {
		this(1, 1, 1, 1);
	}

	public FrameBuffer(int decoderThreads, int preProcessorThreads, int converterThreads, int postProcessorThreads) {

		this.preProcessorThreads = preProcessorThreads;
		this.postProcessorThreads = postProcessorThreads;

		buffer = createBuffer();

		workers = new ArrayList<>();
		threads = new Thread[decoderThreads + preProcessorThreads + converterThreads + postProcessorThreads];

		for (int i = 0; i < decoderThreads; i++) {

			Decoder decoder = new Decoder(decoderThreads, i);

			workers.add(decoder);
			threads[i] = new Thread(decoder);

		}

		for (int i = 0; i < preProcessorThreads; i++) {

			PreProcessor preProcessor = new PreProcessor(preProcessorThreads, i);

			workers.add(preProcessor);
			threads[decoderThreads + i] = new Thread(preProcessor);

		}

		for (int i = 0; i < converterThreads; i++) {

			Converter converter = new Converter(converterThreads, i);

			workers.add(converter);
			threads[decoderThreads + preProcessorThreads + i] = new Thread(converter);

		}

		for (int i = 0; i < postProcessorThreads; i++) {

			PostProcessor postProcessor = new PostProcessor(postProcessorThreads, i);

			workers.add(postProcessor);
			threads[decoderThreads + preProcessorThreads + converterThreads + i] = new Thread(postProcessor);

		}

	}

	public void start() {
		for (Thread thread : threads) {
			thread.start();
		}
	}

	public void reset() {

		for (Worker worker : workers) {
			worker.reset = true;
		}

		while (true) {

			boolean resetPerformed = true;

			for (Worker worker : workers) {
				if (!worker.resetPerformed) {
					resetPerformed = false;
					break;
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
		for (Worker worker : workers) {
			worker.reset = false;
		}
	}

	public void exit() {
		exit = true;
	}

	public boolean isExited() {
		return exit && true; // TODO?
	}

	protected abstract BufferedFrame<F>[] createBuffer();

	protected abstract boolean createDecoder(int thread);

	protected abstract long getDecoderTimestamp(int thread, F source);

	protected abstract void setDecoderTimestamp(int thread, long timestamp);

	protected abstract F decode(int thread);

	protected abstract void closeDecoder(int thread);

	protected abstract boolean createPreProcessor(int thread);

	protected abstract F preProcess(int thread, F frame);

	protected abstract void closePreProcessor(int thread);

	protected abstract boolean createConverter(int thread);

	protected abstract BufferedImage convert(int thread, F source);

	protected abstract void closeConverter(int thread);

	protected abstract boolean createPostProcessor(int thread);

	protected abstract void postProcess(int thread, BufferedImage image);

	protected abstract void closePostProcessor(int thread);

	public void seek(long timestamp) {
		seekTimestamp = timestamp;
	}

	protected class Decoder extends Worker {

		protected Decoder(int workerCount, int workerIndex) {
			super(workerCount, workerIndex);
		}

		@Override
		public void run() {

			if (createDecoder(workerIndex)) {

				try {

					while (!exit) {

						handleReset();

						if (buffer[i].state == BufferedFrame.CLEARED) {

							if (seekTimestamp != null) {
								setDecoderTimestamp(workerIndex, seekTimestamp.longValue());
								seekTimestamp = null;
							}

							buffer[i].source = decode(workerIndex);

							if (buffer[i].source != null) {
								buffer[i].setTimestamp(getDecoderTimestamp(workerIndex, buffer[i].source));
								if (preProcessorThreads > 0) {
									buffer[i].state = BufferedFrame.DECODED;
								} else {
									buffer[i].state = BufferedFrame.PRE_PROCESSED;
								}
							} else {
								// System.err.println("Decode failed, end of video reached?");
								buffer[i].state = BufferedFrame.DECODE_FAILED;
							}

							next();

						} else {
							Thread.sleep(0, 100);
						}

					}

				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				closeDecoder(workerIndex);

			}

		}

	}

	protected class PreProcessor extends Worker {

		protected PreProcessor(int workerCount, int workerIndex) {
			super(workerCount, workerIndex);
		}

		@Override
		public void run() {

			if (createPreProcessor(workerIndex)) {

				try {

					while (!exit) {

						handleReset();

						if (buffer[i].state == BufferedFrame.DECODED) {

							buffer[i].source = preProcess(workerIndex, buffer[i].source);
							if (buffer[i].source != null) {
								buffer[i].state = BufferedFrame.PRE_PROCESSED;
							} else {
								buffer[i].state = BufferedFrame.PRE_PROCESS_FAILED;
							}

							next();

						} else {
							Thread.sleep(0, 100);
						}

					}

				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				closePreProcessor(workerIndex);

			}

		}

	}

	protected class Converter extends Worker {

		protected Converter(int workerCount, int workerIndex) {
			super(workerCount, workerIndex);
		}

		@Override
		public void run() {

			if (createConverter(workerIndex)) {

				try {

					while (!exit) {

						handleReset();

						if (buffer[i].state == BufferedFrame.PRE_PROCESSED) {

							buffer[i].setImage(convert(workerIndex, buffer[i].source));
							buffer[i].getImage().setAccelerationPriority(1.0F);

							buffer[i].source = null;
							if (postProcessorThreads > 0) {
								buffer[i].state = BufferedFrame.CONVERTED;
							} else {
								buffer[i].state = BufferedFrame.READY;
							}

							next();

						} else {
							Thread.sleep(0, 100);
						}

					}

				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				closeConverter(workerIndex);

			}

		}

	}

	protected class PostProcessor extends Worker {

		protected PostProcessor(int workerCount, int workerIndex) {
			super(workerCount, workerIndex);
		}

		@Override
		public void run() {

			if (createPostProcessor(workerIndex)) {

				try {

					while (!exit) {

						handleReset();

						if (buffer[i].state == BufferedFrame.CONVERTED) {

							postProcess(workerIndex, buffer[i].getImage());
							buffer[i].state = BufferedFrame.READY;

							next();

						} else {
							Thread.sleep(0, 100);
						}

					}

				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				closePostProcessor(workerIndex);

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

		public static final int DECODE_FAILED = -1;
		public static final int PRE_PROCESS_FAILED = -2;
		public static final int CLEARED = 0;
		public static final int DECODED = 1;
		public static final int PRE_PROCESSED = 2;
		public static final int CONVERTED = 3;
		public static final int READY = 4;

		protected F source = null;
		protected int state = CLEARED;

		public BufferedFrame() {

		}

		public int getState() {
			return state;
		}

		public boolean isImageReady() {
			return state == READY;
		}

		public void reset() {
			source = null;
			setImage(null);
			setTimestamp(0);
			state = CLEARED;
		}

	}

}
