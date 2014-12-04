package edu.uco.jstone25.bzzt;

import java.util.HashMap;
import java.util.TreeMap;

import com.google.maps.android.quadtree.PointQuadTree;

public interface QueryPointsListener<T extends PointQuadTree.Item> {
	void setPointTree(PointQuadTree<T> p);
	void setSeriesMap(HashMap<Integer, TreeMap<Integer, T>> h);
}
