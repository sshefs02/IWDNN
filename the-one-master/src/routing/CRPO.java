package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.Connection;
import core.Coord;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import util.Tuple;

public class CRPO extends ActiveRouter {
	
	public static final double alpha = 0.25;
	
	public double input_hidden[][] = new double[][] {{0.02,0.02},{0.02,0.02},{0.02,0.02},{0.02,0.02}};
	public double hidden_output[] = new double[] {0.02,0.02};
	public double bias_hidden[] = new double[] {0.02,0.02};
	public double bias_output = 0.02;
	
	/**
	 * Prophet settings
	 * 
	 */
	/** delivery predictability initialization constant*/
	public static final double P_INIT = 0.75;
	/** delivery predictability transitivity scaling constant default value */
	public static final double DEFAULT_BETA = 0.25;
	/** delivery predictability aging constant */
	public static final double GAMMA = 0.98;
	
	/** Prophet router's setting namespace ({@value})*/ 
	public static final String NEWROUTER_NS = "ProphetRouter";
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

	
	protected CRPO(CRPO r) {
		super(r);
		this.secondsInTimeUnit = r.secondsInTimeUnit;
		this.beta = r.beta;
		initPreds();
		
	}
	
	public CRPO(Settings s) {
		super(s);
		// TODO Auto-generated constructor stub
				Settings prophetSettings = new Settings(NEWROUTER_NS);
				secondsInTimeUnit = prophetSettings.getInt(SECONDS_IN_UNIT_S);
				if (prophetSettings.contains(BETA_S)) {
					beta = prophetSettings.getDouble(BETA_S);
				}
				else {
					beta = DEFAULT_BETA;
				}

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
		assert otherRouter instanceof ProphetRouter : "PRoPHET only works " + 
			" with other routers of same type";
		
		double pForHost = getPredFor(host); // P(a,b)
		Map<DTNHost, Double> othersPreds = 
			((CRPO)otherRouter).getDeliveryPreds();
		
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

	
	public void trainNeuralNet(double p, double buf,
			double avg_sp, double dist, int dec){
		int decision = dec;
		double pred = p, buffer = buf, avg_speed = avg_sp, dist_from_dest = dist;
		double input[] = new double[4];
		input[0] = pred;
		input[1] = buffer;
		input[2] = avg_speed;
		input[3] = dist_from_dest;
		double hidd1, hidd2;
		
		hidd1 = pred*input_hidden[0][0]+buffer*input_hidden[1][0]
					+avg_speed*input_hidden[2][0]
							+dist_from_dest*input_hidden[3][0]
									+bias_hidden[0];
			
			
		hidd2 = pred*input_hidden[0][1]+buffer*input_hidden[1][1]
					+avg_speed*input_hidden[2][1]
							+dist_from_dest*input_hidden[3][1]
									+bias_hidden[1];
		
		double act1 = activation(hidd1);
		double act2 = activation(hidd2);
		
		double Yin = act1*hidden_output[0] + act2*hidden_output[0] + bias_output;
		
		double out = activation(Yin);
		
		if(out == decision){
			return;
		}else{
			 
			/**
			 * error portion deltak
			 */
			double deltak = calculateError(out,decision)*differentiation(Yin);
			/**
			 * calculate deltaij error portion between input and hidden layer
			 */
			 double delta_neuron1 = deltak*hidden_output[0];
			 double delta_neuron2 = deltak*hidden_output[0];
			/**
			 * weights and bias update between hidden layer and output layer
			 */
			hidden_output[0] = hidden_output[0]+alpha*deltak*hidd1;
			hidden_output[1] = hidden_output[1]+alpha*deltak*hidd2;
			bias_output = bias_output+alpha*deltak;
			
			/**
			 * calculate error portion of input and hidden layers
			 */
			double delta_1 = delta_neuron1*differentiation(hidd1);
			double delta_2 = delta_neuron2*differentiation(hidd2);
			
			/**
			 * weight updates between input and hidden layer
			 */
			for(int i=0;i<4;i++){
			
				input_hidden[i][0] = input_hidden[i][0] + alpha*delta_1*input[i];
				input_hidden[i][1] = input_hidden[i][1] + alpha*delta_2*input[i];
			}
			/**
			 * bias update
			 */
			bias_hidden[0] = bias_hidden[0]  + alpha*delta_1;
			bias_hidden[0] = bias_hidden[0]  + alpha*delta_2;
		}
		
	}
	
	public double activation(double num){
		//System.out.println(num);
		double f = 1/(1+Math.pow(Math.E, -0.5*(num)));
		//System.out.println(f);
		return f;
	}

	public double differentiation(double num){
		double f = 1/(1+Math.pow(Math.E, -0.5*(num)));
		return 0.5*f*(1-f);
	}
	
	public double calculateError(double target, double output){
		double error = 0.5*Math.pow(target-output, 2);
		return error;
	}
	
	public double decisionNeuralNet(double p, double buf,
			double avg_sp, double dist){
		double pred = p, buffer = buf, avg_speed = avg_sp, dist_from_dest = dist;

		
		double hidd1, hidd2;
		
		hidd1 = pred*input_hidden[0][0]+buffer*input_hidden[1][0]
					+avg_speed*input_hidden[2][0]+dist_from_dest*input_hidden[3][0];
			
			
		hidd2 = pred*input_hidden[0][1]+buffer*input_hidden[1][1]
					+avg_speed*input_hidden[2][1]+dist_from_dest*input_hidden[3][1];
		
		double act1 = activation(hidd1);
		double act2 = activation(hidd2);
		
		double Yin = act1*hidden_output[0] + act2*hidden_output[1] + bias_output;
		
		double out = activation(Yin);
		return out;
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
			CRPO othRouter = (CRPO)other.getRouter();
			
			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}
			
			for (Message m : msgCollection) {
				if (othRouter.hasMessage(m.getId())) {
					continue; // skip messages that the other one has
				}
				
				if(SimClock.getTime()<5000||
						(SimClock.getTime()>30000 && SimClock.getTime()<35000)||
						(SimClock.getTime()>60000 && SimClock.getTime()<65000)||
						(SimClock.getTime()>90000 && SimClock.getTime()<95000)){
					
					double buffer = (othRouter.getFreeBufferSize()/othRouter.getBufferSize())*5;
					double avg_speed = other.getAverageSpeed()-other.getSpeed();
					double distance = getweight(other.getLocation(), m.getTo().getLocation());
					double pred = othRouter.getPredFor(m.getTo())*10;
					
					if(othRouter.getPredFor(m.getTo()) > getPredFor(m.getTo())){
						trainNeuralNet(pred, buffer, avg_speed, distance,1);
						messages.add(new Tuple<Message, Connection>(m,con));
						
					}else{
						trainNeuralNet(pred, buffer, avg_speed, distance,0);
					}
				}else{
						double buffer = (othRouter.getFreeBufferSize()/othRouter.getBufferSize())*5;
						double avg_speed = other.getAverageSpeed()-other.getSpeed();
						double distance = getweight(other.getLocation(), m.getTo().getLocation());
						double pred = othRouter.getPredFor(m.getTo())*10;
						
						if (decisionNeuralNet(pred, buffer, avg_speed, distance)>=0.9999) {
							// the other node has higher probability of delivery
							//System.out.println("hello aman");
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
	public MessageRouter replicate() {
		CRPO r = new CRPO(this);
		return r;
	}

}