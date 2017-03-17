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
	
	public double delT;							//boid sim timestep - set at beginning of every update cycle
	
	public boolean[] bflk_flags;	
	public final static int useLifespan 	= 0,		//uses lifespan value of cell
							is3dIDX 		= 1,		//creatures live in 3d world
							boundedPercep 	= 2,		//whether or not creatures have limited perceptions
							wghtSqCom 		= 3;		//whether or not to use the inverse square weighting for the com calculation
	public final int numbflk_flags = 4;	
	
	public final int type, mtFrameSize = 100;	
	public myBoatRndrObj tmpl;							//template to render boid	
	public myBoidFlock preyFlock, predFlock;		//direct reference to flock that is my prey and my predator -- set in main program after init is called
	
	
	public List<Future<Boolean>> callFwdSimFutures, callUpdFutures, callInitFutures;
	public List<myFwdStencil> callFwdBoidCalcs;
	public List<myUpdateStencil> callUbdBoidCalcs;	
	public List<myInitPredPreyMaps> callInitBoidCalcs;
	
	
	public myBoidFlock(Project2 _p, String _name, int _numBoids, int _type, flkVrs _fv){
		p = _p;	name = _name;fv = _fv;
		delT = .1f;
		numBoids = _numBoids;
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

	}//myBoidFlock constructor
	//init bflk_flags state machine
	
	public void initbflk_flags(boolean initVal){bflk_flags = new boolean[numbflk_flags];for(int i=0;i<numbflk_flags;++i){bflk_flags[i]=initVal;}}
	public void initFlock(){
		boidFlock = new ArrayList<myBoid>(numBoids);
		//System.out.println("make flock of size : "+ numBoids);
		for(int c = 0; c < numBoids; ++c){
			boidFlock.add(c, new myBoid(p,this,randBoidStLoc(1), type, fv));
		//	boidFlock.get(c).setInitState();
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
	
	//adjust boid population by m
	public void modBoidPop(int m){
		if(m>0){if(boidFlock.size() >= p.maxBoats) {return;}for(int i=0;i<m;++i){ addBoid();}} 
		else { int n=-1*m; n = (n>numBoids-1 ? numBoids-1 : n);for(int i=0;i<n;++i){removeBoid();}}
		nearCount = (int) Math.max(Math.min(nearMinCnt,numBoids), numBoids*nearPct); 
	}//modBoidPop
	
	public myBoid addBoid(){	return addBoid(randBoidStLoc(1));	}
//	public myBoid addBoid(myPoint stLoc){
//		numBoids++;	
//		myBoid[] tmpList = new myBoid[numBoids];
//		for(int i=0;i<boidFlock.size();++i){tmpList[i]=boidFlock[i];}
//		tmpList[numBoids-1] =  new myBoid(p,this, stLoc, type, fv); 
//		//tmpList[numBoids-1].setInitState();
//		boidFlock = tmpList;
//		return tmpList[numBoids-1];
//	}//addBoid
	
	public myBoid addBoid(myPoint stLoc){
		myBoid tmp = new myBoid(p,this, stLoc, type, fv); 
		boidFlock.add(tmp);
		//tmpList[numBoids-1].setInitState();
		numBoids = boidFlock.size();
		return tmp;
	}//addBoid	
	
	public void removeBoid(){removeBoid(boidFlock.size()-1);}
//	public void removeBoid(int idx){
//		if(idx<0){return;}	
//		myBoid[] tmpList = new myBoid[numBoids-1];
//		for(int i=0;i<idx;++i){tmpList[i]=boidFlock[i];}
//		for(int i=idx+1;i<boidFlock.size();++i){tmpList[i-1]=boidFlock[i];}	
//		numBoids--;	
//		boidFlock = tmpList;
//	}//removeBoid
	public void removeBoid(int idx){
		if(idx<0){return;}	
		boidFlock.remove(idx);
		numBoids = boidFlock.size();
	}//removeBoid	
//	public void removeBoids(ArrayList<Integer> dead){
//		if(dead.size()<=0){return;}	ArrayList<myBoid> tmpList = new ArrayList<myBoid>();
//		for(int i=0; i<boidFlock.size(); ++i){if(!dead.contains(i)){tmpList.add(boidFlock[i]);}	}
//		boidFlock = tmpList.toArray(new myBoid[0]);	
//	}

	//move creatures to random start positions
	public void scatterBoids() {for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).coords[0] =  randBoidStLoc(1);}}//	randInit
	public void drawBoids(){
  		for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).drawMe(); } 
	}
//
//	public int[] getFwdSimVals(){
//		int[] retVal = new int[262144], res;
//		int i = 0;
//		for(int c = 0; c < boidFlock.size(); ++c){res = boidFlock.get(c).getSimData();for(int j = 0; j<res.length; ++j){retVal[i++] = res[j];}}		
//		return retVal;
//	}//getFwdSimVals
//	
//	public void updateBoidsFromGLSL(int[] data){
//		int offset = 6;//# of variables to set
//		for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).setGLSLData(data, c*offset);}
//	}
	
	//clear out last time step boid values
	public void clearOutBoids(){for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).clearAllBoidMaps();}	}					//clear out all neighbors, preds and prey	
	public void clearBoidForces(){for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).forces[0].set(0,0,0);}}	
	//initialize all maps for both neighbors and predator/prey
	public void initAllMaps(){
		callInitBoidCalcs.clear();
		//myBoid[] tmpList;
		for(int c = 0; c < boidFlock.size(); c+=mtFrameSize){
			int finalLen = (c+mtFrameSize < boidFlock.size() ? mtFrameSize : boidFlock.size() - c);
			//tmpList = new myBoid[finalLen];
			//System.arraycopy(boidFlock, c, tmpList, 0, finalLen);			
			//callInitBoidCalcs.add(new myInitPredPreyMaps(p, this, preyFlock, predFlock, fv, tmpList));
			callInitBoidCalcs.add(new myInitPredPreyMaps(p, this, preyFlock, predFlock, fv, boidFlock.subList(c, c+finalLen)));
		}							//find next turn's motion for every creature by finding total force to act on creature
		try {callInitFutures = p.th_exec.invokeAll(callInitBoidCalcs);for(Future<Boolean> f: callInitFutures) { f.get(); }} catch (Exception e) { e.printStackTrace(); }			
	}
	
	//update physics simulation
	public void moveBoidsMultTH(double _delT){
		delT = _delT; 
		callFwdBoidCalcs.clear();
		//myBoid[] tmpList;
		for(int c = 0; c < boidFlock.size(); c+=mtFrameSize){
			int finalLen = (c+mtFrameSize < boidFlock.size() ? mtFrameSize : boidFlock.size() - c);
			//tmpList = new myBoid[finalLen];
			//System.arraycopy(boidFlock, c, tmpList, 0, finalLen);			
			callFwdBoidCalcs.add(new myFwdStencil(p, this, preyFlock, predFlock, fv, boidFlock.subList(c, c+finalLen)));
		}							//find next turn's motion for every creature by finding total force to act on creature
		try {callFwdSimFutures = p.th_exec.invokeAll(callFwdBoidCalcs);for(Future<Boolean> f: callFwdSimFutures) { f.get(); }} catch (Exception e) { e.printStackTrace(); }		
	}
	public void updateBoidMovement(double _delT){
		delT = _delT; 
		callUbdBoidCalcs.clear();
		//myBoid[] tmpList;
		for(int c = 0; c < boidFlock.size(); c+=mtFrameSize){
			int finalLen = (c+mtFrameSize < boidFlock.size() ? mtFrameSize : boidFlock.size() - c);
			//tmpList = new myBoid[finalLen];
			//System.arraycopy(boidFlock, c, tmpList, 0, finalLen);			
			//p.th_exec.execute(new myUpdateStencil(p, this, boidFlock.get(c)));
			//List<myBoid> tmp = boidFlock.subList(c, c+finalLen);
			//System.out.println("size of tmp  :" + tmp.size() + " c : " + c + " boidFlockSize : " + boidFlock.size() + " final len : " + finalLen + " to ix : " + (c+finalLen));
			callUbdBoidCalcs.add(new myUpdateStencil(p, this, boidFlock.subList(c, c+finalLen)));
		}							//apply update
		try {callUpdFutures = p.th_exec.invokeAll(callUbdBoidCalcs);for(Future<Boolean> f: callUpdFutures) { f.get(); }} catch (Exception e) { e.printStackTrace(); }		
    	
    	//update pred counters
        myPoint[] bl = new myPoint[1];
        myVector[] bVelFrc  = new myVector[2];
        bl[0] = new myPoint(); bVelFrc[0]=new myVector(); bVelFrc[1]=new myVector();
        for(int c = 0; c < boidFlock.size(); ++c){
          if((boidFlock.get(c)!= null) && (boidFlock.get(c).bd_flags[myBoid.hasStarved])){    removeBoid(c);  }
          else {  
        	  //bl[0] = new myPoint(); bVelFrc[0]=new myVector(); bVelFrc[1]=new myVector(); 
        	  if(boidFlock.get(c).hadAChild(bl,bVelFrc)){myBoid tmpBby = this.addBoid(bl[0]); tmpBby.initNewborn(bVelFrc);}}
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
