package BoidsProject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import processing.core.PApplet;

//struct-type class to hold flocking variables
public class flkVrs {
	public Project2 p;	
	public int numFlocks;										//3 types of creatures	
	
	public myBoidFlock[] flocks;								//from p
	//weight multiplier for forces - centering, avoidance, velocity matching and wander
	
	private double neighborMult = .35f;							//multiplier for neighborhood consideration against zone size - all rads built off this
	
	public double[] dampConst = new double[]{.01f, .01f, .01f};					//multiplier for damping force, to slow boats down if nothing else acting on them
	
	public double[] nghbrRad,									//radius of the creatures considered to be neighbors
					colRad,										//radius of creatures to be considered for colision avoidance
					velRad,										//radius of creatures to be considered for velocity matching
					predRad,									//radius for creature to be considered for pred/prey
					spawnPct,									//% chance to reproduce given the boids breech the required radius
					spawnRad,									//distance to spawn * mass
					killPct,									//% chance to kill prey creature
					killRad;									//radius to kill * mass (distance required to make kill attempt)
	
	public int[] spawnFreq, 									//# of cycles that must pass before can spawn again
				eatFreq,								 		//# cycles w/out food until starve to death
				bodyColor = new int[]{Project2.gui_boatBody1, Project2.gui_boatBody2, Project2.gui_boatBody3  };	//idxs of body color for 3 types			
	
	double nghbrRadMax;							//max allowed neighborhood - min dim of cube
	public double totMaxRad;						//max search distance for neighbors
	public int nearCount;						//# of creatures required to have a neighborhood - some % of total # of creatures, or nearMinCnt, whichever is larger
	public final int nearMinCnt = 5;			//smallest neighborhood size allowed -> 5 or total # of creatures, whichever is smaller
	
	public double[][] massForType = new double[][]{{2,4},{1,3},{.5f,2}},
					maxFrcs,									//max forces for each flock, for each force type
					wts;										//weights for flock calculations for each flock
	//idx's of vars in wts arrays
	public final int wFrcCtr = 0,								//idx in wt array for multiplier of centering force
            		wFrcAvd = 1,								//idx in wt array for multiplier of col avoidance force
            		wFrcVel = 2,								//idx in wt array for multiplier of velocity matching force
            		wFrcWnd = 3,								//idx in wt array for multiplier of wandering force
            		wFrcAvdPred = 4,							//idx in wts array for predator avoidance
            		wFrcChsPrey = 5;							//idx in wts array for prey chasing
	
	public double[] maxVelMag = new double[]{180, 180, 180},		//max velocity for flock member
				minVelMag;										//min velocity for flock member

	public double[] defWtAra = new double[]{.5f, .75f, .5f, 1.5f, .5f, .1f},			//default array of weights for different forces
			MaxWtAra = new double[]{5, 5, 5, 5, 5, 5},								
			MinWtAra = new double[]{.01f, .01f, .01f, .01f, .001f, .001f},			
			MaxSpAra = new double[]{1,10000,100000},								
			MinSpAra = new double[]{.001f, 100, 100},			
			MaxHuntAra = new double[]{.1f,10000,100000},							
			MinHuntAra = new double[]{.00001f, 10, 100};		
	public double[] maxFrc = new double[]{200,200,200};
	
	public static final ArrayList<String> typeNames = new ArrayList<String>(Arrays.asList (new String[] {"Galley","Pirate", "Corsair"} ));	
	
	public flkVrs(Project2 _p, int numSpc) {
		p=_p;
		numFlocks = numSpc;
		initFlockVals();
	}
	
	public void initFlockVals(){
		nghbrRadMax = PApplet.min(p.gridDimDp, p.gridDimH, p.gridDimW)*neighborMult;
		nghbrRad = new double[]{nghbrRadMax, nghbrRadMax*.85f, nghbrRadMax*.6f};
		colRad  = new double[]{nghbrRad[0]*.1f, nghbrRad[1]*.1f, nghbrRad[2]*.1f}; 
		velRad  = new double[]{nghbrRad[0]*.5f, nghbrRad[1]*.5f, nghbrRad[2]*.5f}; 			
		//weight multiplier for forces - centering, avoidance, velocity matching and wander
		spawnPct = new double[]{.05f, .075f, .1f};		//% chance to reproduce given the boids breech the required radius
		spawnRad = new double[]{colRad[0],colRad[1],colRad[2]};			//distance to spawn 
		spawnFreq = new int[]{500,500,500}; 		//# of cycles that must pass before can spawn again
		//required meal time
		eatFreq = new int[]{2000,2000,2000}; 			//# cycles w/out food until starve to death
		killRad = new double[]{1,1,1};						//radius to kill * mass
		killPct = new double[]{.01f, .01f, .01f};				//% chance to kill prey creature
		//predator range for a flock
		predRad = new double[]{500,500,500};					//radius to avoid pred/find prey	
		//wts = new double[][]{{.5f, 1.0f, .5f, 3, .1f, .1f},{.5f, 1.0f, .5f, 3, .1f, .1f},{.5f, 1.0f, .5f, 3, .1f, .1f}};
		//% of total force allowed for each component
		wts = new double[3][];//{defWtAra, defWtAra,	defWtAra};
		for(int i=0;i<3;++i){double[] tmp = new double[defWtAra.length]; System.arraycopy( defWtAra, 0, tmp, 0, defWtAra.length );wts[i]=tmp;}
		maxFrcs = new double[][]{{100,200,100,10,400,20},{100,200,100,10,400,20},{100,200,100,10,400,20}};			//maybe scale forces
		minVelMag = new double[]{maxVelMag[0]*.025f, maxVelMag[1]*.025f, maxVelMag[2]*.025f};
	}
//	public void setBoatColor(int type, int[] sailColor){
//		ThreadLocalRandom.current().nextInt(75);
//		switch (type){
//		case Project2.galley :{sailColor[0] = 180 + ThreadLocalRandom.current().nextInt(75);sailColor[1] = 180 + ThreadLocalRandom.current().nextInt(75);	sailColor[2] = 180 + ThreadLocalRandom.current().nextInt(75);break;}
//		case Project2.pirate :{sailColor[0] = ThreadLocalRandom.current().nextInt(50); sailColor[1] = ThreadLocalRandom.current().nextInt(20);	sailColor[2] = ThreadLocalRandom.current().nextInt(20);break;}
//		case Project2.corsair :{sailColor[0] = 200 + ThreadLocalRandom.current().nextInt(50);sailColor[1] = 10 +ThreadLocalRandom.current().nextInt(20);	sailColor[2] = 20 + ThreadLocalRandom.current().nextInt(20);break;}
//		default :{sailColor[0] = 180 + ThreadLocalRandom.current().nextInt(75);sailColor[1] = 180 + ThreadLocalRandom.current().nextInt(75);	sailColor[2] = 180 + ThreadLocalRandom.current().nextInt(75);break;}
//		}
//	}//setBoatColor	
	public void setSailColor(int type, int[] sailColor){
		switch (type){
		case Project2.galley :{sailColor[0] = 200 + ThreadLocalRandom.current().nextInt(150);sailColor[1] = 200 + ThreadLocalRandom.current().nextInt(150);	sailColor[2] = 200 + ThreadLocalRandom.current().nextInt(150);sailColor[3] = 255;break;}
		case Project2.pirate :{
			int sClr = ThreadLocalRandom.current().nextInt(50) + 100;
			sailColor[0] = sClr; sailColor[1] = sClr;sailColor[2] = sClr;sailColor[3] = 255;break;}
		case Project2.corsair :{
			int sClrOff = 150 +ThreadLocalRandom.current().nextInt(20);
			sailColor[0] = 200 + ThreadLocalRandom.current().nextInt(50);sailColor[1] = sClrOff;	sailColor[2] = sClrOff;sailColor[3] = 255;break;}
		default :{sailColor[0] = 180 + ThreadLocalRandom.current().nextInt(75);sailColor[1] = 180 + ThreadLocalRandom.current().nextInt(75);	sailColor[2] = 180 + ThreadLocalRandom.current().nextInt(75);sailColor[3] = 255;break;}
		}
	}//setBoatColor	
	public double getInitMass(int type){return (massForType[type][0] + (massForType[type][1] - massForType[type][0])*Math.random());}
	
	//handles all modification of flock values from ui - wIdx is manufactured based on location in ui click area
	public void modFlkVal(int fIdx, int wIdx, double mod){
		//System.out.println("Attempt to modify flock : " + p.flkNames[fIdx] + " value : " + wIdx + " by " + mod);
		if((fIdx==-1)||(wIdx==-1)){return;}
		switch(wIdx){
		//hierarchy - if neighbor then col and vel, if col then 
			case 0  : {modVal(fIdx, nghbrRad, nghbrRadMax, .1f*nghbrRadMax, mod);fixNCVRads(fIdx, true, true);break;}			//flck radius
			case 1  : {modVal(fIdx, colRad, .9f*nghbrRad[fIdx], .05f*nghbrRad[fIdx], mod);fixNCVRads(fIdx, false, true);break;}	//avoid radius
			case 2  : {modVal(fIdx, velRad, .9f*nghbrRad[fIdx], colRad[fIdx], mod);break;}			//vel match radius
			
			case 3  : 						//3-9 are the 6 force weights
			case 4  : 
			case 5  : 
			case 6  : 
			case 7  : 
			case 8  : {modFlkWt(fIdx,wIdx-3,mod*.01f);break;}						//3-9 are the 6 force weights
			
			case 9  : {modVal(fIdx, spawnPct, MaxSpAra[0], MinSpAra[0], mod*.001f); break;}
			case 10 : {modVal(fIdx, spawnRad, MaxSpAra[1], MinSpAra[1], mod);break;}
			case 11 : {modVal(fIdx, spawnFreq, MaxSpAra[2], MinSpAra[2], (int)(mod*10));break;}
			case 12 : {modVal(fIdx, killPct, MaxHuntAra[0], MinHuntAra[0], mod*.0001f); break;}
			case 13 : {modVal(fIdx, predRad, MaxHuntAra[1], MinHuntAra[1], mod);break;}
			case 14 : {modVal(fIdx, eatFreq, MaxHuntAra[2], MinHuntAra[2], (int)(mod*10));break;}
			default : break;
		}//switch
		
	}//modFlckVal
	
	//call after neighborhood, collision or avoidance radii have been modified
	private void fixNCVRads(int fIdx, boolean modC, boolean modV){
		if(modC){colRad[fIdx] = Math.min(Math.max(colRad[fIdx],.05f*nghbrRad[fIdx]),.9f*nghbrRad[fIdx]);}//when neighbor rad modded	
		if(modV){velRad[fIdx] = Math.min(Math.max(colRad[fIdx],velRad[fIdx]),.9f*nghbrRad[fIdx]);}//when col or neighbor rad modded
	}
	
	private void modVal(int fIdx, int[] vals, double max, double min, int mod){	int oldVal = vals[fIdx];vals[fIdx] += mod;if(!(inRange(vals[fIdx], max, min))){vals[fIdx] = oldVal;}}	
	private void modVal(int fIdx, double[] vals, double max, double min, double mod){double oldVal = vals[fIdx];vals[fIdx] += mod;	if(!(inRange(vals[fIdx], max, min))){vals[fIdx] = oldVal;}}
	
	
	//modify a particular flock force weight for a particular flock
	private void modFlkWt(int fIdx, int wIdx, double mod){
		double oldVal = this.wts[fIdx][wIdx];
		this.wts[fIdx][wIdx] += mod;
		if(!(inRange(wts[fIdx][wIdx], MaxWtAra[wIdx], MinWtAra[wIdx]))){this.wts[fIdx][wIdx] = oldVal;}		
	}

	public boolean inRange(double val, double max, double min){return ((val<max)&&(val>min));}	
	public String[] getData(int s){
		String res[] = new String[8];
		int idx = 0;
		res[idx++] = flocks[s].numBoids + " " + typeNames.get(s) + "s limits: V: ["+String.format("%.2f", (minVelMag[s]))+"," + String.format("%.2f", (maxVelMag[s]))+"] M ["+ String.format("%.2f", (massForType[s][0])) + "," + String.format("%.2f", (massForType[s][1]))+"]" ;
		res[idx++] = "Radius : Flock |  Avoid  |  VelMatch ";
		res[idx++] = "           "+(nghbrRad[s] > 10 ?(nghbrRad[s] > 100 ? "":" "):"  ")+String.format("%.2f",nghbrRad[s])+" | "+(colRad[s] > 10 ?(colRad[s] > 100 ? "":" "):"  ")+String.format("%.2f",colRad[s])+" | "+(velRad[s] > 10 ?(velRad[s] > 100 ? "":" "):"  ")+ String.format("%.2f",velRad[s]);
		res[idx++] = "Wts: Ctr |  Avoid | VelM | Wndr | AvPrd | Chase" ;
		res[idx++] = "     "+String.format("%.2f", wts[s][wFrcCtr])
				+"  |  "+String.format("%.2f", wts[s][wFrcAvd])
				+"  |  "+String.format("%.2f", wts[s][wFrcVel])
				+"  |  "+String.format("%.2f", wts[s][wFrcWnd])
				+"  |  "+String.format("%.2f", wts[s][wFrcAvdPred])
				+"  |  "+String.format("%.2f", wts[s][wFrcChsPrey]);
		res[idx++] = "          		% success |  radius  |  # cycles.";
		res[idx++] = "Spawning : "+(spawnPct[s] > .1f ? "" : " ")+String.format("%.2f", (spawnPct[s]*100))+" | "+(spawnRad[s] > 10 ?(spawnRad[s] > 100 ? "":" "):"  ")+String.format("%.2f", spawnRad[s])+" | "+spawnFreq[s];
		res[idx++] = "Hunting   :  "+(killPct[s] > .1f ? "" : " ")+String.format("%.2f", (killPct[s]*100))+" | "+(predRad[s] > 10 ?(predRad[s] > 100 ? "":" "):"  ")+String.format("%.2f", predRad[s])+" | "+eatFreq[s];	
		return res;
	}
	
	public String toString(){
		String res = "Flock Vars for "+numFlocks+" species :\n";
		for(int f=0;f<this.numFlocks;++f){
			String[] flkStrs = getData(f);
			for(int s=0; s<flkStrs.length; ++s){res+=flkStrs[s]+"\n";}
			res+="\tFlock info :\n";
			res+="\t"+flocks[f];			
		}		
		return res;
	}
}
