package BoidsProject;

import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ThreadLocalRandom;
import java.lang.Double;

import processing.core.PConstants;

/**
 * class defining a creature object for flocking
 * @author John
 */
public class myBoid {
	public Project2 p;
	public myBoidFlock f;
	public flkVrs fv;
	
	//public myStencil st;														//performs calculations
	public int ID;
	public static int IDcount = 0;
	
	public int starveCntr, spawnCntr;
	public double[] O_axisAngle,															//axis angle orientation of this boid
				baby_O_axisAngle;													//axis angle orientation of this boid's spawn
	public double mass,oldRotAngle;	
	public final double sizeMult = .15f;
	public final myVector scMult = new myVector(.5f,.5f,.5f);				//multiplier for scale based on mass
	public myVector scaleBt,rotVec, birthVel, birthForce;						//scale of boat - reflects mass, rotational vector, vel and force applied at birth - hit the ground running
	public myPoint coords;													//com coords
	public myVector[] velocity,													//velocity history
					  forces;												//force accumulator
	public myVector[] orientation;												//Rot matrix - 3x3 orthonormal basis matrix - cols are bases for body frame orientation in world frame
	
	public boolean[] bd_flags;	
	public final static int canSpawn 		= 0,							//whether enough time has passed that this boid can spawn
						 	isDead			= 1,							//whether this boid is dead
						 	isHungry		= 2,							//whether this boid is hungry
						 	hadChild		= 3,							//had a child this cycle, needs to "deliver"
						 	numbd_flags 	= 4;
	
	//location to put new child
	public myPoint birthLoc;
	//animation controlling variables
	public double animCntr;
	public static final double maxAnimCntr = 1000.0f, baseAnimSpd = 1.0f;
	public final double preCalcAnimSpd;
	//boat construction variables
	//public int[] sailColor;
	public final int type,gender,bodyColor;													//for spawning gender = 0 == female, 1 == male;
	public final int O_FWD = 0, O_RHT = 1,  O_UP = 2;
		
	public ConcurrentSkipListMap<Double, myBoid> neighbors,			//sorted map of neighbors to this boid
									colliders,			//sorted map of colliding neighbors to this boid - built when neighbors are built
									predFlk,				//sorted map of predators near this boid
									preyFlk,				//sorted map of prey near this boid
									ptnWife;				//sorted map of potential mates near this boid
	
	public ConcurrentSkipListMap<Integer, myPoint> neighLoc,			//boid mapped to location used for distance calc
										colliderLoc,		//boid mapped to location used for distance calc
										predFlkLoc,		//boid mapped to location used for distance calc
										preyFlkLoc,		//boid mapped to location used for distance calc
										ptnWifeLoc;				//sorted map of potential mates near this boid
		
	public myBoid(Project2 _p, myBoidFlock _f,  myPoint _coords, int _type, flkVrs _fv){
		ID = IDcount++;		p = _p;		f = _f; 
		//st = _st; 
		fv=_fv;
		initbd_flags();
		rotVec = myVector.RIGHT.cloneMe(); 			//initial setup
		orientation = new myVector[3];
		orientation[O_RHT] = myVector.RIGHT.cloneMe();
		orientation[O_UP] = myVector.UP.cloneMe();
		orientation[O_FWD] = myVector.FORWARD.cloneMe();
		preCalcAnimSpd = ThreadLocalRandom.current().nextDouble(.5f,2*p.cycleModDraw);		
		animCntr = ThreadLocalRandom.current().nextDouble(.000001f ,maxAnimCntr );
		coords = new myPoint(_coords);	//new myPoint[2]; 
		velocity = new myVector[2];
		//oldForce = new myVector(0,0,0);
		forces = new myVector[2];
		type=_type;
		for (int i = 0; i < 2 ; ++i){
			//coords[i] = new myPoint(_coords);											//coords of body in world space
			velocity[i] = myVector.ZEROVEC.cloneMe();
			forces[i] = myVector.ZEROVEC.cloneMe(); 
		}

		setInitState();
		O_axisAngle=new double[]{0,1,0,0};
		oldRotAngle = 0;
		gender = ThreadLocalRandom.current().nextInt(1000)%2;												//0 or 1
		neighbors 	= new ConcurrentSkipListMap<Double, myBoid>();
		colliders 	= new ConcurrentSkipListMap<Double, myBoid>();
		predFlk 	= new ConcurrentSkipListMap<Double, myBoid>();
		preyFlk 	= new ConcurrentSkipListMap<Double, myBoid>();
		ptnWife 	= new ConcurrentSkipListMap<Double, myBoid>();
		
		neighLoc 	= new ConcurrentSkipListMap<Integer, myPoint>();
		colliderLoc = new ConcurrentSkipListMap<Integer, myPoint>();
		predFlkLoc	= new ConcurrentSkipListMap<Integer, myPoint>();
		preyFlkLoc	= new ConcurrentSkipListMap<Integer, myPoint>();
		ptnWifeLoc	= new ConcurrentSkipListMap<Integer, myPoint>();
		
		bodyColor = fv.bodyColor[type];

	}//constructor
	
	public void setInitState(){
		mass=fv.getInitMass(type);
		scaleBt = new myVector(scMult);					//for rendering different sized boids
		scaleBt._mult(mass);		
		starveCntr = resetCntrs(fv.eatFreq[type],ThreadLocalRandom.current().nextDouble(mass ));
		spawnCntr = 0;		
		bd_flags[canSpawn] = true;
	}
	
	public void clearAllBoidMaps(){	
		neighbors.clear(); colliders.clear(); predFlk.clear();	preyFlk.clear();ptnWife.clear();
		neighLoc.clear(); colliderLoc.clear();	//predFlkLoc.clear();	
		preyFlkLoc.clear();ptnWifeLoc.clear();
	}
	
	public void copySubSetBoidsCol(Double colRadSq){		
		colliders.putAll(neighbors.subMap(0.0, colRadSq));
		for(myBoid b : colliders.values()){colliderLoc.put(b.ID, neighLoc.get(b.ID));}		
	}
	public void copySubSetBoidsMate(Double spawnRadSq){
		if((!bd_flags[canSpawn]) || (gender==0)){return;}//need "males" who can mate
		for(Double dist : neighbors.keySet()){
			if (dist > spawnRadSq){return;}//returns in increasing order - can return once we're past spawn Rad Threshold
			myBoid b = neighbors.get(dist);
			if((b.gender==0)&&(b.canSpawn())){				
				ptnWife.put(dist, b);
				ptnWifeLoc.put(b.ID, neighLoc.get(b.ID));
			}
		}
//		TreeMap<Double, myBoid> ptnMate 	= new TreeMap<Double, myBoid>();
//		ptnMate.putAll(neighbors.subMap(0.0, spawnRadSq));
//		for(Double dist : ptnMate.keySet()){
//			myBoid b = ptnMate.get(dist);
//			if((b.gender==0)&&(b.canSpawn())){				
//				ptnWife.put(dist, b);
//				ptnWifeLoc.put(b.ID, neighLoc.get(b.ID));
//			}
//		}
	}//copySubSetBoidsMate
	public void haveChild(myPoint _bl, myVector _bVel, myVector _bFrc){bd_flags[hadChild]=true; birthLoc=_bl;birthVel=_bVel;birthForce=_bFrc;}
	public boolean hadAChild(myPoint[] _bl, myVector[] _bVelFrc){if(bd_flags[hadChild]){bd_flags[hadChild]=false;_bl[0].set(birthLoc);_bVelFrc[0].set(birthVel);_bVelFrc[1].set(birthForce);return true;} else {return false;}}	
	public int resetCntrs(int cntrBseVal, double mod){return (int)(cntrBseVal*(1+mod));}
	
	public void hasSpawned(){spawnCntr = resetCntrs(fv.spawnFreq[type],ThreadLocalRandom.current().nextDouble()); bd_flags[canSpawn] = false;}
	public boolean canSpawn(){return bd_flags[canSpawn];}
	public void eat(double tarMass){	starveCntr = resetCntrs(fv.eatFreq[type],tarMass);bd_flags[isHungry]=false;}
	public boolean canSprint(){return (starveCntr > .25f*fv.eatFreq[type]);}
	public boolean isHungry(){return bd_flags[isHungry];}
	//init bd_flags state machine
	public void initbd_flags(){bd_flags = new boolean[numbd_flags];for(int i=0;i<numbd_flags;++i){bd_flags[i]=false;}}

	
	//update all counters that determine state of boid
	public void updateBoidCountersMT(){
		starveCntr--;
		if (starveCntr<=0){killMe("Starvation");}
		spawnCntr--;
		bd_flags[canSpawn]=(spawnCntr<=0);
		bd_flags[isHungry] = (bd_flags[isHungry] || (p.random(fv.eatFreq[type])>=starveCntr)); //once he's hungry he stays hungry unless he eats (hungry set to false elsewhere)
	}//updateBoidCounters	
	
	//initialize newborn velocity, forces, and orientation
	public void initNewborn(myVector[] bVelFrc){
		this.velocity[0].set(bVelFrc[0]); this.velocity[1].set(bVelFrc[0]); 
		this.forces[0].set(bVelFrc[1]);this.forces[1].set(bVelFrc[1]); 
		//setOrientation();
	}
	
	public void setOrientation(){
		//find new orientation at new coords - creature is oriented in local axes as forward being positive z and up being positive y vectors correspond to columns, x/y/z elements correspond to rows
		orientation[O_FWD].set(getFwdVec());
		orientation[O_UP].set(getUpVec());	
		orientation[O_RHT] = orientation[O_UP]._cross(orientation[O_FWD]); //sideways is cross of up and forward - backwards(righthanded)
		orientation[O_RHT].set(orientation[O_RHT]._normalize());
		//need to recalc up?  may not be perp to normal
		if(Math.abs(orientation[O_FWD]._dot(orientation[O_UP])) > p.epsValCalc){
			orientation[O_UP] = orientation[O_FWD]._cross(orientation[O_RHT]); //sideways is cross of up and forward
			orientation[O_UP].set(orientation[O_UP]._normalize());
		}
		O_axisAngle = toAxisAngle();
	}
	
	private myVector getFwdVec(){				
		if( velocity[0].magn==0){			return orientation[O_FWD]._normalize();		}
		else {		
			myVector tmp = velocity[0].cloneMe();	
			tmp._normalize();return new myVector(orientation[O_FWD], f.delT, tmp);		
		}
	}
	
	private myVector getUpVec(){		
		if (Math.abs(orientation[O_FWD]._dot(myVector.UP) -1)< p.epsValCalc){
			return myVector._cross(orientation[O_RHT], orientation[O_FWD]);
		}
		return myVector.UP.cloneMe();
	}

	//align the boid along the current orientation matrix
	private void alignBoid(){
		//double res[] = f.toAxisAngle(orientation, O_FWD, O_RHT, O_UP);
		rotVec.set(O_axisAngle[1],O_axisAngle[2],O_axisAngle[3]);
		double rotAngle = oldRotAngle + ((O_axisAngle[0]-oldRotAngle) *f.delT);
		p.rotate(rotAngle,O_axisAngle[1],O_axisAngle[2],O_axisAngle[3]);
		oldRotAngle = rotAngle;
	}//alignBoid
	//kill this boid
	public void killMe(String cause){
		if(p.flags[p.debugMode]){System.out.println("Boid : " +ID+" killed : " + cause);}
		bd_flags[isDead]=true;
	}
	
	//draw this body on mesh
	public void drawMe(){
		p.pushMatrix();p.pushStyle();
			p.strokeWeight(1.0f/(float)mass);
			//p.translate(coords.x,coords.y,coords.z);		//move to location
			p.translate(coords.x,coords.y,coords.z);		//move to location
			if(p.flags[p.debugMode]){drawMyVec(rotVec, Project2.gui_Black,4.0f);p.drawAxes(100, 2.0f, new myPoint(0,0,0), orientation, 255);}
			if(p.flags[p.showVelocity]){drawMyVec(velocity[0], Project2.gui_DarkMagenta,.5f);}
			alignBoid();
			p.rotate(PConstants.PI/2.0f,1,0,0);
			p.rotate(PConstants.PI/2.0f,0,1,0);
			p.scale(scaleBt.x,scaleBt.y,scaleBt.z);																	//make appropriate size				
			p.pushStyle();
			f.tmpl.drawMe(animCntr,bodyColor, type);
			p.popStyle();			
		p.popStyle();p.popMatrix();
		animIncr();
	}//drawme 
	
	//draw this boid as a ball
	public void drawMeDebug(){
		p.pushMatrix();p.pushStyle();
			p.strokeWeight(1.0f/(float)mass);
			p.translate(coords.x,coords.y,coords.z);		//move to location
			if(p.flags[p.debugMode]){drawMyVec(rotVec, Project2.gui_Black,4.0f);p.drawAxes(100, 2.0f, new myPoint(0,0,0), orientation, 255);}
			if(p.flags[p.showVelocity]){drawMyVec(velocity[0], Project2.gui_DarkMagenta,.5f);}
			p.setColorValFill(p.gui_boatBody1 + type);
			p.noStroke();
			p.sphere(5);
		p.popStyle();p.popMatrix();
		animIncr();
	}//drawme 
	
	
	//public double calcBobbing(){		return 2*(p.cos(.01f*animCntr));	}		//bobbing motion
	
	public void drawMyVec(myVector v, int clr, float sw){
		p.pushMatrix();
			p.pushStyle();
			p.setColorValStroke(clr);
			p.strokeWeight(sw);
			//myVector tmpv = myVector._mult(v, 1);
			myPoint tmp =  new myPoint(new myPoint(0,0,0),v);
			p.line(new myPoint(0,0,0),tmp);
			p.popStyle();
		p.popMatrix();		
	}
	
	private void animIncr(){
		//use animCntr to control animation
		//animCntr+=(baseAnimSpd + (velocity[0].magn*.1f))*preCalcAnimSpd;						//set animMod based on velocity -> 1 + mag of velocity		
		animCntr = (animCntr + (baseAnimSpd + (velocity[0].magn *.1f)) * preCalcAnimSpd) % maxAnimCntr;						//set animMod based on velocity -> 1 + mag of velocity		
//		if((animCntr>maxAnimCntr)||(animCntr<0)){
//			animCntr =0;
//		}
	}//animIncr		

	public double[] toAxisAngle() {
		double angle, rt2 = .5f*Math.sqrt(2),x=rt2,y=rt2,z=rt2,s;
		if ((Math.abs(orientation[O_FWD].y-orientation[O_RHT].x) < p.epsValCalc)
		  && (Math.abs(orientation[O_UP].x-orientation[O_FWD].z) < p.epsValCalc)
		  && (Math.abs(orientation[O_RHT].z-orientation[O_UP].y) < p.epsValCalc)) {			//checking for rotational singularity
			// angle == 0
			if ((Math.abs(orientation[O_FWD].y+orientation[O_RHT].x) < 1) && (Math.abs(orientation[O_FWD].z+orientation[O_UP].x) < 1) && (Math.abs(orientation[O_RHT].z+orientation[O_UP].y) < 1)
			  && (Math.abs(orientation[O_FWD].x+orientation[O_RHT].y+orientation[O_UP].z-3) < 1)) {	return new double[]{0,1,0,0}; }
			// angle == pi
			angle = PConstants.PI;
			double fwd2x = (orientation[O_FWD].x+1)/2.0f,rht2y = (orientation[O_RHT].y+1)/2.0f,up2z = (orientation[O_UP].z+1)/2.0f,
				fwd2y = (orientation[O_FWD].y+orientation[O_RHT].x)/4.0f, fwd2z = (orientation[O_FWD].z+orientation[O_UP].x)/4.0f, rht2z = (orientation[O_RHT].z+orientation[O_UP].y)/4.0f;
			if ((fwd2x > rht2y) && (fwd2x > up2z)) { // orientation[O_FWD].x is the largest diagonal term
				if (fwd2x< p.epsValCalc) {	x = 0;} else {			x = Math.sqrt(fwd2x);y = fwd2y/x;z = fwd2z/x;}
			} else if (rht2y > up2z) { 		// orientation[O_RHT].y is the largest diagonal term
				if (rht2y< p.epsValCalc) {	y = 0;} else {			y = Math.sqrt(rht2y);x = fwd2y/y;z = rht2z/y;}
			} else { // orientation[O_UP].z is the largest diagonal term so base result on this
				if (up2z< p.epsValCalc) {	z = 0;} else {			z = Math.sqrt(up2z);	x = fwd2z/z;y = rht2z/z;}
			}
			return new double[]{angle,x,y,z}; // return 180 deg rotation
		}
		//no singularities - handle normally
		s = Math.sqrt((orientation[O_UP].y - orientation[O_RHT].z)*(orientation[O_UP].y - orientation[O_RHT].z)
						+(orientation[O_FWD].z - orientation[O_UP].x)*(orientation[O_FWD].z - orientation[O_UP].x)
						+(orientation[O_RHT].x - orientation[O_FWD].y)*(orientation[O_RHT].x - orientation[O_FWD].y)); // used to normalise
		if (Math.abs(s) < p.epsValCalc){ s=1; }
			// prevent divide by zero, should not happen if matrix is orthogonal -- should be caught by singularity test above
		angle = -Math.acos(( orientation[O_FWD].x + orientation[O_RHT].y + orientation[O_UP].z - 1)/2);
		x = (orientation[O_UP].y - orientation[O_RHT].z)/s;
		y = (orientation[O_FWD].z - orientation[O_UP].x)/s;
		z = (orientation[O_RHT].x - orientation[O_FWD].y)/s;
	   return new double[]{angle,x,y,z};
	}//toAxisAngle
	
	public String toString(){
		String result = "ID : " + ID + " Type : "+p.flkNames[f.type]+" | Mass : " + mass + " | Spawn CD "+spawnCntr + " | Starve CD " + starveCntr+"\n";
		result+=" | location : " + coords + " | velocity : " + velocity[0] + " | forces : " + forces[0] +"\n" ;
		//if(p.flags[p.debugMode]){result +="\nOrientation : UP : "+orientation[O_UP] + " | FWD : "+orientation[O_FWD] + " | RIGHT : "+orientation[O_RHT] + "\n";}
		int num =neighbors.size();
		result += "# neighbors : "+ num + (num==0 ? "\n" : " | Neighbor IDs : \n");
		if(p.flags[p.showFlkMbrs]){	for(Double bd_K : neighbors.keySet()){result+="\tNeigh ID : "+neighbors.get(bd_K).ID + " dist from me : " + bd_K+"\n";}}
		num = colliders.size();
		result += "# too-close neighbors : "+ num + (num==0 ? "\n" : " | Colliders IDs : \n");
		if(p.flags[p.showFlkMbrs]){for(Double bd_K : colliders.keySet()){result+="\tFlck ID : "+colliders.get(bd_K).ID + " dist from me : " + bd_K+"\n";}}
		num = predFlk.size();
		result += "# predators : "+ num + (num==0 ? "\n" : " | Predator IDs : \n");
		if(p.flags[p.showFlkMbrs]){for(Double bd_K : predFlk.keySet()){result+="\tPred ID : "+predFlk.get(bd_K).ID + " dist from me : " + bd_K+"\n";}}
		num = preyFlk.size();
		result += "# prey : "+ num + (num==0 ? "\n" : " | Prey IDs : \n");
		if(p.flags[p.showFlkMbrs]){for(Double bd_K : preyFlk.keySet()){result+="\tPrey ID : "+preyFlk.get(bd_K).ID + " dist from me : " + bd_K+"\n";}}
		return result;
	}	
}//myBoid class
