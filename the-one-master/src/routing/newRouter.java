/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import routing.newrouter.NodeInformation;
import routing.util.RoutingInfo;

import util.Tuple;

import core.Connection;
import core.Coord;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;

/**
 * Implementation of PRoPHET router as described in 
 * <I>Probabilistic routing in intermittently connected networks</I> by
 * Anders Lindgren et al.
 */
public class newRouter extends ActiveRouter {
	/** delivery predictability initialization constant*/
	public static final double P_INIT = 0.75;
	/** delivery predictability transitivity scaling constant default value */
	public static final double DEFAULT_BETA = 0.25;
	/** delivery predictability aging constant */
	public static final double GAMMA = 0.98;
	
	/** Prophet router's setting namespace ({@value})*/ 
	public static final String PROPHET_NS = "newRouter";
	/**
	 * Number of seconds in time unit -setting id ({@value}).
	 * How many seconds one time unit is when calculating aging of 
	 * delivery predictions. Should be tweaked for the scenario.*/
	public static final String SECONDS_IN_UNIT_S ="secondsInTimeUnit";
	
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
	private Map<DTNHost, Double> preds;
	/** last delivery predictability update (sim)time */
	private double lastAgeUpdate;
	
	private boolean flag;
	
	private ArrayList<NodeInformation> nodeProp;
	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public newRouter(Settings s) {
		super(s);
		Settings prophetSettings = new Settings(PROPHET_NS);
		secondsInTimeUnit = prophetSettings.getInt(SECONDS_IN_UNIT_S);
		if (prophetSettings.contains(BETA_S)) {
			beta = prophetSettings.getDouble(BETA_S);
		}
		else {
			beta = DEFAULT_BETA;
		}

		initPreds();
		initNodeProp();
	}

	/**
	 * Copyconstructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected newRouter(newRouter r) {
		super(r);
		this.secondsInTimeUnit = r.secondsInTimeUnit;
		this.beta = r.beta;
		initPreds();
		initNodeProp();
	}
	
	/**
	 * Initializes predictability hash
	 */
	private void initPreds() {
		this.preds = new HashMap<DTNHost, Double>();
	}
	
	private void initNodeProp(){
		this.nodeProp = new ArrayList<NodeInformation>(500);
		this.flag = true;
	}

	@Override
	public void changedConnection(Connection con) {
		super.changedConnection(con);
		
		if (con.isUp()) {
			DTNHost otherHost = con.getOtherNode(getHost());
			updateDeliveryPredFor(otherHost);
			updateTransitivePreds(otherHost);
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
		assert otherRouter instanceof newRouter : "PRoPHET only works " + 
			" with other routers of same type";
		
		double pForHost = getPredFor(host); // P(a,b)
		Map<DTNHost, Double> othersPreds = 
			((newRouter)otherRouter).getDeliveryPreds();
		
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
	
	
	public void refreshtable(){
		this.nodeProp.clear();
		System.out.println("table is refreshed"+ this.nodeProp.size());
	}
	
	public double decisionThroughKNN(NodeInformation node){
		int s = nodeProp.size();
		HashMap<Double,Double> decision = new HashMap<Double, Double>(); 
		
		for (int i = s-1;i>Math.max(0,s-200);i--){
			NodeInformation nodeI = nodeProp.get(i);
			
			// distance function
			decision.put(calculateDistance(nodeI,node), nodeI.getDecision());
			
		}
		
		/**
		 * define K for KNN
		 *K
		 *K
		 *K
		 *K
		 *K
		 *K
		 *K
		 *K
		 */
		
		int k=50;
		int count=0;
		/**
		 * tree map is used to sort the Decision. treemap keep
		 * the element in order of their key values
		 * order is increasing
		 */
		Map<Double, Double>d = new TreeMap<Double, Double>(decision);
		double ans=0;
		for(Map.Entry<Double, Double> entry: d.entrySet()){
			count++;
			if(count>k){
				break;
			}
			ans = ans + (double)entry.getValue();
		}
		return ans/k;
	}
	
	
	/*public double calculateDistance(NodeInformation n){
		
		double Distance=0;
		Distance = (n.getHostTTL() * n.getNeighbourBuffer() * n.getHostdelProb())/(n.getNeighbourSpeed() * n.getHostHopCount() * n.getNeighbourDisDest());
		
		return Distance;
	}*/
	
	/**
	 * 
	 * @param n - Node parameter in the data set
	 * @param c - Current node's parameter
	 * @return
	 */
	public double calculateDistance(NodeInformation n, NodeInformation c){
		 double Distance=0;
		 /**if current buffer is more than the value 
		  * is less hence distance would be less
		 */
		 double buffer_better = n.getNeighbourBuffer()-c.getNeighbourBuffer();
		 /**
		  * ttlbetter positive means current ttl is more which is not desirable
		  * therefore it will increase the distance
		  */
		 double ttlbetter = c.getHostTTL() - n.getHostTTL();
		 /**
		  * nspeed buffer positive means current speed is more which 
		  * will increase the distance i.e. make it non favourable
		  *
		  */
		 double nSpeed_better = c.getNeighbourSpeed() - n.getNeighbourSpeed();
		 /**
		  * hop_better is positive means more hop is there for current
		  * this means we can't accommodate few hops
		  * this is not desired
		  */
		 double hop_better = c.getHostHopCount() - n.getHostHopCount();
		 /**
		  * dist_better posiitve means current ditance is more
		  * 
		  * not desired
		  */
		 double dist_better = c.getNeighbourDisDest() - n.getNeighbourDisDest();
		 /**
		  * prob_better positive means current has less probability
		  * not desired
		  */
		 double prob_better = n.getHostdelProb() - c.getHostdelProb();
		 
		 Distance = buffer_better + ttlbetter +
				 nSpeed_better + hop_better + 
				 dist_better + prob_better;
		
		 return Distance;
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
	
	/**
	 * Tries to send all other messages to all connected hosts ordered by
	 * their delivery probability
	 * @return The return value of {@link #tryMessagesForConnected(List)}
	 */
	private Tuple<Message, Connection> tryOtherMessages() {
		List<Tuple<Message, Connection>> messages = 
			new ArrayList<Tuple<Message, Connection>>(); 
	
		Collection<Message> msgCollection = getMessageCollection();
		
		/* for all connected hosts collect all messages that have a higher
		   probability of delivery by the other host */
		for (Connection con : getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			newRouter othRouter = (newRouter)other.getRouter();
			
			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}
			
			for (Message m : msgCollection) {
				if (othRouter.hasMessage(m.getId())) {
					continue; // skip messages that the other one has
				}
				
				if(SimClock.getTime()<1000||(SimClock.getTime()>11000&&SimClock.getTime()<12000)||
						(SimClock.getTime()>22000&&SimClock.getTime()<23000)||
						(SimClock.getTime()>33000&&SimClock.getTime()<34000)){
					
					if(this.flag==false){
						refreshtable();
						this.flag = true;
					}
					/**
					 * Trainign through KNN
					 * Problem: KNN classifier works best in cases when 
					 * number of instances are small.
					 * But in case of OppNets number of instances can 
					 * be very large. hence classification is done only for 
					 * latest scenarios.
					 * This can be included in your future work.
					 */
					if (othRouter.getPredFor(m.getTo()) > getPredFor(m.getTo())) {
						// the other node has higher probability of delivery
						messages.add(new Tuple<Message, Connection>(m,con));
						
						NodeInformation nodeI = new NodeInformation();
						nodeI.setDecision(1);
						nodeI.setHostdelProb(getPredFor(m.getTo()));
						nodeI.setHostHopCount(m.getHopCount());
						
						double ttl = (SimClock.getTime()-m.getCreationTime())/(300*60);
						nodeI.setHostTTL(ttl);
						nodeI.setNeighbourBuffer(othRouter.getFreeBufferSize()/othRouter.getBufferSize());
						double avg_speed = other.getAverageSpeed()-other.getSpeed();
						double distance = getweight(other.getLocation(), m.getTo().getLocation());
						nodeI.setNeighbourDisDest(distance);
						nodeI.setNeighbourSpeed(avg_speed);
						nodeProp.add(nodeI);
					}
					else{
						NodeInformation nodeI = new NodeInformation();
						nodeI.setDecision(0);
						nodeI.setHostdelProb(getPredFor(m.getTo()));
						nodeI.setHostHopCount(m.getHopCount());
						
						double ttl = (SimClock.getTime()-m.getCreationTime())/(300*60);
						nodeI.setHostTTL(ttl);
						nodeI.setNeighbourBuffer(othRouter.getFreeBufferSize());
						double avg_speed = other.getAverageSpeed()-other.getSpeed();
						double distance = getweight(other.getLocation(), m.getTo().getLocation());
						nodeI.setNeighbourDisDest(distance);
						nodeI.setNeighbourSpeed(avg_speed);
						nodeProp.add(nodeI);
					}
				}
				else{
					
					this.flag = false;
					
					/**
					 * Setting parameter of current node configuration to find the distance
					 */
					NodeInformation nodeI = new NodeInformation();
					nodeI.setHostdelProb(getPredFor(m.getTo()));
					nodeI.setHostHopCount(m.getHopCount());
					
					double ttl = (SimClock.getTime()-m.getCreationTime())/(300*60);
					nodeI.setHostTTL(ttl);
					nodeI.setNeighbourBuffer(othRouter.getFreeBufferSize());
					double avg_speed = other.getAverageSpeed()-other.getSpeed();
					double distance = getweight(other.getLocation(), m.getTo().getLocation());
					nodeI.setNeighbourDisDest(distance);
					nodeI.setNeighbourSpeed(avg_speed);
					//decision through KNN
					
					double dec = decisionThroughKNN(nodeI);
					
					/* T
					 * H
					 * R  
					 *  E 
					 *   S
					 *   H
					 *   H
					 *   O
					 *   L
					 *   D
					 *   */
					if(dec>=1){
						messages.add(new Tuple<Message, Connection>(m,con));
						
					}
				}
			}			
		}
		
		if (messages.size() == 0) {
			return null;
		}
		
		return tryMessagesForConnected(messages);	// try to send messages
	}
	
	
	public double getweight(Coord node1, Coord node2) {
		double x = Math.pow(node1.getX() - node2.getX(), 2);
		double y = Math.pow(node1.getY() - node2.getY(), 2);
		double dsn = Math.sqrt(x + y);
		if (dsn == 0)
			return 10;
		else if (dsn <= 100)
			return 9;
		else if (dsn <= 200)
			return 8;
		else if (dsn <= 300)
			return 7;
		else if (dsn <= 400)
			return 6;
		else if (dsn <= 500)
			return 5;
		else if (dsn <= 600)
			return 4;
		else if (dsn <= 700)
			return 3;
		else if (dsn <= 800)
			return 2;
		else if (dsn <= 1000)
			return 1;
		else
			return 0;
	}
	
	@Override
	public RoutingInfo getRoutingInfo() {
		ageDeliveryPreds();
		RoutingInfo top = super.getRoutingInfo();
		RoutingInfo ri = new RoutingInfo(preds.size() + 
				" delivery prediction(s)");
		
		for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
			DTNHost host = e.getKey();
			Double value = e.getValue();
			
			ri.addMoreInfo(new RoutingInfo(String.format("%s : %.6f", 
					host, value)));
		}
		
		top.addMoreInfo(ri);
		return top;
	}
	
	@Override
	public MessageRouter replicate() {
		newRouter r = new newRouter(this);
		return r;
	}

}
