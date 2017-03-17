package BoidsProject;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ThreadLocalRandom;

public class myFwdStencil implements Callable<Boolean> {

	//an overlay for calculations to be used to determine forces acting on a creature
	public Project2 p;
	public myBoid b;								//boid being worked on
	public List<myBoid> bAra;								//boid being worked on
	public myBoidFlock f, pry, prd;
	public flkVrs fv;
	public int type;
	public double mass, velRadSq, predRadSq, neighRadSq, colRadSq;				
	public final int invSq 		= 0,			//1/sq dist
	 		 		 sqDist 	= 1,
			 		 sqNegDist 	= 2,			//increases by sq as dist from increases
			 		 linDist	= 3, 			//increases linearly as dist increases
					 invLin 	= 4;	//decreases linearly as dist increases, up to some threshold

	public myFwdStencil(Project2 _p, myBoidFlock _f, myBoidFlock _pry, myBoidFlock _prd, flkVrs _fv, List<myBoid> _bAra) {
		p = _p;	f = _f; fv = _fv; pry=_pry; prd=_prd; bAra=_bAra;
		velRadSq = fv.velRad[f.type] * fv.velRad[f.type]; 		
		predRadSq = fv.predRad[f.type] * fv.predRad[f.type];
		neighRadSq = fv.nghbrRad[f.type]* fv.nghbrRad[f.type];
		colRadSq = fv.colRad[f.type] * fv.colRad[f.type];
	}	
	
	//collect to center of local group
	private myVector frcToCenter(){
		double wtSum = 0, wtDist;		
		myVector frcVec = new myVector();		
		for(Double bd_k : b.neighbors.keySet()){	
			//if(null==b.neighbors.get(bd_k)){continue;}
			wtDist = bd_k;//(bd_k*bd_k);
			frcVec._add(myVector._mult(myVector._sub(b.neighLoc.get(b.neighbors.get(bd_k).ID), b.coords[0]), wtDist));
			wtSum += wtDist;	
		}
		frcVec._mult(wtSum == 0 ? 1 : 1.0f/wtSum);	
		return frcVec;
	}

	//avoid collision, avoid predators within radius frcThresh - scale avoidance force by distThresh
	private myVector frcAvoidCol(ConcurrentSkipListMap<Double,myBoid> others, ConcurrentSkipListMap<Integer,myPoint> otherLoc, double distThresh, double frcThresh){
	//private myVector frcAvoidCol(ConcurrentSkipListMap<Double,myBoid> others, double distThresh, double frcThresh){
		myVector frcVec = new myVector();
		double subRes;
		for(Double bd_k : others.keySet()){	
			//if((bd_k>distThresh)||(null==b.neighbors.get(bd_k))){continue;}
			subRes = (frcThresh-bd_k);
			//1 frc at threshold, force <1 past threshold, but increases quickly when less than distthresh -- avoidance	
			frcVec._add(myVector._mult(myVector._sub(b.coords[0],otherLoc.get(others.get(bd_k).ID)), (subRes * subRes) + p.epsValCalc));
			//frcVec._add(myVector._mult(myVector._sub(b.coords[0],others.get(bd_k).coords[0]), (subRes * subRes) + p.epsValCalc));
		}
		return frcVec;
	}//frcAvoidCol
	
	private myVector frcVelMatch(){
		double dsq;
		myVector frcVec = new myVector();		
		for(Double bd_k : b.neighbors.keySet()){	
			//if((bd_k>velRadSq)||(null==b.neighbors.get(bd_k))){continue;}
			if(bd_k>velRadSq){continue;}
			dsq=(velRadSq-bd_k);
			frcVec._add(myVector._mult(myVector._sub(b.neighbors.get(bd_k).velocity[0], b.velocity[0]), dsq));
		}
		return frcVec;
	}
	private myVector frcWander(){		return new myVector(ThreadLocalRandom.current().nextDouble(-mass,mass),ThreadLocalRandom.current().nextDouble(-mass,mass),ThreadLocalRandom.current().nextDouble(-mass,mass));}			//boid independent

	//pass arrays since 2d arrays are arrays of references in java
	private myVector setFrcVal(myVector frc, double[] multV, double[] maxV, int idx){
		frc._mult(multV[idx]);
		if(frc.magn > maxV[idx]){	frc._normalize();	frc._mult(maxV[idx]);	}
		return frc;		
	}
	//return all forces acting upon a certain location - location of boid
	public myVector getForceAtLocation(){
		myVector res = new myVector(), ctrFrc , velMtch , avoidCol , wndrFrc, avoidPred, sprintFromPred, chasePrey; 
		//add all appropriate forces here to res.
		//if exgternal forces to add, add here
		//res._add(getVelAtLocation(b.coords[0]);
		//forceWeights = new TreeMap<Double, myVector>();
		//first find user input forces
		
		if((p.flags[p.mouseClicked] ) && (!p.flags[p.shiftKeyPressed])){res._add(p.mouseForceAtLoc(b.coords[0]));}		
		
		//for pred/prey
		if(!p.flags[p.singleFlock]){
			//check if predators are near and 
			if((b.predFlk.size() !=0) && (p.flags[p.flkAvoidPred])){
				//get avoid force if within neighborhood
				//avoidPred = frcAvoidCol(b.predFlk, neighRadSq, predRadSq);	
				avoidPred = frcAvoidCol(b.predFlk, b.predFlkLoc, neighRadSq, predRadSq);	
				res._add(setFrcVal(avoidPred, fv.wts[type], fv.maxFrcs[type],fv.wFrcAvdPred));	//flee from predators
				if(b.canSprint()){
					//add greater force if within collision radius
					//sprintFromPred = frcAvoidCol(b.predFlk, colRadSq, colRadSq); 
					sprintFromPred = frcAvoidCol(b.predFlk, b.predFlkLoc,  colRadSq, colRadSq); 
					res._add(setFrcVal(sprintFromPred,fv.wts[type], fv.maxFrcs[type],fv.wFrcAvdPred));
					//expensive to sprint, hunger increases
					b.starveCntr-=2;
				}//last gasp, only a brief period for sprint allowed, and can starve prey
			}	
			if((b.preyFlk.size() !=0) && (p.flags[p.flkHunt])){
				myBoid tar = b.preyFlk.firstEntry().getValue(); 
				//double dist = b.preyFlk.firstEntry().getKey();
				//add force at single boid target
				chasePrey = myVector._mult(myVector._sub(tar.coords[0], b.coords[0]),  (fv.eatFreq[type]/(fv.eatFreq[type]-b.starveCntr+2)) );			//increase weight depending on how close to starving
				res._add(setFrcVal(chasePrey,fv.wts[type], fv.maxFrcs[type],fv.wFrcChsPrey));
			}//run after prey, with increasing frc the hungrier we get			
		}				

		if(p.flags[p.flkAvoidCol]){
			avoidCol = frcAvoidCol(b.colliders, b.colliderLoc, colRadSq, colRadSq);	
			//avoidCol = frcAvoidCol(b.colliders, colRadSq, colRadSq);	
			res._add(setFrcVal(avoidCol,fv.wts[type], fv.maxFrcs[type],fv.wFrcAvd));
		}	//then find avoidance forces, if appropriate within f.colRad
		if(p.flags[p.flkCenter]){
			ctrFrc = frcToCenter();		
			res._add(setFrcVal(ctrFrc,fv.wts[type], fv.maxFrcs[type],fv.wFrcCtr));
		}	//then find attracting forces, if appropriate within f.nghbrRad		
		
		if(p.flags[p.flkVelMatch]){			
			velMtch = frcVelMatch();		
			res._add(setFrcVal(velMtch,fv.wts[type], fv.maxFrcs[type],fv.wFrcVel));
		}		//then find velocity matching forces, if appropriate within f.colRad
		
		if(p.flags[p.flkWander]){
			wndrFrc = frcWander();								
			res._add(setFrcVal(wndrFrc,fv.wts[type], fv.maxFrcs[type],fv.wFrcWnd));
		}		//then find wandering forces, if appropriate	
		
		myVector dampFrc = new myVector(b.velocity[0]);
		dampFrc._mult(-fv.dampConst[type]);
		res._add(dampFrc);
		return res;
	}
	
	//returns a vector denoting the environmental velocity like wind or fluid
	public myVector getVelAtLocation(myVector _loc){
		//stam fluid lookup could happen here
		return myVector.ZEROVEC.cloneMe();
	}
	
	//integrator
	public myPoint integrate(myVector stateDot, myPoint state){		return myPoint._add(state, myVector._mult(stateDot, f.delT));}
	public myVector integrate(myVector stateDot, myVector state){	return myVector._add(state, myVector._mult(stateDot, f.delT));}
	
	public void run(){
		for(int i =0; i<bAra.size();++i){
			b = bAra.get(i);
			if(b==null){continue;}
			mass = b.mass;
			type = b.type;

			b.forces[1].set(getForceAtLocation());
			
			b.velocity[1].set(integrate(myVector._mult(b.forces[1], (1/(1.0f*mass))), b.velocity[0]));			//myVector._add(velocity[0], myVector._mult(forces[1], p.delT/(1.0f * mass)));	divide by  mass, multiply by delta t
			if(b.velocity[1].magn > fv.maxVelMag[type]){b.velocity[1]._scale(fv.maxVelMag[type]);}
			if(b.velocity[1].magn < fv.minVelMag[type]){b.velocity[1]._scale(fv.minVelMag[type]);}
			b.coords[1].set(integrate(b.velocity[1], b.coords[0]));												// myVector._add(coords[0], myVector._mult(velocity[1], p.delT));	
			setValWrapCoordsForDraw(b.coords[1]);
		}
	}
	
	@Override
	public Boolean call() throws Exception {
		run(); return true;
	}	
	
	public void setValWrapCoordsForDraw(myPoint _coords){_coords.set(((_coords.x+p.gridDimW) % p.gridDimW),((_coords.y+p.gridDimDp) % p.gridDimDp),((_coords.z+p.gridDimH) % p.gridDimH));	}//findValidWrapCoords	
	
	public String toString(){
		String res = "";
		return res;
	}

	
}//class myStencil