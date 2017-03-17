package BoidsProject;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ThreadLocalRandom;

public class myFwdStencil implements Callable<Boolean> {
	//an overlay for calculations to be used to determine forces acting on a creature
	private Project2 p;							//boid being worked on
	private flkVrs fv;
	private List<myBoid> bAra;								//boid being worked on
	private myBoidFlock f;
	myVector dampFrc;
	private double velRadSq, predRadSq, neighRadSq, colRadSq;				
	private final int invSq 		= 0,			//1/sq dist
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
	
	//collect to center of local group
	private myVector frcToCenter(myBoid b){
		double wtSum = 0;//, wtDist;		
		myVector frcVec = new myVector();		
		for(Double bd_k : b.neighbors.keySet()){	
			//if(null==b.neighbors.get(bd_k)){continue;}
			//wtDist = bd_k;//(bd_k*bd_k);
			frcVec._add(myVector._mult(myVector._sub(b.neighLoc.get(b.neighbors.get(bd_k).ID), b.coords[0]), bd_k));
			wtSum += bd_k;	
		}
		frcVec._mult(wtSum == 0 ? 1 : 1.0f/wtSum);	
		return frcVec;
	}

	//avoid collision, avoid predators within radius frcThresh - scale avoidance force by distThresh
	private myVector frcAvoidCol(myBoid b, ConcurrentSkipListMap<Double,myBoid> others, ConcurrentSkipListMap<Integer,myPoint> otherLoc, double distThresh, double frcThresh){
	//private myVector frcAvoidCol(ConcurrentSkipListMap<Double,myBoid> others, double distThresh, double frcThresh){
		myVector frcVec = new myVector();
		double subRes;
		for(Double bd_k : others.keySet()){	//already limited to those closer than colRadSq
			//if((bd_k>distThresh)||(null==b.neighbors.get(bd_k))){continue;}
			subRes = (frcThresh-bd_k);
			//1 frc at threshold, force <1 past threshold, but increases quickly when less than distthresh -- avoidance	
			//frcVec._add(myVector._mult(myVector._sub(b.coords[0],otherLoc.get(others.get(bd_k).ID)), (subRes * subRes) + p.epsValCalc));
			frcVec._add(myVector._mult(myVector._sub(b.coords[0],otherLoc.get(others.get(bd_k).ID))._normalize(), subRes ));
			//frcVec._add(myVector._mult(myVector._sub(b.coords[0],others.get(bd_k).coords[0]), (subRes * subRes) + p.epsValCalc));
		}
		return frcVec;
	}//frcAvoidCol
	
	private myVector frcVelMatch(myBoid b){
		double dsq;
		myVector frcVec = new myVector();		
		for(Double bd_k : b.neighbors.keySet()){	
			//if((bd_k>velRadSq)||(null==b.neighbors.get(bd_k))){continue;}
			if(bd_k>velRadSq){continue;}
			dsq=(velRadSq-bd_k);
			frcVec._add(myVector._mult(myVector._sub(b.neighbors.get(bd_k).velocity[0], b.velocity[0])._normalize(), dsq));
		}
		return frcVec;
	}
	private myVector frcWander(myBoid b){		return new myVector(ThreadLocalRandom.current().nextDouble(-b.mass,b.mass),ThreadLocalRandom.current().nextDouble(-b.mass,b.mass),ThreadLocalRandom.current().nextDouble(-b.mass,b.mass));}			//boid independent

	//pass arrays since 2d arrays are arrays of references in java
	private myVector setFrcVal(myVector frc, double[] multV, double[] maxV, int idx){
		frc._mult(multV[idx]);
		if(frc.magn > maxV[idx]){	frc._mult(maxV[idx]/frc.magn);}
		return frc;		
	}
	//return all forces acting upon a certain location - location of boid
	public myVector getForceAtLocation(myBoid b){
		myVector res = new myVector(), ctrFrc , velMtch , avoidCol , wndrFrc, avoidPred, sprintFromPred, chasePrey; 
		//add all appropriate forces here to res.
		//if exgternal forces to add, add here
		//res._add(getVelAtLocation(b.coords[0]);
		//forceWeights = new TreeMap<Double, myVector>();
		//first find user input forces
		
		//forces should be weighted in order of collision avoidance, velocity matching, grouping together - if col force == either of the others, than others should be decreased
		
		if((p.flags[p.mouseClicked] ) && (!p.flags[p.shiftKeyPressed])){res._add(p.mouseForceAtLoc(b.coords[0]));}		
		
		//for pred/prey
		if(!p.flags[p.singleFlock]){
			//check if predators are near and 
			if((b.predFlk.size() !=0) && (p.flags[p.flkAvoidPred])){
				//get avoid force if within neighborhood
				//avoidPred = frcAvoidCol(b.predFlk, neighRadSq, predRadSq);	
				avoidPred = frcAvoidCol(b, b.predFlk, b.predFlkLoc, neighRadSq, predRadSq);	
				res._add(setFrcVal(avoidPred, fv.wts[b.type], fv.maxFrcs[b.type],fv.wFrcAvdPred));	//flee from predators
				if(b.canSprint()){
					//add greater force if within collision radius
					sprintFromPred = frcAvoidCol(b, b.predFlk, b.predFlkLoc,  colRadSq, colRadSq); 
					res._add(setFrcVal(sprintFromPred,fv.wts[b.type], fv.maxFrcs[b.type],fv.wFrcAvdPred));
					//expensive to sprint, hunger increases
					b.starveCntr-=2;
				}//last gasp, only a brief period for sprint allowed, and can starve prey
			}	
			if((b.preyFlk.size() !=0) && (p.flags[p.flkHunt])){
				myBoid tar = b.preyFlk.firstEntry().getValue(); 
				myPoint tarLoc = b.preyFlkLoc.get(tar.ID);
				//add force at single boid target
				chasePrey = myVector._mult(myVector._sub(tarLoc, b.coords[0]),  (fv.eatFreq[b.type]/(fv.eatFreq[b.type]-b.starveCntr+2)) );			//increase weight depending on how close to starving
				res._add(setFrcVal(chasePrey,fv.wts[b.type], fv.maxFrcs[b.type],fv.wFrcChsPrey));
			}//run after prey, with increasing frc the hungrier we get			
		}				

		if(p.flags[p.flkAvoidCol]){
			avoidCol = frcAvoidCol(b, b.colliders, b.colliderLoc, colRadSq, colRadSq);	
			//avoidCol = frcAvoidCol(b.colliders, colRadSq, colRadSq);	
			res._add(setFrcVal(avoidCol,fv.wts[b.type], fv.maxFrcs[b.type],fv.wFrcAvd));
		}	//then find avoidance forces, if appropriate within f.colRad
		
		if(p.flags[p.flkVelMatch]){			
			velMtch = frcVelMatch(b);		
			res._add(setFrcVal(velMtch,fv.wts[b.type], fv.maxFrcs[b.type],fv.wFrcVel));
		}		//then find velocity matching forces, if appropriate within f.colRad
		if(p.flags[p.flkCenter]){
			ctrFrc = frcToCenter(b);		
			res._add(setFrcVal(ctrFrc,fv.wts[b.type], fv.maxFrcs[b.type],fv.wFrcCtr));
		}	//then find attracting forces, if appropriate within f.nghbrRad		
		
		if(p.flags[p.flkWander]){//brownian motion
			wndrFrc = frcWander(b);								
			res._add(setFrcVal(wndrFrc,fv.wts[b.type], fv.maxFrcs[b.type],fv.wFrcWnd));
		}		//then find wandering forces, if appropriate	
		
		dampFrc.set(b.velocity[0]);
		dampFrc._mult(-fv.dampConst[b.type]);
		res._add(dampFrc);
		return res;
	}
	
	//returns a vector denoting the environmental velocity like wind or fluid
	public myVector getVelAtLocation(myVector _loc){
		//stam fluid lookup could happen here
		return myVector.ZEROVEC.cloneMe();
	}
	
	public void run(){
		for(myBoid b : bAra){b.forces[0].set(getForceAtLocation(b));}
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