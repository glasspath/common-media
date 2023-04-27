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
package org.glasspath.common.media.ffmpeg.player;

import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber.Exception;
import org.glasspath.common.media.ffmpeg.FFVideoFrameConverter;
import org.glasspath.common.media.player.IVideoPlayer;
import org.glasspath.common.media.player.VideoFramePlayerPanel;
import org.glasspath.common.media.video.Frame;
import org.glasspath.common.media.video.FrameBuffer;
import org.glasspath.common.media.video.FrameBuffer.BufferedFrame;
import org.glasspath.common.media.video.Video;

import com.jhlabs.image.ContrastFilter;

public class FFVideoPlayerPanel extends VideoFramePlayerPanel {

	public static boolean TODO_DEBUG = false;
	public static boolean TODO_TEST_QSV_DECODER = false;

	static {
		if (!TODO_DEBUG) {
			org.bytedeco.ffmpeg.global.avutil.av_log_set_level(org.bytedeco.ffmpeg.global.avutil.AV_LOG_ERROR);
		}
	}

	private final FFBufferedFrame[] buffer;
	private final FrameBuffer<org.bytedeco.javacv.Frame> frameBuffer;
	private final FFVideoFrameConverter frameConverter = new FFVideoFrameConverter();
	private long duration = 0L;
	private double frameRate = 0.0;
	private int interval = 0;
	private int bufferIndex = 0;
	private boolean exit = false;

	public FFVideoPlayerPanel(IVideoPlayer context, Video video) {
		super(context, video);

		buffer = new FFBufferedFrame[10];
		for (int i = 0; i < buffer.length; i++) {
			buffer[i] = new FFBufferedFrame();
		}

		ContrastFilter contrastFilter = new ContrastFilter();
		contrastFilter.setBrightness(1.5F);
		contrastFilter.setContrast(2.0F);

		frameBuffer = new FrameBuffer<org.bytedeco.javacv.Frame>() {

			private FFmpegFrameGrabber frameGrabber = null;

			@Override
			protected BufferedFrame<org.bytedeco.javacv.Frame>[] createBuffer() {
				return buffer;
			}

			@Override
			protected boolean createDecoder() {

				frameGrabber = new FFmpegFrameGrabber(video.getPath());
				// frameGrabber.setVideoCodecName("h264_cuvid");
				if (TODO_TEST_QSV_DECODER) {
					frameGrabber.setVideoCodecName("h264_qsv");
				}

				// frameGrabber.setImageMode(ImageMode.RAW);
				// frameGrabber.setOption("hwaccel", "videotoolbox");
				// frameGrabber.setOption("hwaccel", "cuvid");
				// frameGrabber.setOption("hwaccel", "h264_videotoolbox");
				// frameGrabber.setOption("hwaccel", "h264_cuvid");
				// frameGrabber.setOption("hwaccel", "vp8");

				// frameGrabber.setOption("hwaccel", "cuvid");
				// frameGrabber.setVideoCodecName("h264_cuvid");
				// frameGrabber.setVideoCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264);

				// frameGrabber.setVideoOption("threads", "1");
				// --enable-dxva2
				// frameGrabber.setOption("-enable-dxva2", ""); //Attempt to add --enable-dxva2 option
				// frameGrabber.setOption("hwaccel", "dxva2");
				// frameGrabber.setPixelFormat(org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_DXVA2_VLD);

				// frameGrabber.setImageScalingFlags(org.bytedeco.ffmpeg.global.swscale.SWS_FAST_BILINEAR);

				// TODO
				// int cores = Runtime.getRuntime().availableProcessors(); // Includes hyper threading cores
				// int threads = cores + 1;
				// System.out.println("Setting threads option to: " + threads);
				// frameGrabber.setVideoOption("threads", "" + threads);
				// frameGrabber.setVideoOption("threads", "" + 4);

				try {

					frameGrabber.start();

					context.fireVideoOpened(video.getPath());

					duration = frameGrabber.getLengthInTime();
					frameRate = frameGrabber.getFrameRate();
					
					if (TODO_TEST_QSV_DECODER) {
						duration /= 10;
					}

					if (TODO_DEBUG) {
						System.out.println("FFVideoPlayerPanel, video codec: " + frameGrabber.getVideoCodecName());
						System.out.println("FFVideoPlayerPanel, duration: " + duration);
						System.out.println("FFVideoPlayerPanel, frameRate: " + frameRate);
					}

					if (frameRate > 1) {
						interval = (int) (1000 / frameRate) - 1; // TODO: Added -1 as a test to get closer to the desired fps
					} else {
						interval = -1;
					}

					/*
					final double scale = (getWidth() - 30) / 1280.0;
					final int width = (int)(1280 * scale);
					final int height = (int)(720 * scale);
					
					frameGrabber.setImageWidth(width);
					frameGrabber.setImageHeight(height);
					 */

					return interval > 0;

				} catch (Exception e) {
					e.printStackTrace();
					context.fireVideoClosed(video.getPath());
				}

				return false;

			}

			@Override
			protected long getDecoderTimestamp(org.bytedeco.javacv.Frame source) {
				if (TODO_TEST_QSV_DECODER) {
					return frameGrabber.getTimestamp() / 1000;
				} else {
					return frameGrabber.getTimestamp();
				}
			}

			@Override
			protected void setDecoderTimestamp(long timestamp) {
				try {
					if (TODO_TEST_QSV_DECODER) {
						frameGrabber.setVideoTimestamp(timestamp / 10000);
					} else {
						frameGrabber.setVideoTimestamp(timestamp);
					}
				} catch (org.bytedeco.javacv.FFmpegFrameGrabber.Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			protected org.bytedeco.javacv.Frame decode() {

				org.bytedeco.javacv.Frame frame = decodeFrame();

				if (frame == null) {

					// TODO: Check if we are at the end of the video?
					setDecoderTimestamp(0);

					if (isRepeatEnabled()) {
						frame = decodeFrame();
					} else {
						// Returning null will result in DECODE_FAILED which will cause the player to stop
					}

				}

				return frame;

			}

			@Override
			protected BufferedImage convert(org.bytedeco.javacv.Frame source) {
				return frameConverter.createBufferedImage(source);
			}

			@Override
			protected void filter(BufferedImage image) {
				if (image != null) { // TODO: Shouldn't be possible..
					// contrastFilter.filter(image, image);
				}
			}

			@Override
			protected void closeDecoder() {

				if (frameGrabber != null) {
					try {
						System.out.println("Closing frame grabber");
						frameGrabber.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			}

			private org.bytedeco.javacv.Frame decodeFrame() {
				try {
					org.bytedeco.javacv.Frame frame = frameGrabber.grabFrame(false, true, true, false);
					if (frame != null) {
						return frame.clone(); // FFmpegFrameGrabber returns same instance each time, so for buffering we need to clone
					}
				} catch (org.bytedeco.javacv.FFmpegFrameGrabber.Exception e) {
					e.printStackTrace();
				}
				return null;
			}
		};

	}

	@Override
	protected Frame getFrame() {

		if (buffer[bufferIndex].isImageReady()) {

			Frame frame = new Frame();
			frame.setTimestamp(buffer[bufferIndex].getTimestamp());
			frame.setImage(buffer[bufferIndex].getImage());

			buffer[bufferIndex].reset();

			bufferIndex++;
			if (bufferIndex >= buffer.length) {
				bufferIndex = 0;
			}

			return frame;

		} else if (buffer[bufferIndex].getState() == BufferedFrame.DECODE_FAILED) {

			buffer[bufferIndex].reset();

			bufferIndex++;
			if (bufferIndex >= buffer.length) {
				bufferIndex = 0;
			}

			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					setPlaying(false);
				}
			});

			return null;

		} else {
			return null;
		}

	}

	@Override
	public void videoPlayerShown() {
		super.videoPlayerShown();
		frameBuffer.start();
	}

	@Override
	public void setPlaying(boolean playing) {
		super.setPlaying(playing);

		/*
		if (playing) {
		
			frameBuffer.reset();
			frameBuffer.seek(getTimestamp());
			bufferIndex = 0;
			frameBuffer.resume();
		
		}
		 */

	}

	@Override
	public void setTimestamp(long timestamp, boolean play) {

		frameBuffer.reset();
		frameBuffer.seek(timestamp);
		bufferIndex = 0;
		frameBuffer.resume();

		super.setTimestamp(timestamp, play);

	}

	@Override
	public long getDuration() {
		return duration;
	}

	@Override
	public void exit() {

		exit = true;

		frameBuffer.exit();

	}

	@Override
	public boolean isExited() {
		return super.isExited() && exit && (frameBuffer == null || frameBuffer.isExited()); // TODO?
	}

	public static class FFBufferedFrame extends BufferedFrame<org.bytedeco.javacv.Frame> {

		public FFBufferedFrame() {

		}

	}

}
