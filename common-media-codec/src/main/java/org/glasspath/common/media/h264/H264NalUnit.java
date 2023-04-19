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
package org.glasspath.common.media.h264;

public class H264NalUnit {

	public static final byte[] NAL_START_PREFIX_CODE = {
			(byte) 0x00,
			(byte) 0x00,
			(byte) 0x00,
			(byte) 0x01
	};

	public static enum NalUnitType {

		UNKNOWN(-1),
		// TODO
		CODED_SLICE_IDR_PICTURE(5),
		SUPPLEMENTAL_ENHANCEMENT_INFORMATION(6),
		SEQUENCE_PARAMETER_SET(7),
		PICTURE_PARAMETER_SET(8),
		ACCESS_UNIT_DELIMITER(9),
		END_OF_SEQUENCE(10),
		END_OF_STREAM(11)
		// TODO
		;

		private final int typeValue;

		private NalUnitType(int typeValue) {
			this.typeValue = typeValue;
		}

		public int getTypeValue() {
			return typeValue;
		}

	}

	public static enum NalFragmentType {

		UNKNOWN(-1),
		NAL_UNIT(1, 23),
		STAP_A(24),
		STAP_B(25),
		MTAP16(26),
		MTAP24(27),
		FU_A(28),
		FU_B(29);

		private final int valueFrom;
		private final int valueTo;

		private NalFragmentType(int value) {
			this.valueFrom = value;
			this.valueTo = value;
		}

		private NalFragmentType(int valueFrom, int valueTo) {
			this.valueFrom = valueFrom;
			this.valueTo = valueTo;
		}

		public int getValueFrom() {
			return valueFrom;
		}

		public int getValueTo() {
			return valueTo;
		}

	}

	public final byte[] bytes;
	public final int nalUnitType;
	public final long timestamp;
	public final long receivedAt;

	public H264NalUnit(byte[] bytes, int nalUnitType, long timestamp, long receivedAt) {
		this.bytes = bytes;
		this.nalUnitType = nalUnitType;
		this.timestamp = timestamp;
		this.receivedAt = receivedAt;
	}

	public boolean isFrame() {
		return nalUnitType == 1 || nalUnitType == 97 || nalUnitType == 101; // TODO
	}

	public boolean isSequenceParameterSet() {
		return nalUnitType == 7; // TODO
	}

	public boolean isPictureParameterSet() {
		return nalUnitType == 8; // TODO
	}

	public boolean isIFrame() {
		return nalUnitType == 101; // TODO
	}

}
