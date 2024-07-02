package org.glasspath.common.media.opencv;

import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_dnn;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_core.StringVector;
import org.bytedeco.opencv.opencv_dnn.Net;
import org.glasspath.common.Common;
import org.glasspath.common.GlasspathSystemProperties;

public class YoloV4ObjectDetectionModel implements DetectionModel {

	public static final String DEFAULT_TINY_CONFIG_FILE_NAME = "yolov4-tiny.cfg";
	public static final String DEFAULT_TINY_WEIGHTS_FILE_NAME = "yolov4-tiny.weights";
	public static final int DEFAULT_TINY_WIDTH = 416;
	public static final int DEFAULT_TINY_HEIGHT = 416;

	public static final String DEFAULT_CONFIG_FILE_NAME = "yolov4.cfg";
	public static final String DEFAULT_WEIGHTS_FILE_NAME = "yolov4.weights";
	public static final int DEFAULT_WIDTH = 608;
	public static final int DEFAULT_HEIGHT = 608;

	public static final String DEFAULT_P6_CONFIG_FILE_NAME = "yolov4-p6.cfg";
	public static final String DEFAULT_P6_WEIGHTS_FILE_NAME = "yolov4-p6.weights";
	public static final int DEFAULT_P6_WIDTH = 1280;
	public static final int DEFAULT_P6_HEIGHT = 1280;

	private final String configFileName;
	private final String weightsFileName;
	private final int width;
	private final int height;
	private Net net = null;
	private StringVector outputNames = null;

	public YoloV4ObjectDetectionModel() {
		this(DEFAULT_TINY_CONFIG_FILE_NAME, DEFAULT_TINY_WEIGHTS_FILE_NAME, DEFAULT_TINY_WIDTH, DEFAULT_TINY_HEIGHT);
	}

	public YoloV4ObjectDetectionModel(String configFileName, String weightsFileName, int width, int height) {
		this.configFileName = configFileName;
		this.weightsFileName = weightsFileName;
		this.width = width;
		this.height = height;
	}

	@Override
	public boolean load(int backend) {

		try {

			String configPath;
			String weightsPath;

			String modelsPath = System.getProperty(GlasspathSystemProperties.BUNDLED_MODELS_PATH);
			if (modelsPath != null) {
				configPath = modelsPath + "/" + configFileName;
				weightsPath = modelsPath + "/" + weightsFileName;
			} else {
				configPath = configFileName;
				weightsPath = weightsFileName;
			}

			net = opencv_dnn.readNetFromDarknet(configPath, weightsPath);

			if (backend == BACKEND_CUDA) {
				net.setPreferableBackend(opencv_dnn.DNN_BACKEND_CUDA);
				net.setPreferableTarget(opencv_dnn.DNN_TARGET_CUDA_FP16);
			}

			outputNames = net.getUnconnectedOutLayersNames();

			return true;

		} catch (Exception e) {
			Common.LOGGER.error("Exception while loading YoloV4 model", e);
		}

		return false;

	}

	@Override
	public List<DetectionResult> process(Mat mat, DetectionResultFilter detectionFilter) {

		long start = System.currentTimeMillis();

		// opencv_imgproc.resize(matImage, matImage, new Size(300, 300)); // Resize the image to match the input size of the model

		Mat blob = opencv_dnn.blobFromImage(mat, 0.00392, new Size(width, height), new Scalar(0.0, 0.0, 0.0, 0.0), true, false, opencv_core.CV_32F);

		net.setInput(blob);

		MatVector outputs = new MatVector();
		net.forward(outputs, outputNames);

		System.out.println("outputs.size() = " + outputs.size() + ", " + (System.currentTimeMillis() - start) + " ms");

		List<DetectionResult> results = getResults(outputs, detectionFilter);
		outputs.close();

		return results;

	}

	private List<DetectionResult> getResults(MatVector outputs, DetectionResultFilter resultFilter) {

		List<DetectionResult> results = new ArrayList<>();

		for (int i = 0; i < outputs.size(); i++) {

			Mat output = outputs.get(i);
			FloatIndexer indexer = output.createIndexer();

			// System.out.println(indexer.size(0) + ", " + indexer.size(1) + ", " + indexer.size(2));

			long boxCount = indexer.size(0);
			for (int box = 0; box < boxCount; box++) {

				float confidence = indexer.get(box, 4 + resultFilter.getDetectionClassifier());
				if (resultFilter.includeResult(confidence)) {

					int x = (int) (indexer.get(box, 0) * width);
					int y = (int) (indexer.get(box, 1) * height);
					int w = (int) (indexer.get(box, 2) * width);
					int h = (int) (indexer.get(box, 3) * height);

					if (resultFilter.includeResult(box, y, w, h)) {
						resultFilter.addResult(results, x - (w / 2), y - (h / 2), w, h, confidence);
						// results.add(new DetectionResult(x - (w / 2), y - (h / 2), w, h, confidence));
					}

				}

			}

			indexer.close();
			output.close();

		}

		return results;

	}

}
