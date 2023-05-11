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

import java.awt.image.BufferedImage;
import java.util.Map;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber.Exception;
import org.glasspath.common.media.video.DefaultVideo;
import org.glasspath.common.media.video.DefaultVideo.MetadataTimestamp;
import org.glasspath.common.media.video.Frame;
import org.glasspath.common.media.video.FrameLoader;
import org.glasspath.common.media.video.FrameLoaderCallback;

public class FFFrameLoader extends FrameLoader {

	public static boolean TODO_DEBUG = false;

	static {
		FFmpegUtils.initLogLevel();
	}

	private final FFVideoFrameConverter frameConverter;
	private FFmpegFrameGrabber frameGrabber = null;
	private boolean fileOpen = false;

	public FFFrameLoader() {
		frameConverter = new FFVideoFrameConverter(false); // TODO? (we are setting width/height on frame grabber, this causes optimized converter to fail..)
	}

	@Override
	public void installOnVideo(DefaultVideo video, boolean closeFile) {
		super.installOnVideo(video, closeFile);

		frameGrabber = new FFmpegFrameGrabber(video.getPath());

		int cores = Runtime.getRuntime().availableProcessors(); // Includes hyper threading cores
		int threads = cores + 1;
		frameGrabber.setVideoOption("threads", "" + threads);

		try {

			frameGrabber.start();

			video.setDuration(frameGrabber.getLengthInTime() / 1000L);
			video.setFrameRate(frameGrabber.getFrameRate());

			Map<String, String> metadata;

			metadata = frameGrabber.getMetadata();
			if (TODO_DEBUG) {
				System.out.println("General Metdata:");
				printMetadata(metadata);
			}
			parseMetadata(metadata);

			metadata = frameGrabber.getVideoMetadata();
			if (TODO_DEBUG) {
				System.out.println("Video Metdata:");
				printMetadata(metadata);
			}
			parseMetadata(metadata);

			fileOpen = true;

		} catch (Exception e) {
			fileOpen = false;
			e.printStackTrace();
		}

	}

	private void printMetadata(Map<String, String> metadata) {
		for (Map.Entry<String, String> entry : metadata.entrySet()) {
			System.out.println("    " + entry.getKey() + ": " + entry.getValue().toString());
		}
	}

	private void parseMetadata(Map<String, String> metadata) {

		MetadataTimestamp metadataTimestamp;
		for (Map.Entry<String, String> entry : metadata.entrySet()) {

			if (entry.getValue() != null) {

				metadataTimestamp = new MetadataTimestamp(entry.getKey(), entry.getValue().toString());
				if (metadataTimestamp.getTimestamp() != null) {
					video.getMetadataTimestamps().add(metadataTimestamp);
				}

			}

		}

	}

	@Override
	public boolean loadFrame(FrameLoaderCallback callback, long timestamp, int width, int height, boolean returnFirstFrame) {

		if (fileOpen) {

			try {

				// TODO? (Setting width/height causes optimized converter to fail..)
				frameGrabber.setImageWidth(width);
				frameGrabber.setImageHeight(height);

				timestamp -= (video.getTimestamp() + video.getStartTimeCorrectionOffset()); // Video starts at 0

				if (timestamp >= 0 && timestamp <= video.getDuration()) {

					// frameGrabber.setTimestamp(timestamp);
					frameGrabber.setVideoTimestamp(timestamp * 1000);

					while (!callback.isCanceled()) { // TODO?

						org.bytedeco.javacv.Frame frame = frameGrabber.grabFrame(false, true, true, false);

						if (frame == null) {
							break;
						}

						if (frame.image != null) {

							BufferedImage image = frameConverter.createBufferedImage(frame);

							// callback.fireFrameLoaded(video, new Frame(video.getTimestamp() + timestamp, image));
							callback.fireFrameLoaded(video, new Frame(timestamp, image));

							return true;

						}

					}

				}

			} catch (Exception e) {
				fileOpen = false;
				e.printStackTrace();
			}

		}

		return false;

	}

	@Override
	public synchronized void loadFrame(FrameLoaderCallback callback, long timestamp, int width, int height) {

		if (fileOpen) {

			try {

				frameGrabber.setImageWidth(width);
				frameGrabber.setImageHeight(height);

				timestamp -= (video.getTimestamp() + video.getStartTimeCorrectionOffset()); // Video starts at 0

				if (timestamp >= 0 && timestamp <= video.getDuration()) {

					// frameGrabber.setTimestamp(timestamp);
					frameGrabber.setVideoTimestamp(timestamp * 1000);

					while (!callback.isCanceled()) { // TODO?

						org.bytedeco.javacv.Frame frame = frameGrabber.grabFrame(false, true, true, false);

						if (frame == null) {
							break;
						}

						if (frame.image != null) {

							BufferedImage image = frameConverter.createBufferedImage(frame);

							// callback.fireFrameLoaded(video, new Frame(video.getTimestamp() + timestamp, image));
							callback.fireFrameLoaded(video, new Frame(timestamp, image));

							break;

						}

					}

				}

			} catch (Exception e) {
				fileOpen = false;
				e.printStackTrace();
			}

		}

	}

	@Override
	public synchronized void loadFrames(FrameLoaderCallback callback, long from, long to, int frameHeight, int totalWidth, int frameSpacing) {

		if (fileOpen) {

			try {

				long videoCorrectionOffset = video.getStartTimeCorrectionOffset();
				long fromTimestamp = from - (video.getTimestamp() + videoCorrectionOffset);
				long toTimestamp = to - (video.getTimestamp() + videoCorrectionOffset);

				// System.out.println("from, end: " + from + " -> " + end);
				if (fromTimestamp < 0) {
					fromTimestamp = 0;
				}

				if (fromTimestamp < toTimestamp && fromTimestamp < video.getDuration()) {

					frameGrabber.setImageHeight(frameHeight);

					int frameWidth;
					if (video.getHeight() != 0 && video.getWidth() != 0) {
						frameWidth = (int) (((double) frameHeight / (double) video.getHeight()) * video.getWidth());
					} else {
						frameWidth = frameHeight;
					}
					frameGrabber.setImageWidth(frameWidth);

					double count = (double) totalWidth / (double) (frameWidth + frameSpacing);
					if (count > 0) {

						long interval = (long) ((double) (to - from) / count);

						long timestamp = fromTimestamp;
						int noFrameCount = 0;
						while (!callback.isCanceled() && timestamp < toTimestamp) {

							if (TODO_DEBUG) {
								System.out.println("Grabbing frame for video: " + video.getName() + " at: " + timestamp);
							}
							/*
							frameGrabber.setVideoTimestamp(timestamp * 1000);
							org.bytedeco.javacv.Frame frame = frameGrabber.grabFrame(false, true, true, false);
							 */
							org.bytedeco.javacv.Frame frame = grabFrame(frameGrabber, timestamp);

							if (frame == null) {

								noFrameCount++;

								// TODO?
								if (timestamp > 5000 && noFrameCount > 3) {
									break;
								} else {

									if (TODO_DEBUG) {
										System.err.println("No frame loaded for video: " + video.getName() + " at: " + timestamp);
									}

									if (timestamp == 0) {

										while (timestamp < 1000) {

											timestamp += 25;

											frameGrabber.setVideoTimestamp(timestamp * 1000);
											frame = frameGrabber.grabFrame(false, true, false, false);
											if (frame != null) {
												frame = frameGrabber.grabFrame(false, true, true, false);
												if (frame != null) {
													if (TODO_DEBUG) {
														System.err.println("Retry succeeded for video: " + video.getName() + " at: " + timestamp);
													}
													break;
												}
											}

										}

										if (frame == null) {
											if (TODO_DEBUG) {
												System.err.println("Retry failed for video: " + video.getName() + " at: " + timestamp);
											}
										}

									}

								}

							}

							if (frame != null && frame.image != null) {
								BufferedImage image = frameConverter.createBufferedImage(frame);
								// callback.fireFrameLoaded(video, new Frame(video.getTimestamp() + timestamp, image));
								if (TODO_DEBUG) {
									System.out.println("Frame loaded for video: " + video.getName() + " pts: " + frame.timestamp);
								}
								callback.fireFrameLoaded(video, new Frame(timestamp, image));
							}

							timestamp += interval;

						}

					}

				}

			} catch (Exception e) {
				fileOpen = false;
				e.printStackTrace();
			}

		}

	}

	@Override
	public void frameLoaderCallbackCanceled(FrameLoaderCallback callback) {
		// No action needed, callback.isCanceled() is checked in while loops
	}

	@Override
	public void close() {

		try {

			fileOpen = false;

			// close() will call stop() and release()
			// frameGrabber.stop();
			// frameGrabber.release();
			if (frameGrabber != null) {
				frameGrabber.close();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// TODO
	public static synchronized org.bytedeco.javacv.Frame grabFrame(FFmpegFrameGrabber frameGrabber, long timestamp) {

		try {
			frameGrabber.setVideoTimestamp(timestamp * 1000);
			return frameGrabber.grabFrame(false, true, true, false);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;

	}

}
