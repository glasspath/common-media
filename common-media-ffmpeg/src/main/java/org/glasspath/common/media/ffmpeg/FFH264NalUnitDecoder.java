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

import java.nio.Buffer;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext.Get_format_AVCodecContext_IntPointer;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.ffmpeg.global.swscale;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder.Exception;

public class FFH264NalUnitDecoder {

	public static boolean TODO_DEBUG = false;
	public static boolean TODO_TEST_HW_DEVICE_CONTEXT = false;

	static {
		if (TODO_DEBUG) {
			org.bytedeco.ffmpeg.global.avutil.av_log_set_level(org.bytedeco.ffmpeg.global.avutil.AV_LOG_DEBUG);
		} else {
			org.bytedeco.ffmpeg.global.avutil.av_log_set_level(org.bytedeco.ffmpeg.global.avutil.AV_LOG_ERROR);
		}
	}

	static {
		try {
			FFmpegFrameGrabber.tryLoad();
//            FFmpegLockCallback.init();
		} catch (FFmpegFrameGrabber.Exception ex) {
		}
	}

	private int imageWidth = 1280;
	private int imageHeight = 720;
	private AVCodecContext video_c;
	private FFHWDeviceContextInfo hardwareContext;
	private Get_format_AVCodecContext_IntPointer hwPixelformatCallback;
	private AVFrame picture, picture_rgb;
	private BytePointer[] image_ptr;
	private Buffer[] image_buf;
	private AVPacket pkt;
	private SwsContext img_convert_ctx;
	private Frame frame;

	public FFH264NalUnitDecoder() {

	}

	public int getImageWidth() {
		return imageWidth;
	}

	public void setImageWidth(int imageWidth) {
		this.imageWidth = imageWidth;
	}

	public int getImageHeight() {
		return imageHeight;
	}

	public void setImageHeight(int imageHeight) {
		this.imageHeight = imageHeight;
	}

	public void start() {
		synchronized (org.bytedeco.ffmpeg.global.avcodec.class) {
			startUnsafe();
		}
	}

	private void startUnsafe() {

		pkt = new AVPacket().retainReference();
		pkt.stream_index(-1);

		AVCodec avCodec = avcodec.avcodec_find_decoder(avcodec.AV_CODEC_ID_H264);
		// AVCodec avCodec = avcodec.avcodec_find_decoder_by_name("libopenh264");
		// AVCodec avCodec = avcodec.avcodec_find_decoder_by_name("h264_cuvid");
		// AVCodec avCodec = avcodec.avcodec_find_decoder_by_name("h264_qsv");
		if (TODO_DEBUG) {
			System.out.println("FFH264NalUnitDecoder, avCodec.name: " + avCodec.name().getString());
		}

		video_c = avcodec.avcodec_alloc_context3(avCodec);

		if (TODO_TEST_HW_DEVICE_CONTEXT) {

			hardwareContext = FFmpegUtils.createHWDeviceContextInfo(avCodec, avutil.AV_HWDEVICE_TYPE_CUDA);
			// hardwareContext = FFmpegUtils.createHWDeviceContextInfo(avCodec, avutil.AV_HWDEVICE_TYPE_DXVA2);
			// hardwareContext = FFmpegUtils.createHWDeviceContextInfo(avCodec, avutil.AV_HWDEVICE_TYPE_D3D11VA);
			if (hardwareContext != null) {
				video_c.hw_device_ctx(hardwareContext.getHWDeviceContext());
				System.out.println("FFH264NalUnitDecoder, hardware device context set");
			}

			video_c.flags(video_c.flags() | avcodec.AV_CODEC_FLAG_LOW_DELAY);
			video_c.flags(video_c.flags() | avcodec.AV_CODEC_FLAG_OUTPUT_CORRUPT);
			video_c.flags2(video_c.flags2() | avcodec.AV_CODEC_FLAG2_SHOW_ALL);

			// video_c.err_recognition(video_c.err_recognition() | AVCodecContext.AV_EF_EXPLODE);

			video_c.thread_count(2);

			video_c.width(imageWidth);
			video_c.height(imageHeight);

			video_c.pix_fmt(avutil.AV_PIX_FMT_NV12);

			hwPixelformatCallback = new AVCodecContext.Get_format_AVCodecContext_IntPointer() {

				@Override
				public int call(AVCodecContext context, IntPointer pixelFormats) {

					int preferredPixelFormat;
					if (context.hw_device_ctx() != null && hardwareContext != null) {
						preferredPixelFormat = hardwareContext.getHWConfig().pix_fmt();
					} else {
						preferredPixelFormat = context.pix_fmt();
					}

					// First try to find preferred pixel format
					int i = 0;
					while (true) {

						int supportedFormat = pixelFormats.get(i++);
						if (supportedFormat == avutil.AV_PIX_FMT_NONE) {
							break;
						} else if (supportedFormat == preferredPixelFormat) {
							if (TODO_DEBUG) {
								System.out.println("FFH264NalUnitDecoder, preferred pixel format found: " + supportedFormat);
							}
							return supportedFormat;
						}

					}

					// Next try to find AV_PIX_FMT_YUV420P
					i = 0;
					while (true) {

						int supportedFormat = pixelFormats.get(i++);
						if (supportedFormat == avutil.AV_PIX_FMT_NONE) {
							break;
						} else if (supportedFormat == avutil.AV_PIX_FMT_YUV420P) {
							if (TODO_DEBUG) {
								System.out.println("FFH264NalUnitDecoder, first preferred pixel format not found, using: " + supportedFormat);
							}
							return supportedFormat;
						}
					}

					// Next try to find AV_PIX_FMT_NV12
					i = 0;
					while (true) {

						int supportedFormat = pixelFormats.get(i++);
						if (supportedFormat == avutil.AV_PIX_FMT_NONE) {
							break;
						} else if (supportedFormat == avutil.AV_PIX_FMT_NV12) {
							if (TODO_DEBUG) {
								System.out.println("FFH264NalUnitDecoder, first and second preferred pixel format not found, using: " + supportedFormat);
							}
							return supportedFormat;
						}

					}

					if (TODO_DEBUG) {
						System.out.println("FFH264NalUnitDecoder, no preferred pixel formats found, using: " + avutil.AV_PIX_FMT_NONE);
					}

					return avutil.AV_PIX_FMT_NONE;

				}
			};
			video_c.get_format(hwPixelformatCallback);

		} else {

			video_c.flags(video_c.flags() | avcodec.AV_CODEC_FLAG_LOW_DELAY);
			// video_c.flags2(video_c.flags2() | avcodec.AV_CODEC_CAP_TRUNCATED);
			// video_c.flags(video_c.flags() | avcodec.AV_CODEC_FLAG_LOW_DELAY);
			video_c.flags2(video_c.flags2() | avcodec.AV_CODEC_FLAG2_CHUNKS); // Without this flag we can get: [h264 @ 000001ef2a05b980] no frame!
			// video_c.thread_count(0);
			// video_c.profile(AVCodecContext.FF_PROFILE_H264_HIGH);

		}

		AVDictionary opts = new AVDictionary(null);
		avcodec.avcodec_open2(video_c, avCodec, opts);
		avutil.av_dict_free(opts);

		frame = new Frame();

		if ((picture = avutil.av_frame_alloc()) == null) {
			System.err.println("av_frame_alloc() error: Could not allocate raw picture frame.");
		}
		if ((picture_rgb = avutil.av_frame_alloc()) == null) {
			System.err.println("av_frame_alloc() error: Could not allocate RGB picture frame.");
		}

		initPictureRGB();

	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		release();
	}

	public void release() throws Exception {
		synchronized (org.bytedeco.ffmpeg.global.avcodec.class) {
			releaseUnsafe();
		}
	}

	public void releaseUnsafe() throws Exception {

		if (TODO_DEBUG) {
			System.out.println("Releasing FFH264NalUnitDecoder");
		}

		if (pkt != null) {
			if (pkt.stream_index() != -1) {
				avcodec.av_packet_unref(pkt);
			}
			pkt.releaseReference();
			pkt = null;
		}

		// Free the RGB image
		if (image_ptr != null) {
			for (int i = 0; i < image_ptr.length; i++) {
				avutil.av_free(image_ptr[i]);
			}
			image_ptr = null;
		}
		if (picture_rgb != null) {
			avutil.av_frame_free(picture_rgb);
			picture_rgb = null;
		}

		// Free the native format picture frame
		if (picture != null) {
			avutil.av_frame_free(picture);
			picture = null;
		}

		// Close the video codec
		if (video_c != null) {
			avcodec.avcodec_free_context(video_c);
			video_c = null;
		}

		if (hardwareContext != null) {
			hardwareContext.release();
			hardwareContext = null;
		}

		if (hwPixelformatCallback != null) {
			hwPixelformatCallback.close();
			hwPixelformatCallback = null;
		}

		if (img_convert_ctx != null) {
			swscale.sws_freeContext(img_convert_ctx);
			img_convert_ctx = null;
		}

		frame = null;

	}

	private void initPictureRGB() {

		int width = imageWidth;
		int height = imageHeight;

		// If size changes I new allocation is needed -> free the old one.
		if (image_ptr != null) {
			// First kill all references, then free it.
			image_buf = null;
			BytePointer[] temp = image_ptr;
			image_ptr = null;
			avutil.av_free(temp[0]);
		}
		int fmt = getPixelFormat();

		// work around bug in swscale: https://trac.ffmpeg.org/ticket/1031
		int align = 32;
		int stride = width;
		for (int i = 1; i <= align; i += i) {
			stride = (width + (i - 1)) & ~(i - 1);
			avutil.av_image_fill_linesizes(picture_rgb.linesize(), fmt, stride);
			if ((picture_rgb.linesize(0) & (align - 1)) == 0) {
				break;
			}
		}

		// Determine required buffer size and allocate buffer
		int size = avutil.av_image_get_buffer_size(fmt, stride, height, 1);
		image_ptr = new BytePointer[] { new BytePointer(avutil.av_malloc(size)).capacity(size) };
		image_buf = new Buffer[] { image_ptr[0].asBuffer() };

		// Assign appropriate parts of buffer to image planes in picture_rgb
		// Note that picture_rgb is an AVFrame, but AVFrame is a superset of AVPicture
		avutil.av_image_fill_arrays(new PointerPointer(picture_rgb), picture_rgb.linesize(), image_ptr[0], fmt, stride, height, 1);
		picture_rgb.format(fmt);
		picture_rgb.width(width);
		picture_rgb.height(height);

	}

	public int getPixelFormat() {
		return avutil.AV_PIX_FMT_BGR24;
	}

	public void flush() {
		avcodec.avcodec_flush_buffers(video_c);
	}

	public boolean decodeNalUnit(byte[] data, long timestamp, boolean processImage) {

		boolean nalUnitDecoded = false;

		// AVPacket avPacket = new AVPacket();
		// avcodec.av_init_packet(avPacket);

		if (pkt.stream_index() != -1) {
			// Free the packet that was allocated by av_read_frame
			avcodec.av_packet_unref(pkt);
			pkt.stream_index(-1);
		}

		BytePointer bytePointer = new BytePointer(data);
		bytePointer.capacity(data.length);

		// BytePointer bytePointer = new BytePointer(65_535 * 4);
		// bytePointer.position(0);
		// bytePointer.put(data);
		// bytePointer.limit(data.length);

		pkt.pts(avutil.AV_NOPTS_VALUE);
		pkt.dts(avutil.AV_NOPTS_VALUE);
		pkt.data(bytePointer);
		pkt.size(data.length);
		pkt.pos(-1);

		int result = avcodec.avcodec_send_packet(video_c, pkt);
		if (result == 0) {

			if (TODO_TEST_HW_DEVICE_CONTEXT) {

				AVFrame hwAvFrame = avutil.av_frame_alloc();

				result = avcodec.avcodec_receive_frame(video_c, hwAvFrame);
				if (result == 0) {
					result = avutil.av_hwframe_transfer_data(picture, hwAvFrame, 0);
				}

				avutil.av_frame_unref(hwAvFrame);

			} else {
				// TODO: Can we skip this when we don't have to return a frame?
				result = avcodec.avcodec_receive_frame(video_c, picture);

			}

			if (result == 0) {

				if (processImage) {

					frame.image = image_buf;
					try {
						processImage();
					} catch (Exception e) {
						e.printStackTrace();
					}

					frame.timestamp = timestamp; // TODO?
					frame.keyFrame = picture.key_frame() != 0;

					nalUnitDecoded = true;

				}

			} else if (TODO_DEBUG && processImage) {
				System.err.println("FFH264NalUnitDecoder, decode result = " + result);
			}

		}

		bytePointer.close();

		return nalUnitDecoded;

	}

	private void processImage() throws Exception {

		frame.imageWidth = imageWidth;
		frame.imageHeight = imageHeight;
		frame.imageDepth = Frame.DEPTH_UBYTE;

		// Has the size changed?
		if (frame.imageWidth != picture_rgb.width() || frame.imageHeight != picture_rgb.height()) {
			initPictureRGB();
		}

		// Convert the image into BGR or GRAY format that OpenCV uses
		if (TODO_TEST_HW_DEVICE_CONTEXT) {

			img_convert_ctx = swscale.sws_getCachedContext(img_convert_ctx,
					video_c.width(), video_c.height(), avutil.AV_PIX_FMT_NV12, // TODO: How to check which pixel format we need here?
					frame.imageWidth, frame.imageHeight, getPixelFormat(),
					swscale.SWS_FAST_BILINEAR, null, null, (DoublePointer) null);

		} else {

			img_convert_ctx = swscale.sws_getCachedContext(img_convert_ctx,
					video_c.width(), video_c.height(), video_c.pix_fmt(),
					frame.imageWidth, frame.imageHeight, getPixelFormat(),
					swscale.SWS_FAST_BILINEAR, null, null, (DoublePointer) null);

		}

		if (img_convert_ctx == null) {
			throw new Exception("sws_getCachedContext() error: Cannot initialize the conversion context.");
		}

		// TODO: Experimental..
		// int[] dummy = new int[4];
		// int[] srcRange = new int[4], dstRange = new int[4];
		// int[] brightness = new int[4], contrast = new int[4], saturation = new int[4];
		// org.bytedeco.ffmpeg.global.swscale.sws_getColorspaceDetails(img_convert_ctx, dummy, srcRange, dummy, dstRange, brightness, contrast, saturation);
		// IntPointer coefs = org.bytedeco.ffmpeg.global.swscale.sws_getCoefficients(org.bytedeco.ffmpeg.global.swscale.SWS_CS_DEFAULT);
		// srcRange[0] = 1; // this marks that values are according to yuvj
		// org.bytedeco.ffmpeg.global.swscale.sws_setColorspaceDetails(img_convert_ctx, coefs, srcRange[0], coefs, dstRange[0], brightness[0], contrast[0], saturation[0]);

		// Convert the image from its native format to RGB or GRAY
		swscale.sws_scale(img_convert_ctx, new PointerPointer(picture), picture.linesize(), 0, video_c.height(), new PointerPointer(picture_rgb), picture_rgb.linesize());

		frame.imageStride = picture_rgb.linesize(0);
		frame.image = image_buf;
		frame.opaque = picture_rgb;

		if (frame.image[0] == null) {
			System.err.println("frame.image[0] == null");
		} else {
			frame.image[0].limit(frame.imageHeight * frame.imageStride);
			frame.imageChannels = frame.imageStride / frame.imageWidth;
		}

	}

	public Frame getFrame() {
		return frame;
	}

	public void close() {

		if (TODO_DEBUG) {
			System.out.println("Closing FFH264NalUnitDecoder");
		}

		if (image_ptr != null) {
			// First kill all references, then free it.
			image_buf = null;
			BytePointer[] temp = image_ptr;
			image_ptr = null;
			avutil.av_free(temp[0]);
		}

		// TODO: What else needs to be closed/released?

	}

}
