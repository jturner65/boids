package BoidsProject;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;



public class myInitPredPreyMaps implements Callable<Boolean> {

	//an overlay for calculations to be used to determine forces acting on a creature
	public Project2 p;
	//public myBoid b;									//boid being worked on
	public List<myBoid> bAra;								//boid ara being worked on
	public myBoidFlock f, pry, prd;
	public flkVrs fv;
	public int type, nearCount;
	public float predRad, mass, totMaxRadSq,tot2MaxRad, minNghbDistSq, minPredDistSq, min2DistPrey,min2DistNghbr, colRadSq, spawnRadSq;
	public boolean tor;							//is torroidal
	public final int invSq 		= 0,			//1/sq dist
	 		 		 sqDist 	= 1,
			 		 sqNegDist 	= 2,			//increases by sq as dist from increases
			 		 linDist	= 3, 			//increases linearly as dist increases
					 invLin 	= 4;	//decreases linearly as dist increases, up to some threshold

	public myInitPredPreyMaps(Project2 _p, myBoidFlock _f, myBoidFlock _pry, myBoidFlock _prd, flkVrs _fv, List<myBoid> _bAra) {
		p = _p;	f = _f; fv = _fv; pry=_pry; prd=_prd; bAra=_bAra; type = f.type;
		tot2MaxRad = 2* f.totMaxRad;
		totMaxRadSq = f.totMaxRad * f.totMaxRad;
		tor = p.flags[p.useTorroid]; 
		colRadSq = fv.colRad[type]* fv.colRad[type];
		spawnRadSq = fv.spawnRad[type]* fv.spawnRad[type];
		minNghbDistSq = fv.nghbrRad[type] * fv.nghbrRad[type];
		minPredDistSq = fv.predRad[type] * fv.predRad[type];
		min2DistNghbr = 2 * fv.predRad[type];
		min2DistPrey = 2 * fv.predRad[type];
	}		
		//populate arrays with closest neighbors, sorted by distance, so that dist array and neighbor array coincde
	public void findMyNeighbors(myBoid _src){
		if(nearCount >= bAra.size() ){		srchForNeighbors(_src, bAra, totMaxRadSq);	}		//if we want to have more than the size of the flock, get the whole flock			
		else{								srchForNeighbors(_src, bAra,minNghbDistSq);	}		
		_src.copySubSetBoidsCol(colRadSq);			
	}//findMyNeighbors
	
	//look for all neighbors until found neighborhood, expanding distance
	private void srchForNeighbors(myBoid _src, List<myBoid> flock, float minDistSq ){
		Float distSq;
		//int numAdded = 0;
		for(myBoid chk : flock){
		//for(int c = 0; c < flock.size(); ++c){
			//if((chk.ID == _src.ID) || (chk.neighbors.containsKey(_src.ID))){continue;}
			if(chk.ID == _src.ID){continue;}
			distSq = myPointf._SqrDist(_src.coords, chk.coords);
			if(distSq>minDistSq){continue;}
			//what if same dist as another?
			//distSq = chkPutDistInMap(_src.neighbors, chk.neighbors, distSq, _src, chk);
			//_src.neighLoc.put(chk.ID,chk.coords);
			//chk.neighLoc.put(_src.ID, _src.coords);		
			distSq = chkPutDistInMap(_src.neighLoc,chk.neighLoc,distSq,_src.coords, chk.coords);
			_src.neighbors.put(distSq,chk);
			chk.neighbors.put(distSq, _src);		
		}
	}//srchForNeighbors	
	//populate arrays with closest neighbors, sorted by distance, so that dist array and neighbor array coincde
	public void findMyNeighborsTor(myBoid _src){
		if(nearCount >= bAra.size() ){	srchForNeighborsTor(_src, bAra, tot2MaxRad, totMaxRadSq);	}		//if we want to have more than the size of the flock, get the whole flock			
		else{					srchForNeighborsTor(_src, bAra, min2DistNghbr,minNghbDistSq);	}
		_src.copySubSetBoidsCol(colRadSq);		
	}//findMyNeighbors
	
	
	//look for all neighbors until found neighborhood, expanding distance - keyed by squared distance
	private void srchForNeighborsTor(myBoid _src, List<myBoid> flock, float min2Dist, float minDistSq ){
		Float distSq;
		myPointf tarLoc, srcLoc;
		for(myBoid chk : flock){
		//for(int c = 0; c < flock.size(); ++c){
			//if((chk.ID == _src.ID) || (chk.neighbors.containsKey(_src.ID))){continue;}
			if(chk.ID == _src.ID){continue;}
			tarLoc = new myPointf(chk.coords); srcLoc = new myPointf(_src.coords);//resetting because may be changed in calcMinSqDist
			distSq = calcMinDistSq(_src.coords, chk.coords, srcLoc, tarLoc, min2Dist);
			if(distSq>minDistSq){continue;}
			//what if same dist as another?
//			distSq = chkPutDistInMap(_src.neighbors, chk.neighbors, distSq, _src, chk);
//			_src.neighLoc.put(distSq,tarLoc);
//			chk.neighLoc.put(distSq, srcLoc);		
			distSq = chkPutDistInMap(_src.neighLoc,chk.neighLoc,distSq,srcLoc, tarLoc);
			_src.neighbors.put(distSq, chk);
			chk.neighbors.put(distSq, _src);		
		}
	}//srchForNeighbors	

	//non-torroidal boundaries
	private void srchForPrey(myBoid _src, List<myBoid> preyflock){
		Float distSq;
		for(myBoid prey : preyflock){
			distSq = myPointf._SqrDist(_src.coords, prey.coords);
			if(distSq>minPredDistSq){continue;}
			//what if same dist as another?
			//distSq = chkPutDistInMap(_src.preyFlk,chk.predFlk,distSq,_src, chk);
			distSq = chkPutDistInMap(_src.preyFlkLoc,prey.predFlkLoc,distSq,_src.coords, prey.coords);
			_src.preyFlk.put(distSq, prey);
		}	
	}
	private void srchForPreyTor(myBoid _src, List<myBoid> preyflock){
		Float distSq, min2dist = min2DistPrey;
		myPointf preyLoc, srcLoc;
		for(myBoid prey : preyflock){
		//for(int c = 0; c < flock.size(); ++c){
			if(_src == null){return;}//_src boid might have been eaten
			preyLoc = new myPointf(prey.coords); srcLoc = new myPointf(_src.coords);//resetting because may be changed in calcMinSqDist
			distSq = calcMinDistSq(_src.coords, prey.coords, srcLoc, preyLoc, min2dist);
			if(distSq>minPredDistSq){continue;}
			//what if same dist as another - need to check both src and predflk
			//distSq = chkPutDistInMap(_src.preyFlk,chk.predFlk,distSq,_src, chk);
			distSq = chkPutDistInMap(_src.preyFlkLoc,prey.predFlkLoc,distSq,srcLoc, preyLoc);
			_src.preyFlk.put(distSq, prey);	
		}	
	}	
//	//need to check 2 flocks for pred - this will make sure any predators or prey at the same distance as other preds/prey will get moved a bit further away(instead of colliding)
//	private Float chkPutDistInMapBoid(ConcurrentSkipListMap<Float, myBoid> smap,ConcurrentSkipListMap<Float, myBoid> dmap, Float distSq, myBoid _sboid, myBoid _dboid){
//		myBoid chks4d = smap.get(distSq),
//				chkd4s = dmap.get(distSq);
//		//int iter=0;
//		while((chks4d != null) || (chkd4s != null)){
//			//replace chk	if not null
//			distSq *= 1.0000001;//mod distance some tiny amount
//			chks4d = smap.get(distSq);
//			chkd4s = dmap.get(distSq);
//			//System.out.println("chkPutDistInMap collision : " + distSq + " iter : " + iter++ );
//		}
//		chks4d = smap.put(distSq, _dboid);	
//		chkd4s = dmap.put(distSq, _sboid);
//		return distSq;
//	}//chkDistInMap
	
	//check if src boid map or tar boid map contain passed dist already - if so, increase dist a bit to put in unoccupied location in map
	private Float chkPutDistInMap(ConcurrentSkipListMap<Float, myPointf> smap,ConcurrentSkipListMap<Float, myPointf> dmap, Float distSq, myPointf _sLoc, myPointf _dLoc){
		myPointf chks4d = smap.get(distSq),
				chkd4s = dmap.get(distSq);
		//int iter=0;
		while((chks4d != null) || (chkd4s != null)){
			//replace chk	if not null
			distSq *= 1.0000001f;//mod distance some tiny amount
			chks4d = smap.get(distSq);
			chkd4s = dmap.get(distSq);
			//System.out.println("chkPutDistInMap collision : " + distSq + " iter : " + iter++ );
		}
		chks4d = smap.put(distSq, _dLoc);	
		chkd4s = dmap.put(distSq, _sLoc);
		return distSq;
	}//chkDistInMap
	
	//finds closest dimension - returns square of that distance
	public float calcMinDist1D(float p1, float p2, float dim, float[] newP1, float[] newP2){
		float 	d1 = (p1-p2),		d1s = d1*d1,
				d2 = (p1-(p2-dim)),	d2s = d2*d2,
				d3 = ((p1-dim)-p2),	d3s = d3*d3;
		if(d1s <= d2s){
			if(d1s <= d3s){	newP1[0] = p1;newP2[0] = p2;return d1s;} 			//d1s is min, for this dim newP1 == p1 and newP2 == p2
			else {			newP1[0] = p1-dim;newP2[0] = p2+dim;return d3s;}} 	//d3is the min
		else {
			if(d2s <= d3s){		newP1[0] = p1+dim;newP2[0] = p2-dim;return d2s;}	//d2s is min, for this dim newP1 == p1 and newP2 == p2 
			else {				newP1[0] = p1-dim;newP2[0] = p2+dim;return d3s;}}	//d3is the min
	}//calcMinDist1D
	
	//returns the minimum sq length vector from p1 to p2 for torroidal mapping; puts "virtual location" of torroidal mapped distances in newPt1 and newPt2
	public Float calcMinDistSq(myPointf pt1, myPointf pt2, myPointf newPt1, myPointf newPt2, float minSqDist){
		Float dist = myPointf._SqrDist(pt1, pt2);
		if(dist <= minSqDist){return dist;}			//means points are already closer to each other in regular space so they don't need to be special referenced.
		//we're here because two boids are further from each other than the passed distance - now we have to find the closest they could be to each other given torroidal wrapping
		float[] newP1 = new float[]{0}, newP2 = new float[]{0};
		float dx = calcMinDist1D(pt1.x, pt2.x, p.gridDimX ,newP1, newP2);
		newPt1.x = newP1[0];newPt2.x = newP2[0];
		float dy = calcMinDist1D(pt1.y, pt2.y, p.gridDimY ,newP1, newP2);
		newPt1.y = newP1[0];newPt2.y = newP2[0];
		float dz = calcMinDist1D(pt1.z, pt2.z, p.gridDimZ ,newP1, newP2);
		newPt1.z = newP1[0];newPt2.z = newP2[0];
		return dx+dy+dz;
	}	
		
	public void run(){	
		if(tor){
			for(myBoid b : bAra){	findMyNeighborsTor(b);		}						//find neighbors to each boid	
			if(p.flags[p.flkHunt] &&(f!=pry)){//f!=pry means only 1 flock
				//System.out.println("Prey flock for " + f.name + " = " + pry.name);
				for(myBoid b : bAra){	if(b.isHungry()){srchForPreyTor(b, pry.boidFlock);}}						//find neighbors to each boid		
			}
		} else {
			for(myBoid b : bAra){	findMyNeighbors(b);}						//find neighbors to each boid		
			if(p.flags[p.flkHunt] &&(f!=pry)){//will == if only 1 flock
				for(myBoid b : bAra){if(b.isHungry()){srchForPrey(b, pry.boidFlock);}}						//find neighbors to each boid		
			}
		}
		//find subset of neighbors who are potential mates
		if (p.flags[p.flkSpawn]) {	for(myBoid b : bAra){	if(b.canSpawn()){	b.copySubSetBoidsMate(spawnRadSq);	}}	}
	}//run()
	
	@Override
	public Boolean call() throws Exception {
		run(); return true;
	}
	
	
	public String toString(){
		String res = "";
		return res;
	}

	
}//class myStencil