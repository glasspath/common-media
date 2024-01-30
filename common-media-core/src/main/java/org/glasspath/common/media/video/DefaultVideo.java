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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DefaultVideo extends Video {

	public static boolean TODO_DEBUG = false;

	private final FrameLoader frameLoader;
	private int width = Resolution.W1280_H720.getWidth();
	private int height = Resolution.W1280_H720.getHeight();
	private long timestamp = 0L;
	private long duration = 0L;
	private Double frameRate = null;
	private Long creationDate = null;
	private final List<MetadataTimestamp> metadataTimestamps = new ArrayList<>();

	public DefaultVideo(String name, String path) {
		this(name, path, null, true, Resolution.W1280_H720.getWidth(), Resolution.W1280_H720.getHeight());
	}

	public DefaultVideo(String name, String path, FrameLoader frameLoader) {
		this(name, path, null, true, Resolution.W1280_H720.getWidth(), Resolution.W1280_H720.getHeight());
	}

	public DefaultVideo(String name, String path, FrameLoader frameLoader, boolean closeFile) {
		this(name, path, frameLoader, closeFile, Resolution.W1280_H720.getWidth(), Resolution.W1280_H720.getHeight());
	}

	public DefaultVideo(String name, String path, FrameLoader frameLoader, boolean closeFile, int width, int height) {
		super(name, path);

		this.frameLoader = frameLoader;
		this.width = width;
		this.height = height;

		if (frameLoader != null) {
			frameLoader.installOnVideo(this, width, height, closeFile);
		}

	}

	public FrameLoader getFrameLoader() {
		return frameLoader;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public double getFrom() {
		return timestamp;
	}

	public double getTo() {
		return timestamp + duration;
	}

	public Double getFrameRate() {
		return frameRate;
	}

	public void setFrameRate(Double frameRate) {
		this.frameRate = frameRate;
	}

	public Long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Long creationDate) {
		this.creationDate = creationDate;
	}

	public List<MetadataTimestamp> getMetadataTimestamps() {
		return metadataTimestamps;
	}

	public long getStartTimeCorrectionOffset() {
		return 0;
	}

	public void initTimestamp() {

		Long startTimestamp = parseDateTime(getName());

		if (startTimestamp == null) {

			String nameLowerCase;
			for (MetadataTimestamp metadataTimestamp : metadataTimestamps) {

				if (metadataTimestamp.getName() != null && metadataTimestamp.getTimestamp() != null) {

					nameLowerCase = metadataTimestamp.getName().toLowerCase();
					if (nameLowerCase.contains("start") || nameLowerCase.contains("begin")) {
						if (startTimestamp == null || metadataTimestamp.getTimestamp() < startTimestamp) {
							// System.out.println(metadataTimestamp.getTimestampString());
							startTimestamp = metadataTimestamp.getTimestamp();
						}
					}

				}

			}

		}

		if (startTimestamp != null) {
			timestamp = startTimestamp;
		} else {

			if (creationDate != null) {
				timestamp = creationDate;
			} else {
				timestamp = System.currentTimeMillis();
			}

		}

		// timestamp = System.currentTimeMillis();
		// System.out.println("Video start timestamp: " + new Date(timestamp));

	}

	public static Long parseDateTime(String dateTimeString) {

		String s = dateTimeString.replaceAll("[^0-9]+", " ");
		String[] intStrings = s.trim().split(" ");

		if (intStrings.length >= 6) {

			Calendar calendar = Calendar.getInstance();

			int[] intValues = new int[intStrings.length];

			int yearIndex = 0;
			int monthIndex = 1;
			int dayIndex = 2;
			int hourIndex = 3;
			int minuteIndex = 4;
			int secondIndex = 5;

			for (int i = 0; i < intStrings.length; i++) {

				try {

					intValues[i] = Integer.parseInt(intStrings[i]);

					if (intValues[i] >= 1970 && intValues[i] <= 2500) {
						yearIndex = i;
					}

				} catch (Exception e) {
					// TODO?
				}

			}

			if (yearIndex <= 2) {

				if (yearIndex == 2) {
					monthIndex = 1;
					dayIndex = 0;
				} else if (yearIndex == 1) { // Would be very unusual
					monthIndex = 0;
					dayIndex = 2;
				} else {
					monthIndex = 1;
					dayIndex = 2;
				}

				// Swap dayIndex and monthIndex if month is higher than December
				if (intValues[monthIndex] > 12) {
					int temp = dayIndex;
					dayIndex = monthIndex;
					monthIndex = temp;
				}

				calendar.set(Calendar.YEAR, intValues[yearIndex]);
				calendar.set(Calendar.MONTH, intValues[monthIndex] - 1);
				calendar.set(Calendar.DAY_OF_MONTH, intValues[dayIndex]);
				calendar.set(Calendar.HOUR_OF_DAY, intValues[hourIndex]);
				calendar.set(Calendar.MINUTE, intValues[minuteIndex]);
				calendar.set(Calendar.SECOND, intValues[secondIndex]);

			}

			return calendar.getTimeInMillis();

		}

		if (TODO_DEBUG) {
			System.err.println("TODO: Parse date with format: " + dateTimeString);
		}

		return null;

	}

	public static class MetadataTimestamp {

		private final String name;
		private final String timestampString;
		private Long timestamp = null;

		public MetadataTimestamp(String name, String timestampString) {
			this.name = name;
			this.timestampString = timestampString;
			parseTimestampString(null);
		}

		public String getName() {
			return name;
		}

		public String getTimestampString() {
			return timestampString;
		}

		public Long getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(Long timestamp) {
			this.timestamp = timestamp;
		}

		public void parseTimestampString(String format) {
			if (format == null) {
				timestamp = parseDateTime(timestampString);
			} else {
				// TODO
			}
		}

	}

}
