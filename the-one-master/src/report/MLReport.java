/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.*;
import routing.IWDMLRouter;
import routing.MLProph;
import weka.core.DenseInstance;
import weka.core.Instance;

import static core.SimScenario.GROUP_NS;
import static core.SimScenario.ROUTER_S;

/**
 * Report for generating different kind of total statistics about message
 * relaying performance. Messages that were created during the warm up period
 * are ignored.
 *
 * <strong>Note:</strong> if some statistics could not be created (e.g. overhead
 * ratio if no messages were delivered) "NaN" is reported for double values and
 * zero for integer median(s).
 */
public class MLReport extends Report implements MessageListener {

    private static Map<DTNHost, Map<DTNHost, Integer>> noOfSuccessfulContactsBetweenToNodeAndFromNode = new HashMap<DTNHost, Map<DTNHost, Integer>>();
    private static Map<DTNHost, Map<DTNHost, Ratio>> ratioOfSuccessfulTransfersToTotalTransferInitiated = new HashMap<DTNHost, Map<DTNHost, Ratio>>();
    private List<Detail> conn_details;
    private Map<DTNHost, Map<DTNHost, Integer>> noOfSuccessfulMsgDeliveriesBetweenToNodeAndFromNode = new HashMap<DTNHost, Map<DTNHost, Integer>>();
    private Map<DTNHost, Map<DTNHost, Ratio>> ratioOfSuccessfulMsgDeliveriesToTotalMsgCreated = new HashMap<DTNHost, Map<DTNHost, Ratio>>();
    public MLReport() {
        init();
    }

    public static Double get_success_ratio(DTNHost from, DTNHost to) {
        Double ratio;
        if (ratioOfSuccessfulTransfersToTotalTransferInitiated.get(to) == null) {
            ratio = 0.0;
        } else {
            if (ratioOfSuccessfulTransfersToTotalTransferInitiated.get(to).get(from) == null) {
                ratio = 0.0;
            } else {
                ratio = ratioOfSuccessfulTransfersToTotalTransferInitiated
                        .get(to)
                        .get(from).evaluateRatio();
            }
        }
        return ratio;
    }

    public static int get_contact_no(DTNHost from, DTNHost to) {
        int x;
        if (noOfSuccessfulContactsBetweenToNodeAndFromNode.get(to) == null) {
            x = 0;
        } else {
            if (noOfSuccessfulContactsBetweenToNodeAndFromNode.get(to).get(from) == null) {
                x = 0;
            } else {
                x = noOfSuccessfulContactsBetweenToNodeAndFromNode
                        .get(to)
                        .get(from);
            }
        }
        return x;
    }

    public static Instance get_decision_vector(DTNHost from, DTNHost to,
                                               Message m) {
        Double pred_val;
        if ((new Settings(GROUP_NS).getSetting(ROUTER_S)).equals("IWDMLRouter")) {
            pred_val = ((IWDMLRouter) to.getRouter()).getPredFor(m.getTo());
        } else {
            pred_val = ((MLProph) to.getRouter()).getPredFor(m.getTo());
        }
        if (m.getTo() == to) {
            pred_val = 1.0;
        }
        Double distance_to_end = to.getLocation().distance(m.getTo().getLocation());
        Double free_buf = (to.getRouter().getFreeBufferSize() - m.getSize()) / 1.0;
        Double time_from_creation = SimClock.getTime() - m.timeCreated;

        Double to_energy = (Double) to.getComBus().
                getProperty(routing.util.EnergyModel.ENERGY_VALUE_ID);

        Double from_energy = (Double) from.getComBus().
                getProperty(routing.util.EnergyModel.ENERGY_VALUE_ID);
        int contacts = MLReport.get_contact_no(from, to);
        Double ratio = MLReport.get_success_ratio(from, to);

        Instance newInstance = new DenseInstance(10);
        newInstance.setValue(0, pred_val);
        newInstance.setValue(1, free_buf);
        newInstance.setValue(2, contacts);
        newInstance.setValue(3, ratio);
        newInstance.setValue(4, to.speed);
        newInstance.setValue(5, distance_to_end);
        newInstance.setValue(6, time_from_creation);
        newInstance.setValue(7, from_energy);
        newInstance.setValue(8, to_energy);
        newInstance.setValue(9, m.getHopCount());
        return newInstance;

    }

    @Override
    protected void init() {
        super.init();

        this.conn_details = new ArrayList<Detail>();
    }

    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        if (isWarmupID(m.getId())) {
            return;
        }

    }

    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
        if (isWarmupID(m.getId())) {
            return;
        }

    }

    public void messageTransferred(Message m, DTNHost from, DTNHost to,
                                   boolean finalTarget) {
        if (isWarmupID(m.getId())) {
            return;
        }

        updateSuccessCounter(noOfSuccessfulContactsBetweenToNodeAndFromNode,
                from, to);
        updateNumaratorInRatioMap(
                ratioOfSuccessfulTransfersToTotalTransferInitiated, from, to);

        if (finalTarget) {
            // updateSuccessCounter(noOfSuccessfulMsgDeliveriesBetweenToNodeAndFromNode,
            // from, to);8800928054
            // updateNumaratorInRatioMap(ratioOfSuccessfulMsgDeliveriesToTotalMsgCreated,
            // from, to);9868861080
            for (int i = 0; i < conn_details.size(); i++) {
                if (m.path.contains(conn_details.get(i).from)
                        && m.path.contains(conn_details.get(i).to)
                        && m.getId() == conn_details.get(i).mid) {// means that
                    // success
                    // connection
                    // delivery.
                    conn_details.get(i).set_successful_delivery();
                }
            }

        }
    }

    private void updateSuccessCounter(
            Map<DTNHost, Map<DTNHost, Integer>> ratioMap, DTNHost from,
            DTNHost to) {
        if (!ratioMap.containsKey(to)) {
            ratioMap.put(to, new HashMap<DTNHost, Integer>());
            ratioMap.get(to).put(from, 1);
        } else {
            if (!ratioMap.get(to).containsKey(from)) {
                ratioMap.get(to).put(from, 1);
            } else {
                Integer updatedNoOfContacts = ratioMap.get(to).get(from) + 1;
                ratioMap.get(to).put(from, updatedNoOfContacts);
            }
        }
    }

    private void updateNumaratorInRatioMap(
            Map<DTNHost, Map<DTNHost, Ratio>> ratioMap, DTNHost from, DTNHost to) {
        if (!ratioMap.containsKey(to)) {
            throw new RuntimeException("");
        } else if (!ratioMap.get(to).containsKey(from)) {
            throw new RuntimeException("");
        } else {
            double prevNumerator = ratioMap.get(to).get(from).getNumerator();
            ratioMap.get(to).get(from).setNumerator(++prevNumerator);
        }
    }

    private void updateDenominatorInRatioMap(
            Map<DTNHost, Map<DTNHost, Ratio>> ratioMap, DTNHost from, DTNHost to) {
        if (!ratioMap.containsKey(to)) {
            ratioMap.put(to, new HashMap<DTNHost, Ratio>());
            ratioMap.get(to).put(from, new Ratio(0, 1));
        } else {
            if (!ratioMap.get(to).containsKey(from)) {
                ratioMap.get(to).put(from, new Ratio(0, 1));
            } else {
                double prevDenominator = ratioMap.get(to).get(from)
                        .getDenominator();
                ratioMap.get(to).get(from).setDenominator(++prevDenominator);
            }
        }
    }

    public void newMessage(Message m) {
        if (isWarmup()) {
            addWarmupID(m.getId());
            return;
        }

        if (m.getResponseSize() > 0) {
        }
        // updateDenominatorInRatioMap(ratioOfSuccessfulTransfersToTotalTransferInitiated,
        // m.getFrom(), m.getTo());
        // updateDenominatorInRatioMap(ratioOfSuccessfulMsgDeliveriesToTotalMsgCreated,
        // m.getFrom(), m.getTo());
    }


    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
        if (isWarmupID(m.getId())) {
            return;
        }
        Double pred_val;
        if ((new Settings(GROUP_NS).getSetting(ROUTER_S)).equals("IWDMLRouter")) {
            pred_val = ((IWDMLRouter) to.getRouter()).getPredFor(m.getTo());
        } else {
            pred_val = ((MLProph) to.getRouter()).getPredFor(m.getTo());
        }
        if (m.getTo() == to) {
            pred_val = 1.0;
        }
        Double distance_to_end = to.getLocation().distance(m.getTo().getLocation());
        Double free_buf = (to.getRouter().getFreeBufferSize() - m.getSize()) / 1.0;
        Double m_dist_travelled = from.getLocation().distance(m.getFrom().getLocation());
        Double time_from_creation = SimClock.getTime() - m.timeCreated;

        Double to_energy = (Double) to.getComBus().
                getProperty(routing.util.EnergyModel.ENERGY_VALUE_ID);

        Double from_energy = (Double) from.getComBus().
                getProperty(routing.util.EnergyModel.ENERGY_VALUE_ID);
        int x;
        Double ratio;
        if (noOfSuccessfulContactsBetweenToNodeAndFromNode.get(to) == null) {
            x = 0;
        } else {
            if (noOfSuccessfulContactsBetweenToNodeAndFromNode.get(to).get(from) == null) {
                x = 0;
            } else {
                x = noOfSuccessfulContactsBetweenToNodeAndFromNode
                        .get(to)
                        .get(from);
            }
        }
        if (ratioOfSuccessfulTransfersToTotalTransferInitiated.get(to) == null) {
            ratio = 0.0;
        } else {
            if (ratioOfSuccessfulTransfersToTotalTransferInitiated.get(to).get(from) == null) {
                ratio = 0.0;
            } else {
                ratio = ratioOfSuccessfulTransfersToTotalTransferInitiated
                        .get(to)
                        .get(from).evaluateRatio();
            }
        }
        Detail det = new Detail(from, to, m.getId(), free_buf,
                pred_val, from.speed, to.speed, distance_to_end, m_dist_travelled, time_from_creation, from_energy, to_energy, x, ratio, m.getHopCount());
        //System.out.println(String.valueOf(pred_val));
        conn_details.add(det);
        updateDenominatorInRatioMap(
                ratioOfSuccessfulTransfersToTotalTransferInitiated, from,
                to);

    }

    @Override
    public void done() {
        write("@relation EpidemicML\n@attribute prophet_probability numeric\n@attribute buffer_occupency numeric\n@attribute successful_deliveries numeric\n@attribute success_ratio numeric\n@attribute from_speed numeric\n@attribute to_speed numeric\n@attribute distance_to_end numeric\n@attribute distance_from_source numeric\n@attribute message_live_time numeric\n@attribute from_energy numeric\n@attribute to_energy numeric\n@attribute current_hop_count numeric\n@attribute is_succesfully_delivered {0,1}\n@data\n");
        for (int i = 0; i < conn_details.size(); i++) {

            write(conn_details.get(i).prophet_prob
                    + ","
                    + conn_details.get(i).buffer_occupency
                    + ","
                    + conn_details.get(i).contacts
                    + ","
                    + conn_details.get(i).ratio
                    + "," + conn_details.get(i).from_speed + ","
                    + conn_details.get(i).to_speed +
                    "," + conn_details.get(i).distance_to_end + ","
                    + conn_details.get(i).m_from_source + "," +
                    conn_details.get(i).m_time_from_creation + "," +
                    conn_details.get(i).from_energy + "," +
                    conn_details.get(i).to_energy

                    + ","
                    + (conn_details).get(i).current_hop_count
                    + ","
                    + (conn_details.get(i).is_successfully_delivered ? "1" : "0"));
        }

        super.done();
    }

    public Map<DTNHost, Map<DTNHost, Integer>> getNoOfSuccessfulContactsBetweenToNodeAndFromNode() {
        return noOfSuccessfulContactsBetweenToNodeAndFromNode;
    }

    public void setNoOfSuccessfulContactsBetweenToNodeAndFromNode(
            Map<DTNHost, Map<DTNHost, Integer>> noOfContactsBetweenToNodeAndFromNode) {
        noOfSuccessfulContactsBetweenToNodeAndFromNode = noOfContactsBetweenToNodeAndFromNode;
    }

    public Map<DTNHost, Map<DTNHost, Ratio>> getRatioOfSuccessfulMsgDeliveriesToTotalMsgCreated() {
        return ratioOfSuccessfulMsgDeliveriesToTotalMsgCreated;
    }

    public void setRatioOfSuccessfulMsgDeliveriesToTotalMsgCreated(
            Map<DTNHost, Map<DTNHost, Ratio>> ratioOfSuccessfulTransfersToTotalTransfersInitiated) {
        this.ratioOfSuccessfulMsgDeliveriesToTotalMsgCreated = ratioOfSuccessfulTransfersToTotalTransfersInitiated;
    }

    public Map<DTNHost, Map<DTNHost, Ratio>> getRatioOfSuccessfulTransfersToTotalTransferInitiated() {
        return ratioOfSuccessfulTransfersToTotalTransferInitiated;
    }

    public void setRatioOfSuccessfulTransfersToTotalTransferInitiated(
            Map<DTNHost, Map<DTNHost, Ratio>> ratioOfSuccessfulTransfersToTotalTransferInitiated) {
        MLReport.ratioOfSuccessfulTransfersToTotalTransferInitiated = ratioOfSuccessfulTransfersToTotalTransferInitiated;
    }

    public Map<DTNHost, Map<DTNHost, Integer>> getNoOfSuccessfulMsgDeliveriesBetweenToNodeAndFromNode() {
        return noOfSuccessfulMsgDeliveriesBetweenToNodeAndFromNode;
    }

    public void setNoOfSuccessfulMsgDeliveriesBetweenToNodeAndFromNode(
            Map<DTNHost, Map<DTNHost, Integer>> noOfSuccessfulDeliveriesBetweenToNodeAndFromNode) {
        this.noOfSuccessfulMsgDeliveriesBetweenToNodeAndFromNode = noOfSuccessfulDeliveriesBetweenToNodeAndFromNode;
    }

    class Ratio {
        private double Numerator;
        private double Denominator;

        public Ratio(int n, int d) {
            this.setNumerator(n);
            this.setDenominator(d);
        }

        public Ratio() {
            this.setNumerator(0);
            this.setDenominator(0);
        }

        public double evaluateRatio() {
            if (getDenominator() == 0)
                throw new NumberFormatException("Divide by Zero attempted");
            return getNumerator() / getDenominator();
        }

        public double getDenominator() {
            return Denominator;
        }

        public void setDenominator(double prevDenominator) {
            Denominator = prevDenominator;
        }

        public double getNumerator() {
            return Numerator;
        }

        public void setNumerator(double prevNumerator) {
            Numerator = prevNumerator;
        }
    }


}