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
package org.glasspath.common.media.rtsp;

import java.util.Base64;

import org.glasspath.common.media.h264.H264NalUnit;
import org.glasspath.common.media.rtsp.RtpPacket.NalUnitType;

public class H264ParameterSets {

	public H264NalUnit sequenceParameterSet = null;
	public H264NalUnit pictureParameterSet = null;
	public boolean spropParmeterSets = false;

	public H264ParameterSets() {

	}

	public void parseSpropParameterSets(String spropParameterSets) {

		int indexOfComma;
		if (spropParameterSets != null && spropParameterSets.length() > 0 && (indexOfComma = spropParameterSets.indexOf(",")) > 0) {

			String spsString = spropParameterSets.substring(0, indexOfComma);
			byte[] spsBytes = Base64.getDecoder().decode(spsString);
			byte[] sps = new byte[H264NalUnit.NAL_START_PREFIX_CODE.length + spsBytes.length];
			System.arraycopy(H264NalUnit.NAL_START_PREFIX_CODE, 0, sps, 0, H264NalUnit.NAL_START_PREFIX_CODE.length);
			System.arraycopy(spsBytes, 0, sps, H264NalUnit.NAL_START_PREFIX_CODE.length, spsBytes.length);
			sequenceParameterSet = new H264NalUnit(sps, NalUnitType.SEQUENCE_PARAMETER_SET.getTypeValue(), 0, System.currentTimeMillis()); // TODO: Timestamp?

			String ppsString = spropParameterSets.substring(indexOfComma + 1);
			byte[] ppsBytes = Base64.getDecoder().decode(ppsString);
			byte[] pps = new byte[H264NalUnit.NAL_START_PREFIX_CODE.length + ppsBytes.length];
			System.arraycopy(H264NalUnit.NAL_START_PREFIX_CODE, 0, pps, 0, H264NalUnit.NAL_START_PREFIX_CODE.length);
			System.arraycopy(ppsBytes, 0, pps, H264NalUnit.NAL_START_PREFIX_CODE.length, ppsBytes.length);
			pictureParameterSet = new H264NalUnit(pps, NalUnitType.PICTURE_PARAMETER_SET.getTypeValue(), 0, System.currentTimeMillis()); // TODO: Timestamp?

			spropParmeterSets = true;

		} else {
			spropParmeterSets = false;
		}

	}

	public RtpPacket createSequenceParameterSetRtpPacket() {

		if (sequenceParameterSet != null) {

			// TODO!

		}

		return null;

	}

}
