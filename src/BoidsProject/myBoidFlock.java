package BoidsProject;

//import java.util.SortedMap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

public class myBoidFlock {
	public Project2 p;	
	public String name;
	public int numBoids;
	public ArrayList<myBoid> boidFlock;					
	//public myStencil nStenc;
	
	public flkVrs fv;
	
	public final double  distGrwthMod = 1.1f,	//how the radius should grow to look for more creatures for neighbors, if haven't satisfied minimum number
						nearPct = .4f;			//% size of total population to use as neighborhood target, if enough creatures
	
	public double totMaxRad;						//max search distance for neighbors
	public int nearCount;						//# of creatures required to have a neighborhood - some % of total # of creatures, or nearMinCnt, whichever is larger
	public final int nearMinCnt = 5;			//smallest neighborhood size allowed -> 5 or total # of creatures, whichever is smaller
	
	//public double delT;							//boid sim timestep - set at beginning of every update cycle
	
	public boolean[] bflk_flags;	
	public final static int useLifespan 	= 0,		//uses lifespan value of cell
							is3dIDX 		= 1,		//creatures live in 3d world
							boundedPercep 	= 2,		//whether or not creatures have limited perceptions
							wghtSqCom 		= 3;		//whether or not to use the inverse square weighting for the com calculation
	public final int numbflk_flags = 4;	
	
	public final int type, mtFrameSize = 100;	
	public myBoatRndrObj tmpl;							//template to render boid	
	public myBoidFlock preyFlock, predFlock;		//direct reference to flock that is my prey and my predator -- set in main program after init is called
	
	
	public List<Future<Boolean>> callFwdSimFutures, callUpdFutures, callInitFutures, callResetBoidFutures;
	public List<myFwdStencil> callFwdBoidCalcs;
	public List<myUpdateStencil> callUbdBoidCalcs;	
	public List<myInitPredPreyMaps> callInitBoidCalcs;
	public List<myResetBoidStencil> callResetBoidCalcs;
	
	
	public myBoidFlock(Project2 _p, String _name, int _numBoids, int _type, flkVrs _fv){
		p = _p;	name = _name;fv = _fv;
		//delT = .1f;
		setNumBoids(_numBoids);
		initbflk_flags(true);
		bflk_flags[useLifespan]=false;
		totMaxRad = p.gridDimW + p.gridDimDp + p.gridDimH;
		type = _type;

		callFwdBoidCalcs= new ArrayList<myFwdStencil>();
		callFwdSimFutures = new ArrayList<Future<Boolean>>(); 

		callUbdBoidCalcs = new ArrayList<myUpdateStencil>();
		callUpdFutures = new ArrayList<Future<Boolean>>(); 
		
		callInitBoidCalcs = new ArrayList<myInitPredPreyMaps>();
		callInitFutures = new ArrayList<Future<Boolean>>(); 	

		callResetBoidCalcs = new ArrayList<myResetBoidStencil>();
		callResetBoidFutures = new ArrayList<Future<Boolean>>(); 	

	}//myBoidFlock constructor
	//init bflk_flags state machine
	
	public void initbflk_flags(boolean initVal){bflk_flags = new boolean[numbflk_flags];for(int i=0;i<numbflk_flags;++i){bflk_flags[i]=initVal;}}
	public void initFlock(){
		boidFlock = new ArrayList<myBoid>(numBoids);
		//System.out.println("make flock of size : "+ numBoids);
		for(int c = 0; c < numBoids; ++c){
			boidFlock.add(c, new myBoid(p,this,randBoidStLoc(1), type, fv));
		}
	}//initFlock - run after each flock has been constructed
	
	public void setPredPreyTmpl(int predIDX, int preyIDX){
		predFlock = p.flocks[predIDX];//flock 0 preys on flock 2, is preyed on by flock 1
		preyFlock = p.flocks[preyIDX];	
		tmpl = new myBoatRndrObj(p, fv);
	}//set after init - all flocks should be made
	

	//finds valid coordinates if torroidal walls 
	public myPoint findValidWrapCoordsForDraw(myPoint _coords){return new myPoint(((_coords.x+p.gridDimW) % p.gridDimW),((_coords.y+p.gridDimDp) % p.gridDimDp),((_coords.z+p.gridDimH) % p.gridDimH));	}//findValidWrapCoords	
	public void setValidWrapCoordsForDraw(myPoint _coords){_coords.set(((_coords.x+p.gridDimW) % p.gridDimW),((_coords.y+p.gridDimDp) % p.gridDimDp),((_coords.z+p.gridDimH) % p.gridDimH));	}//findValidWrapCoords	
	public double calcRandLocation(double randNum1, double randNum2, double sqDim, double mathCalc, double mult){return ((sqDim/2.0f) + (randNum2 * (sqDim/3.0f) * mathCalc * mult));}
	public myPoint randBoidStLoc(double mult){		return new myPoint(ThreadLocalRandom.current().nextDouble(p.gridDimW),ThreadLocalRandom.current().nextDouble(p.gridDimDp),ThreadLocalRandom.current().nextDouble(p.gridDimH));	}
	
	public void setNumBoids(int _numBoids){
		numBoids = _numBoids;
		nearCount = (int) Math.max(Math.min(nearMinCnt,numBoids), numBoids*nearPct); 		
	}
	
	//adjust boid population by m
	public void modBoidPop(int m){
		if(m>0){if(boidFlock.size() >= p.maxBoats) {return;}for(int i=0;i<m;++i){ addBoid();}} 
		else { int n=-1*m; n = (n>numBoids-1 ? numBoids-1 : n);for(int i=0;i<n;++i){removeBoid();}}
	}//modBoidPop
	
	public myBoid addBoid(){	return addBoid(randBoidStLoc(1));	}	
	public myBoid addBoid(myPoint stLoc){
		myBoid tmp = new myBoid(p,this, stLoc, type, fv); 
		boidFlock.add(tmp);
		setNumBoids(boidFlock.size());
		return tmp;
	}//addBoid	
	
	public void removeBoid(){removeBoid(boidFlock.size()-1);}
	public void removeBoid(int idx){
		if(idx<0){return;}	
		boidFlock.remove(idx);
		setNumBoids(boidFlock.size());
	}//removeBoid	
	
	//move creatures to random start positions
	public void scatterBoids() {for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).coords.set(randBoidStLoc(1));}}//	randInit
	public void drawBoids(){
		if(p.flags[p.drawBoids]){
	  		for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).drawMe();}			
		} else {
			for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).drawMeBall();  }
			for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).drawClosestPrey();  }
			for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).drawClosestPredator();  }
		}
	}
	
	//clear out last time step boid values
	//public void clearOutBoids(){for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).clearAllBoidMaps();}	}					//clear out all neighbors, preds and prey	
	public void clearBoidForces(){for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).forces[0].set(0,0,0);}}	
	//clear out all data for each boid
	public void clearOutBoids(){
		callResetBoidCalcs.clear();
		for(int c = 0; c < boidFlock.size(); c+=mtFrameSize){
			int finalLen = (c+mtFrameSize < boidFlock.size() ? mtFrameSize : boidFlock.size() - c);
			callResetBoidCalcs.add(new myResetBoidStencil(p, this, preyFlock, boidFlock.subList(c, c+finalLen)));
		}							//find next turn's motion for every creature by finding total force to act on creature
		try {callResetBoidFutures = p.th_exec.invokeAll(callResetBoidCalcs);for(Future<Boolean> f: callResetBoidFutures) { f.get(); }} catch (Exception e) { e.printStackTrace(); }			
	}
	//build all data structures holding neighbors, pred, prey
	public void initAllMaps(){
		callInitBoidCalcs.clear();
		for(int c = 0; c < boidFlock.size(); c+=mtFrameSize){
			int finalLen = (c+mtFrameSize < boidFlock.size() ? mtFrameSize : boidFlock.size() - c);
			callInitBoidCalcs.add(new myInitPredPreyMaps(p, this, preyFlock, predFlock, fv, boidFlock.subList(c, c+finalLen)));
		}							//find next turn's motion for every creature by finding total force to act on creature
		try {callInitFutures = p.th_exec.invokeAll(callInitBoidCalcs);for(Future<Boolean> f: callInitFutures) { f.get(); }} catch (Exception e) { e.printStackTrace(); }			
	}
	
	//build forces using linear distance functions
	public void moveBoidsLinMultTH(){
		callFwdBoidCalcs.clear();
		for(int c = 0; c < boidFlock.size(); c+=mtFrameSize){
			int finalLen = (c+mtFrameSize < boidFlock.size() ? mtFrameSize : boidFlock.size() - c);
			callFwdBoidCalcs.add(new myLinForceStencil(p, this, boidFlock.subList(c, c+finalLen)));
		}							//find next turn's motion for every creature by finding total force to act on creature
		try {callFwdSimFutures = p.th_exec.invokeAll(callFwdBoidCalcs);for(Future<Boolean> f: callFwdSimFutures) { f.get(); }} catch (Exception e) { e.printStackTrace(); }		
	}
	
	//build forces using original boids-style distance functions
	public void moveBoidsOrigMultTH(){
		callFwdBoidCalcs.clear();
		for(int c = 0; c < boidFlock.size(); c+=mtFrameSize){
			int finalLen = (c+mtFrameSize < boidFlock.size() ? mtFrameSize : boidFlock.size() - c);
			callFwdBoidCalcs.add(new myOrigForceStencil(p, this, boidFlock.subList(c, c+finalLen)));
		}							//find next turn's motion for every creature by finding total force to act on creature
		try {callFwdSimFutures = p.th_exec.invokeAll(callFwdBoidCalcs);for(Future<Boolean> f: callFwdSimFutures) { f.get(); }} catch (Exception e) { e.printStackTrace(); }		
	}
	public void updateBoidMovement(){
		callUbdBoidCalcs.clear();
		for(int c = 0; c < boidFlock.size(); c+=mtFrameSize){
			int finalLen = (c+mtFrameSize < boidFlock.size() ? mtFrameSize : boidFlock.size() - c);
			callUbdBoidCalcs.add(new myUpdateStencil(p, this, boidFlock.subList(c, c+finalLen)));
		}							//apply update
		try {callUpdFutures = p.th_exec.invokeAll(callUbdBoidCalcs);for(Future<Boolean> f: callUpdFutures) { f.get(); }} catch (Exception e) { e.printStackTrace(); }		    	
    	//update - remove dead, add babies
        myPoint[] bl = new myPoint[]{new myPoint()};
        myVector[] bVelFrc  = new myVector[]{new myVector(),new myVector()};
        myBoid b;
        for(int c = 0; c < boidFlock.size(); ++c){
        	b = boidFlock.get(c);
        	if((b != null) && (b.bd_flags[myBoid.isDead])){    removeBoid(c);  }
        	else {  
        		if(b.hadAChild(bl,bVelFrc)){myBoid tmpBby = this.addBoid(bl[0]); tmpBby.initNewborn(bVelFrc);}}
        } 
	}//updateBoids	

	/////////////////////////// boid functions 

	public String[] getInfoString(){return this.toString().split("\n",-1);}
	
	public String toString(){
		String res = "Flock Size " + boidFlock.size() + "\n";
		for(myBoid bd : boidFlock){			res+="\t     "+bd.toString(); res+="\n";	}
		return res;
	}
		
}//myBoidFlock class
