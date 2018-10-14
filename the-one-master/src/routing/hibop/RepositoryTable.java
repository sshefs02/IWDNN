package routing.hibop;

import java.util.ArrayList;

import core.Coord;

public class RepositoryTable {

	/** the time when this MPS was last updated */
	// private double aggregate;
	private Coord aggregate;
	/** the alpha parameter */
	private int classtype;
	private ArrayList<Integer> carrier;
	private int hetcount, redcount, concount;

	/**
	 * Constructor. for RepositoryTable class
	 */
	public RepositoryTable() {

		this.hetcount = 0;
		this.redcount = 0;
		this.concount = 0;
		this.aggregate = new Coord(0, 0);
		this.carrier = new ArrayList<Integer>();
		this.classtype = 0;
	}

	public void setAggregate(Coord value) {
		this.aggregate = value;
	}

	public void setclasstype(int value) {
		this.classtype = value;
	}

	public void setcarrier(int value) {
		this.carrier.add(value);
	}

	public void setHetcount(int value) {
		this.hetcount = value;
	}

	public void setRedcount(int value) {
		this.redcount = value;
	}

	public void setConcount(int value) {
		this.concount = value;
	}

	public Coord getAggregate() {
		return this.aggregate;
	}

	public int getHetcount() {
		return this.hetcount;
	}

	public int getRedcount() {
		return this.redcount;
	}

	public int getConcount() {
		return this.concount;
	}

	public ArrayList<Integer> getcarrier() {
		return this.carrier;
	}

	public int getclasstype() {
		return this.classtype;
	}

}