package edu.uco.jstone25.bzzt;

import android.util.Log;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.geometry.Point;
import com.google.maps.android.quadtree.PointQuadTree;

public class AccelerationPoint implements PointQuadTree.Item, Comparable<AccelerationPoint> {
        
    private float accel;
    private Point p;
    private LatLng l;
    private Circle c;
    private int series_id;
    private int sequence_number;
        
    public AccelerationPoint(LatLng ll, float newAccelValue, int series_id, int sequence_number) {
    	setLatLng(ll);
        setPoint(ll.latitude, ll.longitude);
        setSeries(series_id);
        setSequence(sequence_number);
        setAccel(newAccelValue);
    }
        
    public AccelerationPoint(String line) {
    	// format is:
    	// 10,20,35.44281422,-97.59749993,0.0593254169305
    	// series id, sequence number, lat, long, z-score
    	String[] parts = line.split(",");
    	setPoint(Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
    	setLatLng(new LatLng(getPoint().x, getPoint().y));
    	setAccel(Float.parseFloat(parts[4]));
    	setSeries(Integer.parseInt(parts[0]));
	 	setSequence(Integer.parseInt(parts[1]));
    }

    @Override
    public Point getPoint() {
    	return p;
    }

    public void setPoint(double x, double y) {
    	p = new Point(x, y);
    }

    public float getAccel() {
            return accel;
    }

    public void setAccel(float accel) {
        this.accel = accel;
    }

    public Circle getCircle() {
        return c;
    }

    public void setCircle(Circle c) {
        this.c = c;
    }
        
    public void removeCircle() {
        c.remove();
        c = null;
    }

    public int getSeries() {
        return series_id;
    }

    public void setSeries(int series_id) {
        this.series_id = series_id;
    }

    public int getSequence() {
    	return sequence_number;
    }

    public void setSequence(int sequence_number) {
        this.sequence_number = sequence_number;
    }

	@Override
	public int compareTo(AccelerationPoint another) {
		// sort according to sequence order
		return this.getSequence() - another.getSequence();
	}

	public LatLng getLatLng() {
		return l;
	}

	public void setLatLng(LatLng l) {
		this.l = l;
	}
}
