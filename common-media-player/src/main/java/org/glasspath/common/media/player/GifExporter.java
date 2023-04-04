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
package org.glasspath.common.media.player;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

public class GifExporter {

	private ImageWriter imageWriter;
	private ImageWriteParam imageWriteParam;
	private IIOMetadata iioMetaData;

	public GifExporter(ImageOutputStream outputStream, int imageType, int imageInterval, boolean repeat) throws IIOException, IOException {

		Iterator<ImageWriter> iterator = ImageIO.getImageWritersBySuffix("gif");
		if (iterator.hasNext()) {

			imageWriter = iterator.next();
			imageWriteParam = imageWriter.getDefaultWriteParam();
			iioMetaData = imageWriter.getDefaultImageMetadata(ImageTypeSpecifier.createFromBufferedImageType(imageType), imageWriteParam);

			String nativeMetadataFormatName = iioMetaData.getNativeMetadataFormatName();
			IIOMetadataNode root = (IIOMetadataNode) iioMetaData.getAsTree(nativeMetadataFormatName);

			IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");
			graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
			graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
			graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE");
			graphicsControlExtensionNode.setAttribute("delayTime", Integer.toString(imageInterval / 10));
			graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");

			IIOMetadataNode commentExtensionsNode = getNode(root, "CommentExtensions");
			commentExtensionsNode.setAttribute("CommentExtension", "Glasspath.org");

			IIOMetadataNode applicationEntensionsNode = getNode(root, "ApplicationExtensions");

			IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");
			child.setAttribute("applicationID", "NETSCAPE");
			child.setAttribute("authenticationCode", "2.0");

			int dontRepeat = repeat ? 0 : 1;

			child.setUserObject(new byte[] { 0x1, (byte) (dontRepeat & 0xFF), (byte) ((dontRepeat >> 8) & 0xFF) });
			applicationEntensionsNode.appendChild(child);

			iioMetaData.setFromTree(nativeMetadataFormatName, root);

			imageWriter.setOutput(outputStream);
			imageWriter.prepareWriteSequence(null);

		} else {
			throw new IIOException("No image writers found for suffix 'gif'");
		}

	}

	public void writeImage(RenderedImage image) throws IOException {
		imageWriter.writeToSequence(new IIOImage(image, null, iioMetaData), imageWriteParam);
	}

	public void close() throws IOException {
		imageWriter.endWriteSequence();
	}

	public static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName) {

		int length = rootNode.getLength();
		for (int i = 0; i < length; i++) {
			if (rootNode.item(i).getNodeName().compareToIgnoreCase(nodeName) == 0) {
				return ((IIOMetadataNode) rootNode.item(i));
			}
		}

		IIOMetadataNode node = new IIOMetadataNode(nodeName);
		rootNode.appendChild(node);

		return node;

	}

}