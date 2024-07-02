package org.glasspath.common.media.opencv;

import java.util.List;

public class PersonTracker extends ObjectTracker<DetectionResult> implements DetectionResultFilter {

	private double detectionThreshold = 0.25;
	private double trackingThreshold = 0.5;
	private double maxTrackingDistance = 25.0;

	public PersonTracker() {

	}

	public double getDetectionThreshold() {
		return detectionThreshold;
	}

	public void setDetectionThreshold(double detectionThreshold) {
		this.detectionThreshold = detectionThreshold;
	}

	public double getTrackingThreshold() {
		return trackingThreshold;
	}

	public void setTrackingThreshold(double trackingThreshold) {
		this.trackingThreshold = trackingThreshold;
	}

	public double getMaxTrackingDistance() {
		return maxTrackingDistance;
	}

	public void setMaxTrackingDistance(double maxTrackingDistance) {
		this.maxTrackingDistance = maxTrackingDistance;
	}

	@Override
	public int getDetectionClassifier() {
		return 0;
	}

	@Override
	public boolean includeResult(double score) {
		return score >= detectionThreshold;
	}

	@Override
	public boolean includeResult(int x, int y, int w, int h) {

		if (w < 10 || h < 10) {
			return false;
		} else if (w > 450 || h > 450) {
			return false;
		}

		/*
		if (y > SCALED_HEIGHT / 2 && h < SCALED_HEIGHT / 10) {
			System.err.println("ignoring, h = " + h);
			return false;
		} else if (h < 10) {
			return false;
		} else if (h > 75) {
			return false;
		}
		*/

		return true;

	}

	@Override
	public DetectionResult addResult(List<DetectionResult> results, int x, int y, int w, int h, double score) {

		DetectionResult result = new DetectionResult(x, y, w, h, score);
		result.grow(10, 10);

		boolean add = true;

		for (DetectionResult dr : results) {
			if (mergeDetectionResults(dr, result)) {
				add = false;
				break;
			}
		}

		if (add) {
			results.add(result);
		}

		return result;

	}

	protected boolean mergeDetectionResults(DetectionResult result1, DetectionResult result2) {

		if (result1.intersects(result2)) {

			result1.union(result2);

			// Accumulate score
			// result1.score += result2.score;

			// Choose highest score
			result1.score = Math.max(result1.score, result2.score);

			return true;

		} else {
			return false;
		}

	}

	@Override
	protected boolean includeObject(DetectionResult object) {
		return object.score >= trackingThreshold;
	}
	
	@Override
	protected DetectionResult createCopy(DetectionResult object) {
		return object.createCopy();
	}

	@Override
	protected double compareObjects(DetectionResult object1, DetectionResult object2) {
		double distance = object1.distance(object2);
		if (distance <= maxTrackingDistance) {
			return 1.0 - (distance / maxTrackingDistance);
		} else {
			return 0.0;
		}
	}

	@Override
	protected void objectUpdated(DetectionResult updatedObject, DetectionResult object) {
		// updatedObject.score = trackingThreshold;
		updatedObject.score = Math.max(object.score, updatedObject.score);
		super.objectUpdated(updatedObject, object);
	}

}
