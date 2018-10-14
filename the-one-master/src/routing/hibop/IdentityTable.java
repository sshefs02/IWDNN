package routing.hibop;

import core.Coord;
import core.SimClock;

public class IdentityTable {

	/** the time when this Table was last updated */
	private double lastUpdateTime;
	/** the alpha parameter */
	private Coord home_loc;
	private Coord office_loc;

	public IdentityTable(Coord home, Coord office) {
		this.home_loc = home;
		this.office_loc = office;

		this.lastUpdateTime = 0;
	}

	public IdentityTable() {

	}

	public void updatevalues(Coord work, Coord home) {

		this.lastUpdateTime = SimClock.getTime();
		this.home_loc = home;
		this.office_loc = work;

	}

	public void setupdatetime() {
		this.lastUpdateTime = SimClock.getTime();
	}

	public Coord gethomeloc() {
		return this.home_loc;
	}

	public Coord getworkloc() {
		return this.office_loc;
	}

	public double getupdatetime() {
		return this.lastUpdateTime;
	}

}