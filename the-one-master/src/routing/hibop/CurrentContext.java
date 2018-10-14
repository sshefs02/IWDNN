package routing.hibop;

import java.util.HashMap;
import java.util.Map;

/**
 * Class for storing and manipulating the meeting probabilities for the MaxProp
 * router module.
 */
public class CurrentContext {
	public static final int INFINITE_SET_SIZE = Integer.MAX_VALUE;
	/** meeting probabilities (probability that the next node one meets is X) */
	private Map<Integer, IdentityTable> itset;
	/** the time when this MPS was last updated */
	private int maxSetSize;

	/**
	 * Constructor. Creates a probability set with empty node-probability
	 * mapping.
	 * 
	 * @param maxSetSize
	 *            Maximum size of the probability set; when the set is full,
	 *            smallest values are dropped when new are added
	 */
	public CurrentContext(int maxSetSize) {

		this.itset = new HashMap<Integer, IdentityTable>();
		if (maxSetSize == INFINITE_SET_SIZE || maxSetSize < 1) {
			this.itset = new HashMap<Integer, IdentityTable>();
			this.maxSetSize = INFINITE_SET_SIZE;
		} else {
			this.itset = new HashMap<Integer, IdentityTable>(maxSetSize);
			this.maxSetSize = maxSetSize;
		}

	}

	/**
	 * Constructor. Creates a probability set with empty node-probability
	 * mapping and infinite set size
	 */
	public CurrentContext() {
		this(INFINITE_SET_SIZE);
	}

	public boolean isITpresentinCC(int nid) {
		return (itset.containsKey(nid));

	}

	public void addITtoCC(Integer nid, IdentityTable it) {
		Map.Entry<Integer, IdentityTable> smallestEntry = null;
		double smallestValue = Double.MAX_VALUE;
		it.setupdatetime();
		itset.put(nid, it);
		for (Map.Entry<Integer, IdentityTable> entry : itset.entrySet()) {

			if (entry.getValue().getupdatetime() < smallestValue) {
				smallestEntry = entry;
				smallestValue = entry.getValue().getupdatetime();
			}

		}

		if (itset.size() >= maxSetSize) {
			core.Debug.p("CCsize: " + itset.size() + " dropping "
					+ itset.remove(smallestEntry.getKey()));
		}
	}

	public IdentityTable getITfromCC(Integer nid) {
		if (itset.containsKey(nid)) {
			return itset.get(nid);
		} else {
			/* the node with the given index has not been met */
			return null;
		}
	}

	public Map<Integer, IdentityTable> getAllITs() {
		return this.itset;
	}

	/**
	 * Returns a String presentation of the probabilities
	 * 
	 * @return a String presentation of the probabilities
	 */
	@Override
	public String toString() {
		return "its: " + this.itset.toString();
	}
}