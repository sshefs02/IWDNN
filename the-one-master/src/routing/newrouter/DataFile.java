package routing.newrouter;

import core.DTNHost;

public class DataFile {
	private DTNHost selected;
	private double probability;
	private boolean flag;
	
	public DTNHost getSelected() {
		return selected;
	}
	public void setSelected(DTNHost selected) {
		this.selected = selected;
	}
	public double getProbability() {
		return probability;
	}
	public void setProbability(double probability) {
		this.probability = probability;
	}
	public boolean isFlag() {
		return flag;
	}
	public void setFlag(boolean flag) {
		this.flag = flag;
	}
	
	
	
}
