package routing.newrouter;

import core.Coord;

public class DestinationData {
	private Coord HomeLocation;
	private Coord WorkLocation;
	
	
	public Coord getHomeLocation() {
		return HomeLocation;
	}
	public void setHomeLocation(Coord homeLocation) {
		this.HomeLocation = homeLocation;
	}
	public Coord getWorkLocation() {
		return WorkLocation;
	}
	public void setWorkLocation(Coord workLocation) {
		this.WorkLocation = workLocation;
	}
	
}
