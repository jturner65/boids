package BoidsProject;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ThreadLocalRandom;

public abstract class myFwdStencil implements Callable<Boolean> {
	//an overlay for calculations to be used to determine forces acting on a creature
	protected Project2 p;							//boid being worked on
	protected flkVrs fv;
	protected List<myBoid> bAra;								//boid being worked on
	protected myBoidFlock f;
	myVector dampFrc;
	protected double velRadSq, predRadSq, neighRadSq, colRadSq;				
	protected final int invSq 		= 0,			//1/sq dist
	 		 		 sqDist 	= 1,
			 		 sqNegDist 	= 2,			//increases by sq as dist from increases
			 		 linDist	= 3, 			//increases linearly as dist increases
					 invLin 	= 4;	//decreases linearly as dist increases, up to some threshold

	public myFwdStencil(Project2 _p, myBoidFlock _f, List<myBoid> _bAra) {
		p = _p;	f = _f;fv = f.fv; bAra=_bAra;
		//pry = f.preyFlock; prd = f.predFlock;
		velRadSq = fv.velRad[f.type] * fv.velRad[f.type]; 		
		predRadSq = fv.predRad[f.type] * fv.predRad[f.type];
		neighRadSq = fv.nghbrRad[f.type]* fv.nghbrRad[f.type];
		colRadSq = fv.colRad[f.type] * fv.colRad[f.type];
		dampFrc = new myVector();
	}	
	protected abstract myVector frcToCenter(myBoid b); 
	protected abstract myVector frcAvoidCol(myBoid b, ConcurrentSkipListMap<Double,myBoid> others, ConcurrentSkipListMap<Integer,myPoint> otherLoc, double frcThresh);
	protected abstract myVector frcVelMatch(myBoid b);
//	//collect to center of local group
//	private myVector frcToCenter(myBoid b){
//		double wtSqSum = 0, wtDist;	
//		myVector frcVec = new myVector();
//		for(Double bd_k : b.neighbors.keySet()){	
//			//wtDist = bd_k;  //old
//			wtDist = 1.0/bd_k;//(bd_k*bd_k);
//			//frcVec._add(myVector._mult(myVector._sub(b.neighLoc.get(b.neighbors.get(bd_k).ID), b.coords[0]), bd_k));
//			frcVec._add(myVector._mult(myVector._sub(b.neighLoc.get(b.neighbors.get(bd_k).ID), b.coords), wtDist));
//			wtSqSum += wtDist;	
//		}
//		frcVec._div(wtSqSum);				
//		return frcVec;
//	}//frcToCenter
//
//	//avoid collision, avoid predators within radius frcThresh - scale avoidance force by distThresh
//	//private myVector frcAvoidCol(myBoid b, ConcurrentSkipListMap<Double,myBoid> others, ConcurrentSkipListMap<Integer,myPoint> otherLoc, double distThresh, double frcThresh){
//	private myVector frcAvoidCol(myBoid b, ConcurrentSkipListMap<Double,myBoid> others, ConcurrentSkipListMap<Integer,myPoint> otherLoc, double frcThresh){
//	//private myVector frcAvoidCol(ConcurrentSkipListMap<Double,myBoid> others, double distThresh, double frcThresh){
//		myVector frcVec = new myVector(), tmpVec;
//		double subRes, wtSqSum = 0;
//		for(Double bd_k : others.keySet()){	//already limited to those closer than colRadSq
//			//if((bd_k>distThresh)||(null==b.neighbors.get(bd_k))){continue;}
//			//subRes = (frcThresh - bd_k); 	//old
//			tmpVec = myVector._sub(b.coords,otherLoc.get(others.get(bd_k).ID));
//			subRes = 1.0/(bd_k * tmpVec.magn);
//			wtSqSum += subRes;
//			//frcVec._add(myVector._mult(myVector._sub(b.coords[0],otherLoc.get(others.get(bd_k).ID)), (subRes * subRes) + p.epsValCalc));
//			frcVec._add(myVector._mult(tmpVec, subRes ));
//		}
//		frcVec._div(wtSqSum);	
//		return frcVec;
//	}//frcAvoidCol
//	
//	private myVector frcVelMatch(myBoid b){
//		double dsq, wtSqSum = 0;
//		myVector frcVec = new myVector();		
//		for(Double bd_k : b.neighbors.keySet()){	
//			//if((bd_k>velRadSq)||(null==b.neighbors.get(bd_k))){continue;}
//			if(bd_k>velRadSq){continue;}
//			dsq = 1.0/bd_k;
//			//dsq=(velRadSq-bd_k);
//			wtSqSum += dsq;
//			frcVec._add(myVector._mult(myVector._sub(b.neighbors.get(bd_k).velocity[0], b.velocity[0])._normalize(), dsq));
//		}
//		frcVec._div(wtSqSum == 0 ? 1 : wtSqSum);
//		return frcVec;
//	}
	
	private myVector frcWander(myBoid b){		return new myVector(ThreadLocalRandom.current().nextDouble(-b.mass,b.mass),ThreadLocalRandom.current().nextDouble(-b.mass,b.mass),ThreadLocalRandom.current().nextDouble(-b.mass,b.mass));}			//boid independent

	//pass arrays since 2d arrays are arrays of references in java
	private myVector setFrcVal(myVector frc, double[] multV, double[] maxV, int idx){
		frc._mult(multV[idx]);
		if(frc.magn > maxV[idx]){	frc._mult(maxV[idx]/frc.magn);}
		return frc;		
	}

	//returns a vector denoting the environmental velocity like wind or fluid
	protected myVector getVelAtLocation(myVector _loc){
		//stam fluid lookup could happen here
		return new myVector();
	}
	//all inheriting classes use the same run
	public void run(){
		if((p.flags[p.mouseClicked] ) && (!p.flags[p.shiftKeyPressed])){//add click force : overwhelms all forces - is not scaled
			for(myBoid b : bAra){b.forces[0]._add(p.mouseForceAtLoc(b.coords));}
		}
		if(!p.flags[p.singleFlock]){
			if (p.flags[p.flkAvoidPred]){//avoid predators
				for(myBoid b : bAra){
					if (b.predFlk.size() !=0){//avoid predators if they are nearby
						b.forces[0]._add(setFrcVal(frcAvoidCol(b, b.predFlk, b.predFlkLoc, predRadSq), fv.wts[b.type], fv.maxFrcs[b.type],fv.wFrcAvdPred));	//flee from predators
						if(b.canSprint()){ 
							//add greater force if within collision radius
							b.forces[0]._add(setFrcVal(frcAvoidCol(b, b.predFlk, b.predFlkLoc,  colRadSq),fv.wts[b.type], fv.maxFrcs[b.type],fv.wFrcAvdPred));
							//expensive to sprint, hunger increases
							b.starveCntr-=2;
						}//last gasp, only a brief period for sprint allowed, and can starve prey
					}					
				}
			}			
			if (p.flags[p.flkHunt]) {//go to closest prey
				for(myBoid b : bAra){
					if (b.preyFlk.size() !=0){//if prey exists
						myBoid tar = b.preyFlk.firstEntry().getValue(); 
						//add force at single boid target
						b.forces[0]._add(setFrcVal(myVector._mult(myVector._sub(b.preyFlkLoc.get(tar.ID), b.coords),  (fv.eatFreq[b.type]/(fv.eatFreq[b.type]-b.starveCntr+2)) ),fv.wts[b.type], fv.maxFrcs[b.type],fv.wFrcChsPrey));						
					}
				}
			}		
		}//if ! single flock
		
		if(p.flags[p.flkAvoidCol]){//find avoidance forces, if appropriate within f.colRad
			for(myBoid b : bAra){
				if(b.colliders.size()==0){continue;}
				b.forces[0]._add(setFrcVal(frcAvoidCol(b, b.colliders, b.colliderLoc, colRadSq),fv.wts[b.type], fv.maxFrcs[b.type],fv.wFrcAvd));
			}
		}				
		
		if(p.flags[p.flkVelMatch]){		//find velocity matching forces, if appropriate within f.colRad	
			for(myBoid b : bAra){
				if(b.neighbors.size()==0){continue;}
				b.forces[0]._add(setFrcVal(frcVelMatch(b),fv.wts[b.type], fv.maxFrcs[b.type],fv.wFrcVel));
			}
		}	
		if(p.flags[p.flkCenter]){ //find attracting forces, if appropriate within f.nghbrRad	
			for(myBoid b : bAra){	
				if(b.neighbors.size()==0){continue;}
				b.forces[0]._add(setFrcVal(frcToCenter(b),fv.wts[b.type], fv.maxFrcs[b.type],fv.wFrcCtr));
			}
		}		
		if(p.flags[p.flkWander]){//brownian motion
			for(myBoid b : bAra){	b.forces[0]._add(setFrcVal(frcWander(b),fv.wts[b.type], fv.maxFrcs[b.type],fv.wFrcWnd));}
		}	
		//damp velocity
		for(myBoid b : bAra){	
			dampFrc.set(b.velocity[0]);
			dampFrc._mult(-fv.dampConst[b.type]);
			b.forces[0]._add(dampFrc);		
		}
		//for(myBoid b : bAra){b.forces[0].set(getForceAtLocation(b));}
	}//run
	
	@Override
	public Boolean call() throws Exception {
		run(); return true;
	}	

}//class myFwdStencil


class myOrigForceStencil extends myFwdStencil{
	public myOrigForceStencil(Project2 _p, myBoidFlock _f, List<myBoid> _bAra) {
		super(_p, _f, _bAra);
		// TODO Auto-generated constructor stub
	}
	
	//collect to center of local group
	@Override
	protected myVector frcToCenter(myBoid b){
		double wtSqSum = 0, wtDist;	
		myVector frcVec = new myVector();
		for(Double bd_k : b.neighbors.keySet()){	
			wtDist = 1.0/bd_k;//(bd_k*bd_k);
			frcVec._add(myVector._mult(myVector._sub(b.neighLoc.get(b.neighbors.get(bd_k).ID), b.coords), wtDist));
			wtSqSum += wtDist;	
		}
		frcVec._div(wtSqSum);				
		return frcVec;
	}//frcToCenter

	//avoid collision, avoid predators within radius frcThresh - scale avoidance force by distThresh
	@Override
	protected myVector frcAvoidCol(myBoid b, ConcurrentSkipListMap<Double,myBoid> others, ConcurrentSkipListMap<Integer,myPoint> otherLoc, double frcThresh){
		myVector frcVec = new myVector(), tmpVec;
		double subRes, wtSqSum = 0;
		for(Double bd_k : others.keySet()){	//already limited to those closer than colRadSq
			tmpVec = myVector._sub(b.coords,otherLoc.get(others.get(bd_k).ID));
			subRes = 1.0/(bd_k * tmpVec.magn);
			wtSqSum += subRes;
			frcVec._add(myVector._mult(tmpVec, subRes ));
		}
		frcVec._div(wtSqSum);	
		return frcVec;
	}//frcAvoidCol
	
	@Override
	protected myVector frcVelMatch(myBoid b){
		double dsq, wtSqSum = 0;
		myVector frcVec = new myVector();		
		for(Double bd_k : b.neighbors.keySet()){	
			if(bd_k>velRadSq){continue;}
			dsq = 1.0/bd_k;
			wtSqSum += dsq;
			frcVec._add(myVector._mult(myVector._sub(b.neighbors.get(bd_k).velocity[0], b.velocity[0]), dsq));
		}
		frcVec._div(wtSqSum == 0 ? 1 : wtSqSum);
		return frcVec;
	}
	
}//myOrigForceStencil

class myLinForceStencil extends myFwdStencil{
	public myLinForceStencil(Project2 _p, myBoidFlock _f, List<myBoid> _bAra) {
		super(_p, _f, _bAra);
		// TODO Auto-generated constructor stub
	}
	
	//collect to center of local group
	@Override
	protected myVector frcToCenter(myBoid b){
		double wtSqSum = 0, wtDist;	
		myVector frcVec = new myVector();
		for(Double bd_k : b.neighbors.keySet()){	
			wtDist = bd_k;
			frcVec._add(myVector._mult(myVector._sub(b.neighLoc.get(b.neighbors.get(bd_k).ID), b.coords), wtDist));
			wtSqSum += wtDist;	
		}
		frcVec._div(wtSqSum);				
		return frcVec;
	}//frcToCenter

	//avoid collision, avoid predators within radius frcThresh - scale avoidance force by distThresh
	@Override
	protected myVector frcAvoidCol(myBoid b, ConcurrentSkipListMap<Double,myBoid> others, ConcurrentSkipListMap<Integer,myPoint> otherLoc, double frcThresh){
		myVector frcVec = new myVector(), tmpVec;
		double subRes;
		for(Double bd_k : others.keySet()){	//already limited to those closer than colRadSq
			tmpVec = myVector._sub(b.coords,otherLoc.get(others.get(bd_k).ID));
			subRes = (frcThresh - bd_k); 	//old
			frcVec._add(myVector._mult(tmpVec, subRes ));
		}
		return frcVec;
	}//frcAvoidCol
	
	@Override
	protected myVector frcVelMatch(myBoid b){
		double dsq;
		myVector frcVec = new myVector();		
		for(Double bd_k : b.neighbors.keySet()){	
			if(bd_k>velRadSq){continue;}
			dsq=(velRadSq-bd_k);
			frcVec._add(myVector._mult(myVector._sub(b.neighbors.get(bd_k).velocity[0], b.velocity[0])._normalize(), dsq));
		}
		return frcVec;
	}
	
	
}//myOrigForceStencil
