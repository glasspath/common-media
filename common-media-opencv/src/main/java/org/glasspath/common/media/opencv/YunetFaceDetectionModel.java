package org.glasspath.common.media.opencv;

import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_dnn;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_core.StringVector;
import org.bytedeco.opencv.opencv_dnn.Net;
import org.glasspath.common.Common;
import org.glasspath.common.GlasspathSystemProperties;

public class YunetFaceDetectionModel implements DetectionModel {

	public static final String DEFAULT_MODEL_FILE_NAME = "face_detection_yunet_2023mar.onnx";

	private final String modelFileName;
	private final int modelInputWidth;
	private final int modelInputHeight;
	private final boolean resize;
	private final double scaleX;
	private final double scaleY;
	private Net net = null;
	private StringVector outputNames = null;
	private int[] strides = null;
	private int divisor = 0;
	private int padW = 0;
	private int padH = 0;

	public YunetFaceDetectionModel(int width, int height) {
		this(DEFAULT_MODEL_FILE_NAME, width, height, width, height);
	}

	public YunetFaceDetectionModel(int imageWidth, int imageHeight, int modelInputWidth, int modelInputHeight) {
		this(DEFAULT_MODEL_FILE_NAME, imageWidth, imageHeight, modelInputWidth, modelInputHeight);
	}

	public YunetFaceDetectionModel(String modelFileName, int imageWidth, int imageHeight, int modelInputWidth, int modelInputHeight) {
		this.modelFileName = modelFileName;
		this.modelInputWidth = modelInputWidth;
		this.modelInputHeight = modelInputHeight;
		this.resize = imageWidth != modelInputWidth || imageHeight != modelInputHeight;
		this.scaleX = imageWidth == modelInputWidth ? 1.0 : (double) imageWidth / (double) modelInputWidth;
		this.scaleY = imageHeight == modelInputHeight ? 1.0 : (double) imageHeight / (double) modelInputHeight;
	}

	@Override
	public boolean load(int backend) {

		try {

			String path;

			String modelsPath = System.getProperty(GlasspathSystemProperties.BUNDLED_MODELS_PATH);
			if (modelsPath != null) {
				path = modelsPath + "/" + modelFileName;
			} else {
				path = modelFileName;
			}

			Common.LOGGER.debug("YunetFaceDetectionModel: opencv_core.getNumThreads() = " + opencv_core.getNumThreads());
			// opencv_core.setNumThreads(8);

			net = opencv_dnn.readNetFromONNX(path);

			/* TODO?
			if (backend == BACKEND_CUDA) {
				net.setPreferableBackend(opencv_dnn.DNN_BACKEND_CUDA);
				net.setPreferableTarget(opencv_dnn.DNN_TARGET_CUDA_FP16);
			}
			*/

			outputNames = new StringVector();
			outputNames.put("cls_8", "cls_16", "cls_32", "obj_8", "obj_16", "obj_32", "bbox_8", "bbox_16", "bbox_32", "kps_8", "kps_16", "kps_32");

			strides = new int[] { 8, 16, 32 };
			divisor = 32;

			// TODO: Implement 'padWithDivisor()' method from OpenCV source?
			padW = ((int) ((modelInputWidth - 1.0) / divisor) + 1) * divisor;
			padH = ((int) ((modelInputHeight - 1.0) / divisor) + 1) * divisor;
			// System.out.println("width = " + width + ", height = " + height);
			// System.out.println("padW = " + padW + ", padH = " + padH);

			return true;

		} catch (Exception e) {
			Common.LOGGER.error("Exception while loading Yunet model", e);
		}

		return false;

	}

	@Override
	public List<DetectionResult> process(Mat mat, DetectionResultFilter detectionFilter) {

		// long start = System.currentTimeMillis();

		Mat inputMat;

		if (resize) {
			inputMat = new Mat();
			opencv_imgproc.resize(mat, inputMat, new Size(modelInputWidth, modelInputHeight));
		} else {
			inputMat = mat;
		}

		// Mat blob = blobFromImage(matImage, 1.0, new Size(modelInputWidth, modelInputHeight), new Scalar(104.0, 177.0, 123.0, 0.0), false, false, CV_32F);
		Mat blob = opencv_dnn.blobFromImage(inputMat, 1.0, new Size(modelInputWidth, modelInputHeight), new Scalar(0.0, 0.0, 0.0, 0.0), false, false, opencv_core.CV_32F);
		// Mat blob = opencv_dnn.blobFromImage(mat, 1.0, new Size(modelInputWidth, modelInputHeight), new Scalar(50.0, 50.0, 50.0, 0.0), false, false, opencv_core.CV_32F);
		// Mat blob = opencv_dnn.blobFromImage(mat, 1.0, new Size(modelInputWidth, modelInputHeight), new Scalar(150.0, 150.0, 150.0, 0.0), false, false, opencv_core.CV_32F);
		// Mat blob = opencv_dnn.blobFromImage(matImage);

		net.setInput(blob);

		MatVector outputs = new MatVector();
		net.forward(outputs, outputNames);

		// System.out.println("outputs.size() = " + outputs.size() + ", " + (System.currentTimeMillis() - start) + " ms");

		List<DetectionResult> results = getResults(outputs, detectionFilter);
		outputs.close();

		if (resize) {
			inputMat.close();
		}

		return results;

	}

	private List<DetectionResult> getResults(MatVector outputs, DetectionResultFilter resultFilter) {

		List<DetectionResult> results = new ArrayList<>();

		if (outputs.size() == 12) {

			for (int i = 0; i < strides.length; i++) {

				int cols = (int) ((double) padW / strides[i]);
				int rows = (int) ((double) padH / strides[i]);

				// Extract from output_blobs
				Mat cls = outputs.get(i);
				Mat obj = outputs.get(i + strides.length * 1);
				Mat bbox = outputs.get(i + strides.length * 2);
				Mat kps = outputs.get(i + strides.length * 3);

				FloatIndexer clsIndexer = cls.createIndexer();
				FloatIndexer objIndexer = obj.createIndexer();
				FloatIndexer bboxIndexer = bbox.createIndexer();
				FloatIndexer kpsIndexer = kps.createIndexer();

				/*
				System.out.println("CHECK");
				System.out.println(clsIndexer.size(0) + ", " + clsIndexer.size(1) + ", " + clsIndexer.size(2) + ", " + clsIndexer.size(3));
				System.out.println(objIndexer.size(0) + ", " + objIndexer.size(1) + ", " + objIndexer.size(2) + ", " + objIndexer.size(3));
				System.out.println(bboxIndexer.size(0) + ", " + bboxIndexer.size(1) + ", " + bboxIndexer.size(2) + ", " + bboxIndexer.size(3));
				System.out.println(kpsIndexer.size(0) + ", " + kpsIndexer.size(1) + ", " + kpsIndexer.size(2) + ", " + kpsIndexer.size(3));
				System.out.println("rows = " + rows + ", cols = " + cols + ", rows*cols = " + (rows * cols));
				*/

				for (int r = 0; r < rows; r++) {

					for (int c = 0; c < cols; c++) {

						int idx = r * cols + c;

						float clsScore = clsIndexer.get(0, idx, 0, 0);
						float objScore = objIndexer.get(0, idx, 0, 0);

						if (clsScore < 0.0) {
							clsScore = 0.0F;
						} else if (clsScore > 1.0) {
							clsScore = 1.0F;
						}
						if (objScore < 0.0) {
							objScore = 0.0F;
						} else if (objScore > 1.0) {
							objScore = 1.0F;
						}
						double score = Math.sqrt(clsScore * objScore);
						if (resultFilter.includeResult(score)) {

							// System.out.println(score);

							double cx = (c + bboxIndexer.get(0, idx, 0, 0)) * strides[i];
							double cy = (r + bboxIndexer.get(0, idx, 1, 0)) * strides[i];
							double w = Math.exp(bboxIndexer.get(0, idx, 2, 0)) * strides[i];
							double h = Math.exp(bboxIndexer.get(0, idx, 3, 0)) * strides[i];

							double x = cx - w / 2;
							double y = cy - h / 2;

							if (resize) {
								x *= scaleX;
								y *= scaleY;
								w *= scaleX;
								h *= scaleY;
							}

							if (resultFilter.includeResult((int) x, (int) y, (int) w, (int) h)) {
								resultFilter.addResult(results, (int) x, (int) y, (int) w, (int) h, score);
							}

						}

					}

				}

				/*
				for (int r = 0; r < rows; r++) {
				
					for (int c = 0; c < cols; c++) {
				
						// Get score
						float cls_score = clsIndexer.get(r, c, 0);
						float obj_score = objIndexer.get(r, c, 0);
				
						// Clamp
						if (cls_score < 0.0) {
							cls_score = 0.0F;
						} else if (cls_score > 1.0) {
							cls_score = 1.0F;
						}
						if (obj_score < 0.0) {
							obj_score = 0.0F;
						} else if (obj_score > 1.0) {
							obj_score = 1.0F;
						}
						double score = Math.sqrt(cls_score * obj_score);
						if (score > THRESHOLD) {
							System.out.println(score);
						}
				
						face.at<float>(0, 14) = score;
						
						// Checking if the score meets the threshold before adding the face
						if (score < scoreThreshold)
						    continue;
						// Get bounding box
						float cx = ((c + bbox_v[idx * 4 + 0]) * strides[i]);
						float cy = ((r + bbox_v[idx * 4 + 1]) * strides[i]);
						float w = exp(bbox_v[idx * 4 + 2]) * strides[i];
						float h = exp(bbox_v[idx * 4 + 3]) * strides[i];
						
						float x1 = cx - w / 2.f;
						float y1 = cy - h / 2.f;
						
						face.at<float>(0, 0) = x1;
						face.at<float>(0, 1) = y1;
						face.at<float>(0, 2) = w;
						face.at<float>(0, 3) = h;
						
						// Get landmarks
						for(int n = 0; n < 5; ++n) {
						    face.at<float>(0, 4 + 2 * n) = (kps_v[idx * 10 + 2 * n] + c) * strides[i];
						    face.at<float>(0, 4 + 2 * n + 1) = (kps_v[idx * 10 + 2 * n + 1]+ r) * strides[i];
						}
						faces.push_back(face);
				
					}
				
				}
				*/

				clsIndexer.close();
				objIndexer.close();
				bboxIndexer.close();
				kpsIndexer.close();

				cls.close();
				obj.close();
				bbox.close();
				kps.close();

			}

		}

		return results;

	}

}
