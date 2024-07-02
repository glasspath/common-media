/*
 * This file is part of Glasspath Common.
 * Copyright (C) 2011 - 2024 Remco Poelstra
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
package org.glasspath.common.media.opencv;

import java.util.ArrayList;
import java.util.List;

public abstract class ObjectTracker<T extends TrackableObject> {

	public static final int DEFAULT_RETAIN = 30;

	private int retain = DEFAULT_RETAIN;

	private final List<T> activeObjects = new ArrayList<>();
	private short updateId = 0;

	private final List<Listener<T>> listeners = new ArrayList<>();

	public ObjectTracker() {

	}

	public int getRetain() {
		return retain;
	}

	public void setRetain(int retain) {
		this.retain = retain;
	}

	protected List<T> processObjects(List<T> objects) {

		List<T> trackedObjects = new ArrayList<>();

		updateId++;

		for (T object : objects) {

			T bestMatch = null;
			double bestMatchScore = 0.0;

			for (T activeObject : activeObjects) {

				if (activeObject.lastUpdateId != updateId) {

					double matchScore = compareObjects(object, activeObject);
					if (matchScore > bestMatchScore) {

						bestMatch = activeObject;
						bestMatchScore = matchScore;

						if (matchScore >= 1.0) {
							break;
						}

					}

				}

			}

			// Combine with active object if possible, reset retain count when combined
			if (bestMatch != null) {

				updateObject(bestMatch, object);

				bestMatch.lastUpdateId = updateId;
				bestMatch.updateCount++;
				bestMatch.retainCount = 0;

				objectUpdated(bestMatch, object);

				trackedObjects.add(createCopy(bestMatch));

			} else if (includeObject(object)) {

				activeObjects.add(object);
				objectAdded(object);

				trackedObjects.add(createCopy(object));

			}

		}

		// Remove object after delay
		for (int i = activeObjects.size() - 1; i >= 0; i--) {

			T object = activeObjects.get(i);

			if (object.lastUpdateId != updateId) {

				object.retainCount++;

				if (object.retainCount > retain || (object.retainCount >= 5 && object.updateCount < 1)) { // TODO
					activeObjects.remove(i);
					objectRemoved(object);
				} else {
					trackedObjects.add(createCopy(object));
				}

			}

		}

		// TODO
		/*
		List<T> removeObjects = new ArrayList<>();
		for (T object : updatedObjects) {
			for (T removeObject : updatedObjects) {
				if (object != removeObject && object.add(removeObject)) {
					System.out.println("merged");
					removeObjects.add(removeObject);
				}
			}
		}
		updatedObjects.removeAll(removeObjects);
		*/

		return trackedObjects;

	}

	protected abstract boolean includeObject(T object);

	protected abstract T createCopy(T object);

	protected double compareObjects(T object1, T object2) {
		if (object1.intersects(object2)) {
			return 1.0;
		} else {
			return 0.0;
		}
	}

	protected void updateObject(T object1, T object2) {
		object1.x = object2.x;
		object1.y = object2.y;
		object1.width = object2.width;
		object1.height = object2.height;
	}

	protected void objectAdded(T object) {
		for (Listener<T> listener : listeners) {
			listener.objectAdded(object);
		}
	}

	protected void objectUpdated(T updatedObject, T object) {
		for (Listener<T> listener : listeners) {
			listener.objectUpdated(updatedObject, object);
		}
	}

	protected void objectRemoved(T object) {
		for (Listener<T> listener : listeners) {
			listener.objectRemoved(object);
		}
	}

	public void addListener(Listener<T> listener) {
		listeners.add(listener);
	}

	public void removeListener(Listener<T> listener) {
		listeners.remove(listener);
	}

	public static interface Listener<T> {

		public void objectAdded(T object);

		public void objectUpdated(T updatedObject, T object);

		public void objectRemoved(T object);

	}

}
