package routing.iicar;

import core.Coord;
import core.DTNHost;

public class MeetList {
	private int hostId;
	private double continuity;
	private Coord homeLocation;
	private Coord officeLocation;
	
	public Coord getHomeLocation() {
		return homeLocation;
	}
	public void setHomeLocation(Coord homeLocation) {
		this.homeLocation = homeLocation;
	}
	public Coord getOfficeLocation() {
		return officeLocation;
	}
	public void setOfficeLocation(Coord officeLocation) {
		this.officeLocation = officeLocation;
	}
	public double getContinuity() {
		return continuity;
	}
	public void setContinuity(double continuity) {
		this.continuity = continuity;
	}
	public int getHostNode() {
		return hostId;
	}
	public void setHostNode(int hostNode) {
		this.hostId = hostNode;
	}
	
	
	
}
