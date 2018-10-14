package core;

public class Detail {
	
	public Double m_from_source;
	public Double prophet_prob;
	public Double buffer_occupency;
	public int contacts;
	public Double ratio;
	public Double from_speed;
	public Double to_speed;
	public Double distance_to_end;
	public Double m_time_from_creation;
	public Double from_energy;
	public Double to_energy;
	public int current_hop_count;
	public boolean is_successfully_delivered;
	
	public DTNHost from, to;
	public String mid;
	public int x;
	
	public Detail(DTNHost from,DTNHost to, String mid, Double free_buf,
			Double pred_val, Double fromspeed, Double tospeed, Double distance_to_end, Double m_dist_travelled,
			Double time_from_creation, Double from_energy, Double to_energy,int x,Double ratio,int hopCount)
	{
		m_from_source = m_dist_travelled;
		prophet_prob = pred_val;
		buffer_occupency = free_buf;
		contacts = x;
		this.ratio = ratio;
		this.mid = mid;
		from_speed = fromspeed;
		to_speed = tospeed;
		this.distance_to_end = distance_to_end;
		m_time_from_creation = time_from_creation;
		this.from_energy = from_energy;
		this.to_energy = to_energy; 
		current_hop_count = hopCount;
		
		this.from = from;
		this.to = to;
		
	}

	public void set_successful_delivery() {
		// TODO Auto-generated method stub
		is_successfully_delivered = true;
	}

}
