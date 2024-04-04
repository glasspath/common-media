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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class VideoExportPreset {

	public static final String NAME_PREFIX = "name:";
	public static final String COMMENTS_PREFIX = "comments:";
	public static final String EXTENSION_PREFIX = "extension:";
	public static final String PRIORITY_PREFIX = "priority:";
	public static final String EXPORTER_PREFIX = "exporter:";
	public static final String ARGUMENTS_PREFIX = "arguments:";

	public static final String EXPORTER_NONE = "none";
	public static final String EXPORTER_FFMPEG = "ffmpeg";

	private String name = "";
	private String comments = "";
	private String extension = "";
	private int priority = 0;
	private String exporter = "";
	private List<String> arguments = null;

	public VideoExportPreset() {

	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public String getExtension() {
		return extension;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public String getExporter() {
		return exporter;
	}

	public void setExporter(String exporter) {
		this.exporter = exporter;
	}

	public List<String> getArguments() {
		return arguments;
	}

	public void setArguments(List<String> arguments) {
		this.arguments = arguments;
	}

	public boolean isExporterNone() {
		return exporter == null || exporter.trim().toLowerCase().equals(EXPORTER_NONE);
	}

	public boolean isExporterFFmpeg() {
		return exporter != null && exporter.trim().toLowerCase().equals(EXPORTER_FFMPEG);
	}

	public static List<VideoExportPreset> loadPresets(File presetsFile) {

		List<VideoExportPreset> presets = new ArrayList<>();

		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(presetsFile))) {

			VideoExportPreset preset = null;

			String line;
			while ((line = bufferedReader.readLine()) != null) {

				if (line.toLowerCase().startsWith(NAME_PREFIX)) {

					preset = new VideoExportPreset();
					presets.add(preset);

					preset.setName(line.substring(NAME_PREFIX.length()).trim());

				} else if (preset != null) {

					if (line.trim().length() == 0) {
						preset = null;
					} else if (line.toLowerCase().startsWith(COMMENTS_PREFIX)) {
						preset.setComments(line.substring(COMMENTS_PREFIX.length()).trim());
					} else if (line.toLowerCase().startsWith(EXTENSION_PREFIX)) {
						preset.setExtension(line.substring(EXTENSION_PREFIX.length()).trim());
					} else if (line.toLowerCase().startsWith(PRIORITY_PREFIX)) {
						try {
							preset.setPriority(Integer.parseInt(line.substring(PRIORITY_PREFIX.length()).trim()));
						} catch (Exception e) {
							// Can fail..
						}
					} else if (line.toLowerCase().startsWith(EXPORTER_PREFIX)) {
						preset.setExporter(line.substring(EXPORTER_PREFIX.length()).trim());
					} else if (line.toLowerCase().startsWith(ARGUMENTS_PREFIX)) {
						preset.setArguments(new ArrayList<>());
					} else if (preset.getArguments() != null) {
						preset.getArguments().add(line.trim());
					}

				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return presets;

	}

}
