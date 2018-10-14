package routing;

import core.*;
import report.MLReport;
import util.Tuple;
import weka.core.Instance;

import java.util.*;

public class IWDMLRouter extends ActiveRouter{
    /** delivery predictability initialization constant*/
	private static final double P_INIT = 0.75;
	/** delivery predictability transitivity scaling constant default value */
	private static final double DEFAULT_BETA = 0.25;
	/** delivery predictability aging constant */
	private static final double GAMMA = 0.98;

  private static final String THRESHOLD = "threshold";

  private static double threshold;
	/** Prophet router's setting namespace ({@value})*/
	private static final String PROPHET_NS = "IWDMLRouter";
	/**
	 * Number of seconds in time unit -setting id ({@value}).
	 * How many seconds one time unit is when calculating aging of
	 * delivery predictions. Should be tweaked for the scenario.*/
	private static final String SECONDS_IN_UNIT_S ="secondsInTimeUnit";

	/**
	 * Transitivity scaling constant (beta) -setting id ({@value}).
	 * Default value for setting is {@link #DEFAULT_BETA}.
	 */
	public static final String BETA_S = "beta";

	/** the value of nrof seconds in time unit -setting */
	private int secondsInTimeUnit;
	/** value of beta setting */
	private double beta;

	/** delivery predictabilities */
	public Map<DTNHost, Double> preds;
	/** last delivery predictability update (sim)time */
	private double lastAgeUpdate;

	/** IDs of the messages that are known to have reached the final dst */
	private Set<String> ackedMessageIds;

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public IWDMLRouter(Settings s) {
		super(s);
		Settings prophetSettings = new Settings(PROPHET_NS);
		secondsInTimeUnit = prophetSettings.getInt(SECONDS_IN_UNIT_S);
		if (prophetSettings.contains(BETA_S)) {
			beta = prophetSettings.getDouble(BETA_S);
		}
		else {
			beta = DEFAULT_BETA;
		}

    threshold = prophetSettings.getDouble(THRESHOLD);
		initPreds();
	}

	/**
	 * Copyconstructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected IWDMLRouter(IWDMLRouter r) {
		super(r);
		this.secondsInTimeUnit = r.secondsInTimeUnit;
		this.beta = r.beta;
    this.threshold = r.threshold;
		this.ackedMessageIds = new HashSet<String>();
		initPreds();
	}

	/**
	 * Initializes predictability hash
	 */
	private void initPreds() {
		this.preds = new HashMap<DTNHost, Double>();
	}

	@Override
	public void changedConnection(Connection con) {
		super.changedConnection(con);

		if (con.isUp()) {
			DTNHost otherHost = con.getOtherNode(getHost());
			IWDMLRouter otherRouter = (IWDMLRouter) otherHost.getRouter();
			updateDeliveryPredFor(otherHost);
			updateTransitivePreds(otherHost);

			/* exchange ACKed message data */
			this.ackedMessageIds.addAll(otherRouter.ackedMessageIds);
			otherRouter.ackedMessageIds.addAll(this.ackedMessageIds);
			deleteAckedMessages();
			otherRouter.deleteAckedMessages();
		}
	}

	/**
	 * Deletes the messages for which ACKs have been received
	 */
	private void deleteAckedMessages() {
		for (String id : this.ackedMessageIds) {
			if (this.hasMessage(id) && !isSending(id)) {
				this.deleteMessage(id, false);
			}
		}
	}

	/**
	 * Updates delivery predictions for a host.
	 * <CODE>P(a,b) = P(a,b)_old + (1 - P(a,b)_old) * P_INIT</CODE>
	 * @param host The host we just met
	 */
	private void updateDeliveryPredFor(DTNHost host) {
		double oldValue = getPredFor(host);
		double newValue = oldValue + (1 - oldValue) * P_INIT;
		preds.put(host, newValue);
	}

	/**
	 * Returns the current prediction (P) value for a host or 0 if entry for
	 * the host doesn't exist.
	 * @param host The host to look the P for
	 * @return the current P value
	 */
	public double getPredFor(DTNHost host) {
		ageDeliveryPreds(); // make sure preds are updated before getting
		if (preds.containsKey(host)) {
			return preds.get(host);
		}
		else {
			return 0;
		}
	}

	/**
	 * Updates transitive (A->B->C) delivery predictions.
	 * <CODE>P(a,c) = P(a,c)_old + (1 - P(a,c)_old) * P(a,b) * P(b,c) * BETA
	 * </CODE>
	 * @param host The B host who we just met
	 */
	private void updateTransitivePreds(DTNHost host) {
		MessageRouter otherRouter = host.getRouter();
		assert otherRouter instanceof IWDMLRouter : "PRoPHET only works " +
			" with other routers of same type";

		double pForHost = getPredFor(host); // P(a,b)
		Map<DTNHost, Double> othersPreds =
			((IWDMLRouter)otherRouter).getDeliveryPreds();

		for (Map.Entry<DTNHost, Double> e : othersPreds.entrySet()) {
			if (e.getKey() == getHost()) {
				continue; // don't add yourself
			}

			double pOld = getPredFor(e.getKey()); // P(a,c)_old
			double pNew = pOld + ( 1 - pOld) * pForHost * e.getValue() * beta;
			preds.put(e.getKey(), pNew);
		}
	}

	/**
	 * Ages all entries in the delivery predictions.
	 * <CODE>P(a,b) = P(a,b)_old * (GAMMA ^ k)</CODE>, where k is number of
	 * time units that have elapsed since the last time the metric was aged.
	 * @see #SECONDS_IN_UNIT_S
	 */
	private void ageDeliveryPreds() {
		double timeDiff = (SimClock.getTime() - this.lastAgeUpdate) /
			secondsInTimeUnit;

		if (timeDiff == 0) {
			return;
		}

		double mult = Math.pow(GAMMA, timeDiff);
		for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
			e.setValue(e.getValue()*mult);
		}

		this.lastAgeUpdate = SimClock.getTime();
	}

	/**
	 * Returns a map of this router's delivery predictions
	 * @return a map of this router's delivery predictions
	 */
	private Map<DTNHost, Double> getDeliveryPreds() {
		ageDeliveryPreds(); // make sure the aging is done
		return this.preds;
	}

	@Override
	public void update() {
		super.update();
		if (!canStartTransfer() ||isTransferring()) {
			return; // nothing to transfer or is currently transferring
		}

		// try messages that could be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return;
		}

		tryOtherMessages();
	}

	private void tryOtherMessages() {
	    List<Tuple<Message, Connection>> messagesToSend = new ArrayList<>();
        Collection<Message> messages = getMessageCollection();

        for (Connection con : getConnections()) {
            DTNHost other= con.getOtherNode(getHost());
            IWDMLRouter otherRouter = (IWDMLRouter) other.getRouter();

            if (otherRouter.isTransferring())
                continue;

            for (Message message : messages) {
                Instance instanceVector = MLReport.get_decision_vector(getHost(), other, message);
                double deliveryProbability = MLTrainer.get_delivery_prob(instanceVector);
                if (deliveryProbability > threshold)
                    messagesToSend.add(new Tuple<>(message,con));
            }
        }
        Collections.sort(messagesToSend, new TupleComparator());
        tryMessagesForConnected(messagesToSend);
	}

	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message m =  super.messageTransferred(id, from);
		if (isDeliveredMessage(m)) {
			this.ackedMessageIds.add(id);
		}
		return m;
	}

	@Override
	protected void transferDone(Connection con) {
		super.transferDone(con);
		Message m = con.getMessage();
		DTNHost recipient = con.getOtherNode(getHost());

		/* was the message delivered to the final recipient? */
		if (m.getTo() == recipient) {
			this.ackedMessageIds.add(m.getId()); // yes, add to ACKed messages
			this.deleteMessage(m.getId(), false); // delete from buffer
		}
	}

	/**
	 * Comparator for Message-Connection-Tuples that orders the tuples by
	 * their delivery probability by the host on the other side of the
	 * connection (GRTRMax)
	 */
	private class TupleComparator implements Comparator<Tuple<Message, Connection>> {

		public int compare(Tuple<Message, Connection> tuple1,
				Tuple<Message, Connection> tuple2) {
			// delivery probability of tuple1's message with tuple1's connection
			double p1 = ((IWDMLRouter)tuple1.getValue().
					getOtherNode(getHost()).getRouter()).getPredFor(
					tuple1.getKey().getTo());
			// -"- tuple2...
			double p2 = ((IWDMLRouter)tuple2.getValue().
					getOtherNode(getHost()).getRouter()).getPredFor(
					tuple2.getKey().getTo());

			// bigger probability should come first
			if (p2-p1 == 0) {
				/* equal probabilities -> let queue mode decide */
				return compareByQueueMode(tuple1.getKey(), tuple2.getKey());
			}
			else if (p2-p1 < 0) {
				return -1;
			}
			else {
				return 1;
			}
		}
	}

	@Override
    public MessageRouter replicate() {
	    IWDMLRouter r = new IWDMLRouter(this);
	    return r;
    }

}
