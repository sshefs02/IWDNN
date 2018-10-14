package routing.newrouter;

public class NodeInformation {
	
	/*neighbor node*/
	private double NeighbourBuffer;
	private double NeighbourSpeed;
	private double NeighbourDisDest;
	
	/*host node*/
	private double HostTTL;
	private double HostHopCount;
	private double HostdelProb;
	
	private double decision;

	public double getNeighbourBuffer() {
		return NeighbourBuffer;
	}

	public void setNeighbourBuffer(double neighbourBuffer) {
		NeighbourBuffer = neighbourBuffer;
	}

	public double getNeighbourSpeed() {
		return NeighbourSpeed;
	}

	public void setNeighbourSpeed(double neighbourSpeed) {
		NeighbourSpeed = neighbourSpeed;
	}

	public double getNeighbourDisDest() {
		return NeighbourDisDest;
	}

	public void setNeighbourDisDest(double neighbourDisDest) {
		NeighbourDisDest = neighbourDisDest;
	}

	public double getHostTTL() {
		return HostTTL;
	}

	public void setHostTTL(double hostTTL) {
		HostTTL = hostTTL;
	}

	public double getHostHopCount() {
		return HostHopCount;
	}

	public void setHostHopCount(double hostHopCount) {
		HostHopCount = hostHopCount;
	}

	public double getHostdelProb() {
		return HostdelProb;
	}

	public void setHostdelProb(double hostdelProb) {
		HostdelProb = hostdelProb;
	}

	public double getDecision() {
		return decision;
	}

	public void setDecision(double decision) {
		this.decision = decision;
	}
	
	
	
	
	
	
	
}
