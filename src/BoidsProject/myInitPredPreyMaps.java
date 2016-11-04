package BoidsProject;

import java.util.TreeMap;
import java.util.concurrent.Callable;



public class myInitPredPreyMaps implements Callable<Boolean> {

	//an overlay for calculations to be used to determine forces acting on a creature
	public Project2 p;
	public myBoid b;									//boid being worked on
	public myBoid[] bAra;								//boid ara being worked on
	public myBoidFlock f, pry, prd;
	public flkVrs fv;
	public int type, nearCount;
	public double predRad, mass, totMaxRadSq,tot2MaxRad, minNghbDistSq, minPredDistSq, min2DistPrey,min2DistNghbr;
	public boolean tor;							//is torroidal
	public final int invSq 		= 0,			//1/sq dist
	 		 		 sqDist 	= 1,
			 		 sqNegDist 	= 2,			//increases by sq as dist from increases
			 		 linDist	= 3, 			//increases linearly as dist increases
					 invLin 	= 4;	//decreases linearly as dist increases, up to some threshold

	public myInitPredPreyMaps(Project2 _p, myBoidFlock _f, myBoidFlock _pry, myBoidFlock _prd, flkVrs _fv, myBoid[] _bAra) {
		p = _p;	f = _f; fv = _fv; pry=_pry; prd=_prd; bAra=_bAra; type = f.type;
		tot2MaxRad = 2* f.totMaxRad;
		totMaxRadSq = f.totMaxRad * f.totMaxRad;
		tor = p.flags[p.useTorroid]; 
		minNghbDistSq = fv.nghbrRad[type] * fv.nghbrRad[type];
		minPredDistSq = fv.predRad[type] * fv.predRad[type];
		min2DistNghbr = 2 * fv.predRad[type];
		min2DistPrey = 2 * fv.predRad[type];
	}		
		//populate arrays with closest neighbors, sorted by distance, so that dist array and neighbor array coincde
	public void findMyNeighbors(myBoid _src){
		if(nearCount >= bAra.length ){		srchForNeighbors(_src, bAra, totMaxRadSq);	}		//if we want to have more than the size of the flock, get the whole flock			
		else{								srchForNeighbors(_src, bAra,minNghbDistSq);	}		
		_src.copySubSetBoidsCol();
	}//findMyNeighbors
	
	//look for all neighbors until found neighborhood, expanding distance
	private void srchForNeighbors(myBoid _src, myBoid[] flock, double minDistSq ){
		Double dist;
		myBoid chk;
		//int numAdded = 0;
		for(int c = 0; c < flock.length; ++c){
			if((flock[c].ID == _src.ID) || (flock[c].neighbors.containsValue(_src))){continue;}
			dist = myPoint._SqrDist(_src.coords[0], flock[c].coords[0]);
			if(dist>minDistSq){continue;}
			//what if same dist as another?
			dist = chkPutDistInMap(_src.neighbors, dist, flock[c]);
			_src.neighLoc.put(flock[c].ID,flock[c].coords[0]);
			flock[c].neighbors.put(dist, _src);	
			flock[c].neighLoc.put(_src.ID, _src.coords[0]);		
		}
	}//srchForNeighbors	
	
	private Double chkPutDistInMap(TreeMap<Double, myBoid> map, Double distSq, myBoid boid){
		myBoid chk = map.put(distSq, boid);
		while(chk != null){
			//replace chk	if not null
			map.put(distSq, chk);						
			distSq *= 1.0000000001;//mod distance some tiny amount
			chk = map.put(distSq, boid);				
		}
		return distSq;
	}//chkDistInMap
	
	//non-torroidal boundaries
	private void srchForPrey(myBoid _src, myBoid[] flock){
		Double distSq;
		//myBoid chk;
		for(int c = 0; c < flock.length; ++c){
			distSq = myPoint._SqrDist(_src.coords[0], flock[c].coords[0]);
			if(distSq>minPredDistSq){continue;}
			//what if same dist as another?
			distSq = chkPredPutDistInMap(_src.preyFlk,flock[c].predFlk,distSq,_src, flock[c]);
			//distSq = chkPutDistInMap(_src.preyFlk, distSq, flock[c]);
			_src.preyFlkLoc.put(flock[c].ID,flock[c].coords[0]);
			//flock[c].predFlk.put(distSq, _src);			
			//flock[c].predFlkLoc.put(_src.ID, _src.coords[0]);		
		}	
	}
	private void srchForPreyTor(myBoid _src, myBoid[] flock){
		Double distSq, min2dist = min2DistPrey;
		myPoint tarLoc, srcLoc;
		for(int c = 0; c < flock.length; ++c){
			if(_src == null){return;}//_src boid might have been eaten
			tarLoc = new myPoint(flock[c].coords[0]); srcLoc = new myPoint(_src.coords[0]);//resetting because may be changed in calcMinSqDist
			distSq = calcMinDistSq(_src.coords[0], flock[c].coords[0], srcLoc, tarLoc, min2dist);
			if(distSq>minPredDistSq){continue;}
			//what if same dist as another - need to check both src and predflk
			distSq = chkPredPutDistInMap(_src.preyFlk,flock[c].predFlk,distSq,_src, flock[c]);
			//distSq = chkPutDistInMap(_src.preyFlk, distSq, flock[c]);
			_src.preyFlkLoc.put(flock[c].ID,tarLoc);
			//flock[c].predFlk.put(distSq, _src);						
			//flock[c].predFlkLoc.put(_src.ID, srcLoc);		
		}	
	}	
	//need to check 2 flocks for pred
	private Double chkPredPutDistInMap(TreeMap<Double, myBoid> smap,TreeMap<Double, myBoid> dmap, Double distSq, myBoid _sboid, myBoid _dboid){
		myBoid chks4d = smap.put(distSq, _dboid),
				chkd4s = dmap.put(distSq, _sboid);
		while((chks4d != null) || (chkd4s != null)){
			//replace chk	if not null
			smap.put(distSq, chks4d);		
			dmap.put(distSq, chkd4s);
			distSq *= 1.0000000001;//mod distance some tiny amount
			chks4d = smap.put(distSq, _dboid);	
			chkd4s = dmap.put(distSq, _sboid);
		}
		return distSq;
	}//chkDistInMap
	
		//populate arrays with closest neighbors, sorted by distance, so that dist array and neighbor array coincde
	public void findMyNeighborsTor(myBoid _src){
		if(nearCount >= bAra.length ){	
			srchForNeighborsTor(_src, bAra, tot2MaxRad, totMaxRadSq);	
		}		//if we want to have more than the size of the flock, get the whole flock			
		else{					srchForNeighborsTor(_src, bAra, min2DistNghbr,minNghbDistSq);	}
		_src.copySubSetBoidsCol();		
	}//findMyNeighbors
	
	
	//look for all neighbors until found neighborhood, expanding distance - keyed by squared distance
	private void srchForNeighborsTor(myBoid _src, myBoid[] flock, double min2Dist, double minDistSq ){
		Double distSq;
		myBoid chk;
		myPoint tarLoc, srcLoc;
		for(int c = 0; c < flock.length; ++c){
			if((flock[c].ID == _src.ID) || (flock[c].neighbors.containsValue(_src))){continue;}
			tarLoc = new myPoint(flock[c].coords[0]); srcLoc = new myPoint(_src.coords[0]);//resetting because may be changed in calcMinSqDist
			distSq = calcMinDistSq(_src.coords[0], flock[c].coords[0], srcLoc, tarLoc, min2Dist);
			if(distSq>minDistSq){continue;}
			//what if same dist as another?
			distSq = chkPutDistInMap(_src.neighbors, distSq, flock[c]);
			_src.neighLoc.put(flock[c].ID,tarLoc);
			flock[c].neighbors.put(distSq, _src);			
			flock[c].neighLoc.put(_src.ID, srcLoc);		
		}
	}//srchForNeighbors
	
	public double calcMinDist1D(double p1, double p2, double dim, double[] newP1, double[] newP2){
		double 	d1 = (p1-p2),		d1s = d1*d1,
				d2 = (p1+dim-p2),	d2s = d2*d2,
				d3 = (p1-dim-p2),	d3s = d3*d3;
		if(d1s <= d2s){
			if(d1s <= d3s){	newP1[0] = p1;newP2[0] = p2;return d1s;} 			//d1s is min, for this dim newP1 == p1 and newP2 == p2
			else {			newP1[0] = p1-dim;newP2[0] = p2+dim;return d3s;}} 	//d3is the min
		else {
			if(d2s <= d3s){		newP1[0] = p1+dim;newP2[0] = p2-dim;return d2s;}	//d2s is min, for this dim newP1 == p1 and newP2 == p2 
			else {				newP1[0] = p1-dim;newP2[0] = p2+dim;return d3s;}}	//d3is the min
	}//calcMinDist1D
	
	//returns the minimum sq length vector from p1 to p2
	//taking into account torroidal mapping
	public Double calcMinDistSq(myPoint pt1, myPoint pt2, myPoint newPt1, myPoint newPt2, double rad2){
		Double dist = myPoint._SqrDist(pt1, pt2);
		if(dist <= 1.1f*rad2){return dist;}			//means points are already closer to each other in regular space so they don't need to be special referenced.
		//we're here because two like-species boids are further from each other than 2x the passed radius - now we have to find the closest they could be to each other
		double[] newP1 = new double[]{0}, newP2 = new double[]{0};
		double dx = calcMinDist1D(pt1.x, pt2.x, p.gridDimW-1 ,newP1, newP2);
		newPt1.x = newP1[0];newPt2.x = newP2[0];
		double dy = calcMinDist1D(pt1.y, pt2.y, p.gridDimDp-1 ,newP1, newP2);
		newPt1.y = newP1[0];newPt2.y = newP2[0];
		double dz = calcMinDist1D(pt1.z, pt2.z, p.gridDimH-1 ,newP1, newP2);
		newPt1.z = newP1[0];newPt2.z = newP2[0];
		return dx+dy+dz;
	}	
	
	public void run(){	
		if(tor){
			for(int c = 0; c < bAra.length; ++c){findMyNeighborsTor(bAra[c]);}						//find neighbors to each boid			
			if(p.flags[p.flkHunt] &&(f!=pry)){//will == if only 1 flock
				for(int c = 0; c < bAra.length; ++c){
					if(bAra[c].canSpawn()){	bAra[c].copySubSetBoidsMate();	}
					if(bAra[c].isHungry()){srchForPreyTor(bAra[c], pry.boidFlock);}}						//find neighbors to each boid		
			}

		} else {
			for(int c = 0; c < bAra.length; ++c){findMyNeighbors(bAra[c]);}						//find neighbors to each boid		
			if(p.flags[p.flkHunt] &&(f!=pry)){//will == if only 1 flock
				for(int c = 0; c < bAra.length; ++c){
					if(bAra[c].canSpawn()){	bAra[c].copySubSetBoidsMate();}
					if(bAra[c].isHungry()){srchForPrey(bAra[c], pry.boidFlock);}}						//find neighbors to each boid		
			}

		}
	}
	
	@Override
	public Boolean call() throws Exception {
		run(); return true;
	}
	
	
	public String toString(){
		String res = "";
		return res;
	}

	
}//class myStencil