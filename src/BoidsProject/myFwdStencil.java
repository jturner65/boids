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
	myVectorf dampFrc;
	protected float velRadSq, predRadSq, neighRadSq, colRadSq;				
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
		dampFrc = new myVectorf();
	}	
	protected abstract myVectorf frcToCenter(myBoid b); 
	//protected abstract myVectorf frcAvoidCol(myBoid b, ConcurrentSkipListMap<Float,myBoid> others, ConcurrentSkipListMap<Integer,myPointf> otherLoc, float frcThresh);
	protected abstract myVectorf frcAvoidCol(myBoid b, ConcurrentSkipListMap<Float,myPointf> otherLoc, float frcThresh);
	protected abstract myVectorf frcVelMatch(myBoid b);
//	//collect to center of local group
//	private myVectorf frcToCenter(myBoid b){
//		float wtSqSum = 0, wtDist;	
//		myVectorf frcVec = new myVectorf();
//		for(Float bd_k : b.neighbors.keySet()){	
//			//wtDist = bd_k;  //old
//			wtDist = 1.0f/bd_k;//(bd_k*bd_k);
//			//frcVec._add(myVectorf._mult(myVectorf._sub(b.neighLoc.get(b.neighbors.get(bd_k).ID), b.coords[0]), bd_k));
//			frcVec._add(myVectorf._mult(myVectorf._sub(b.neighLoc.get(b.neighbors.get(bd_k).ID), b.coords), wtDist));
//			wtSqSum += wtDist;	
//		}
//		frcVec._div(wtSqSum);				
//		return frcVec;
//	}//frcToCenter
//
//	//avoid collision, avoid predators within radius frcThresh - scale avoidance force by distThresh
//	//private myVectorf frcAvoidCol(myBoid b, ConcurrentSkipListMap<Float,myBoid> others, ConcurrentSkipListMap<Integer,myPointf> otherLoc, float distThresh, float frcThresh){
//	private myVectorf frcAvoidCol(myBoid b, ConcurrentSkipListMap<Float,myBoid> others, ConcurrentSkipListMap<Integer,myPointf> otherLoc, float frcThresh){
//	//private myVectorf frcAvoidCol(ConcurrentSkipListMap<Float,myBoid> others, float distThresh, float frcThresh){
//		myVectorf frcVec = new myVectorf(), tmpVec;
//		float subRes, wtSqSum = 0;
//		for(Float bd_k : others.keySet()){	//already limited to those closer than colRadSq
//			//if((bd_k>distThresh)||(null==b.neighbors.get(bd_k))){continue;}
//			//subRes = (frcThresh - bd_k); 	//old
//			tmpVec = myVectorf._sub(b.coords,otherLoc.get(others.get(bd_k).ID));
//			subRes = 1.0f/(bd_k * tmpVec.magn);
//			wtSqSum += subRes;
//			//frcVec._add(myVectorf._mult(myVectorf._sub(b.coords[0],otherLoc.get(others.get(bd_k).ID)), (subRes * subRes) + p.epsValCalc));
//			frcVec._add(myVectorf._mult(tmpVec, subRes ));
//		}
//		frcVec._div(wtSqSum);	
//		return frcVec;
//	}//frcAvoidCol
//	
//	private myVectorf frcVelMatch(myBoid b){
//		float dsq, wtSqSum = 0;
//		myVectorf frcVec = new myVectorf();		
//		for(Float bd_k : b.neighbors.keySet()){	
//			//if((bd_k>velRadSq)||(null==b.neighbors.get(bd_k))){continue;}
//			if(bd_k>velRadSq){continue;}
//			dsq = 1.0f/bd_k;
//			//dsq=(velRadSq-bd_k);
//			wtSqSum += dsq;
//			frcVec._add(myVectorf._mult(myVectorf._sub(b.neighbors.get(bd_k).velocity[0], b.velocity[0])._normalize(), dsq));
//		}
//		frcVec._div(wtSqSum == 0 ? 1 : wtSqSum);
//		return frcVec;
//	}
	
	private myVectorf frcWander(myBoid b){		return new myVectorf((float)ThreadLocalRandom.current().nextDouble(-b.mass,b.mass),(float)ThreadLocalRandom.current().nextDouble(-b.mass,b.mass),(float)ThreadLocalRandom.current().nextDouble(-b.mass,b.mass));}			//boid independent

	//pass arrays since 2d arrays are arrays of references in java
	private myVectorf setFrcVal(myVectorf frc, float[] multV, float[] maxV, int idx){
		frc._mult(multV[idx]);
		if(frc.magn > maxV[idx]){	frc._mult(maxV[idx]/frc.magn);}
		return frc;		
	}

	//returns a vector denoting the environmental velocity like wind or fluid
	protected myVectorf getVelAtLocation(myVectorf _loc){
		//stam fluid lookup could happen here
		return new myVectorf();
	}
	//all inheriting classes use the same run
	public void run(){
		if((p.flags[p.mouseClicked] ) && (!p.flags[p.shiftKeyPressed])){//add click force : overwhelms all forces - is not scaled
			for(myBoid b : bAra){b.forces[0]._add(p.mouseForceAtLoc(b.coords));}
		}
		if(!p.flags[p.singleFlock]){
			if (p.flags[p.flkHunt]) {//go to closest prey
				if (p.flags[p.flkAvoidPred]){//avoid predators
					for(myBoid b : bAra){
						if (b.predFlkLoc.size() !=0){//avoid predators if they are nearby
							//b.forces[0]._add(setFrcVal(frcAvoidCol(b, b.predFlk, b.predFlkLoc, predRadSq), fv.wts[b.type], fv.maxFrcs[b.type],fv.wFrcAvdPred));	//flee from predators
							b.forces[0]._add(setFrcVal(frcAvoidCol(b, b.predFlkLoc, predRadSq), fv.wts[b.type], fv.maxFrcs[b.type],fv.wFrcAvdPred));	//flee from predators
							if(b.canSprint()){ 
								//add greater force if within collision radius
								//b.forces[0]._add(setFrcVal(frcAvoidCol(b, b.predFlk, b.predFlkLoc,  colRadSq),fv.wts[b.type], fv.maxFrcs[b.type],fv.wFrcAvdPred));
								b.forces[0]._add(setFrcVal(frcAvoidCol(b, b.predFlkLoc,  colRadSq),fv.wts[b.type], fv.maxFrcs[b.type],fv.wFrcAvdPred));
								//expensive to sprint, hunger increases
								--b.starveCntr;
							}//last gasp, only a brief period for sprint allowed, and can starve prey
						}					
					}
				}			
				for(myBoid b : bAra){
//					if ((b.preyFlkLoc.size() !=0) || (b.predFlkLoc.size() !=0)){//if prey exists
//						System.out.println("Flock : " + f.name+" ID : " + b.ID + " preyFlock size : " + b.preyFlkLoc.size()+ " pred flk size : " + b.predFlkLoc.size());
//					}
										
					if (b.preyFlkLoc.size() !=0){//if prey exists
						myPointf tar = b.preyFlkLoc.firstEntry().getValue(); 
						//add force at single boid target
						float mult = (fv.eatFreq[b.type]/(b.starveCntr + 1.0f));
						myVectorf chase = setFrcVal(myVectorf._mult(myVectorf._sub(tar, b.coords),  mult),fv.wts[b.type], fv.maxFrcs[b.type],fv.wFrcChsPrey); 
//						if(b.ID % 100 == 0){
//							System.out.println("Flock : " + f.name+" ID : " + b.ID + " Chase force : " + chase.toString() + " mult : " + mult + " starve : " + b.starveCntr);
//							
//						}
						
						b.forces[0]._add(chase);						
					}
				}
			}		
		}//if ! single flock
		
		if(p.flags[p.flkAvoidCol]){//find avoidance forces, if appropriate within f.colRad
			for(myBoid b : bAra){
				if(b.colliderLoc.size()==0){continue;}
				//b.forces[0]._add(setFrcVal(frcAvoidCol(b, b.colliders, b.colliderLoc, colRadSq),fv.wts[b.type], fv.maxFrcs[b.type],fv.wFrcAvd));
				b.forces[0]._add(setFrcVal(frcAvoidCol(b, b.colliderLoc, colRadSq),fv.wts[b.type], fv.maxFrcs[b.type],fv.wFrcAvd));
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
	protected myVectorf frcToCenter(myBoid b){
		float wtSqSum = 0, wtDist;	
		myVectorf frcVec = new myVectorf();
		for(Float bd_k : b.neighLoc.keySet()){	
			wtDist = 1.0f/bd_k;//(bd_k*bd_k);
			frcVec._add(myVectorf._mult(myVectorf._sub(b.neighLoc.get(bd_k), b.coords), wtDist));
			//frcVec._add(myVectorf._mult(myVectorf._sub(b.neighLoc.get(b.neighbors.get(bd_k).ID), b.coords), wtDist));
			wtSqSum += wtDist;	
		}
		frcVec._div(wtSqSum);				
		return frcVec;
	}//frcToCenter

	//avoid collision, avoid predators within radius frcThresh - scale avoidance force by distThresh
	@Override
	protected myVectorf frcAvoidCol(myBoid b, ConcurrentSkipListMap<Float,myPointf> otherLoc, float frcThresh){
		myVectorf frcVec = new myVectorf(), tmpVec;
		float subRes, wtSqSum = 0;
		for(Float bd_k : otherLoc.keySet()){	//already limited to those closer than colRadSq
			tmpVec = myVectorf._sub(b.coords,otherLoc.get(bd_k));
			subRes = 1.0f/(bd_k * tmpVec.magn);
			wtSqSum += subRes;
			frcVec._add(myVectorf._mult(tmpVec, subRes ));
		}
		frcVec._div(wtSqSum);	
		return frcVec;
	}//frcAvoidCol
	
	@Override
	protected myVectorf frcVelMatch(myBoid b){
		float dsq, wtSqSum = 0;
		myVectorf frcVec = new myVectorf();		
		for(Float bd_k : b.neighbors.keySet()){	
			if(bd_k>velRadSq){continue;}
			dsq = 1.0f/bd_k;
			wtSqSum += dsq;
			frcVec._add(myVectorf._mult(myVectorf._sub(b.neighbors.get(bd_k).velocity[0], b.velocity[0]), dsq));
		}
		frcVec._div(wtSqSum == 0 ? 1 : wtSqSum);
		return frcVec;
	}//frcVelMatch
	
}//myOrigForceStencil

class myLinForceStencil extends myFwdStencil{
	public myLinForceStencil(Project2 _p, myBoidFlock _f, List<myBoid> _bAra) {
		super(_p, _f, _bAra);
		// TODO Auto-generated constructor stub
	}
	
	//collect to center of local group
	@Override
	protected myVectorf frcToCenter(myBoid b){
		float wtSqSum = 0, wtDist;	
		myVectorf frcVec = new myVectorf();
		for(Float bd_k : b.neighLoc.keySet()){	
			wtDist = bd_k;
			frcVec._add(myVectorf._mult(myVectorf._sub(b.neighLoc.get(bd_k), b.coords), wtDist));
			wtSqSum += wtDist;	
		}
		frcVec._div(wtSqSum);				
		return frcVec;
	}//frcToCenter

	//avoid collision, avoid predators within radius frcThresh - scale avoidance force by distThresh
	@Override
	protected myVectorf frcAvoidCol(myBoid b, ConcurrentSkipListMap<Float,myPointf> otherLoc, float frcThresh){
		myVectorf frcVec = new myVectorf(), tmpVec;
		float subRes;
		for(Float bd_k : otherLoc.keySet()){	//already limited to those closer than colRadSq
			tmpVec = myVectorf._sub(b.coords,otherLoc.get(bd_k));
			subRes = (frcThresh - bd_k); 	//old
			frcVec._add(myVectorf._mult(tmpVec, subRes ));
		}
		return frcVec;
	}//frcAvoidCol
	
	@Override
	protected myVectorf frcVelMatch(myBoid b){
		float dsq;
		myVectorf frcVec = new myVectorf();		
		for(Float bd_k : b.neighbors.keySet()){	
			if(bd_k>velRadSq){continue;}
			dsq=(velRadSq-bd_k);
			frcVec._add(myVectorf._mult(myVectorf._sub(b.neighbors.get(bd_k).velocity[0], b.velocity[0]), dsq));
		}
		return frcVec;
	}
	
	
}//myLinForceStencil
