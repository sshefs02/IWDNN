package routing.hibop;

import core.Coord;

public class HistoryTable {
	
	
	private Coord aggregate;
	
	private int classtype;
    private double heterogeniety,redundancy,continuity;
	
	public HistoryTable() {
    	
	this.redundancy = 0.0;
	this.classtype=0;
	this.aggregate=new Coord(0,0);
	this.continuity=0.0;
	this.heterogeniety=0.0;
			
}
public void setaggregate(Coord value){
	this.aggregate=value;
}
public void setclasstype(int value){
	this.classtype=value;
}
public void setredundancy(double value){
	this.redundancy=value;
}
public void setcontinuity(double value){
	this.continuity=value;
}
public void setheterogeneity(double value){
	this.heterogeniety=value;
}
public Coord getaggregate(){
	return this.aggregate;
}
public int getclasstype(){
	return this.classtype;
}
public double getredundancy(){
	return this.redundancy;
}
public double getcontinuity(){
	return this.continuity;
}
public double getheterogeneity(){
	return this.heterogeniety;
}

}