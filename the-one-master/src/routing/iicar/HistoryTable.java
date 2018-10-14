package routing.iicar;


import java.util.ArrayList;

import core.Coord;
import core.DTNHost;

public class HistoryTable {
	private int hostId;
	private double continuity;
	private Coord homeLocation;
	private Coord officeLocation;
	private ArrayList<MeetList> meetlist;
	
	
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
	public void updateContinuity() {
		this.continuity++;
	}
	public int getHostNode() {
		return hostId;
	}
	public void setHostNode(int hostNode) {
		this.hostId = hostNode;
	}
	
	public void add (MeetList m){
		this.meetlist.add(m);
	}
	
	public void setlist(ArrayList<MeetList> m){
		this.meetlist = m;
	}
	public ArrayList<MeetList> get(){
		return meetlist;
	}
}
