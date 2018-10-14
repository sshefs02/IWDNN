package routing.iicar;

import java.util.HashMap;
import java.util.Map;

public class GeneTable{
	private Map<Integer,Integer> genes;
	
	public void intialize(){
		genes = new HashMap<>();
		int j=0;
		for(int i=0;i<200;i++){
			genes.put(i, j%4);
			j++;
		}
	}
	public int getGenes(int nodeId) {
		return genes.get(nodeId);
	}

	public void updateGenes(int nodeId, int gene) {
		this.genes.put(nodeId, gene);
	}
	
	
	
}