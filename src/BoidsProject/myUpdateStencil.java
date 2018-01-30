package BoidsProject;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;


public class myUpdateStencil implements Callable<Boolean> {
	private Project2 p;
	private List<myBoid> bAra;
	private myBoidFlock f;
	private final float rt2;
	private final int O_FWD, O_RHT,  O_UP;
	private final float epsValCalc, epsValCalcSq, spawnPct, killPct;
	
	myUpdateStencil(Project2 _p, myBoidFlock _f, List<myBoid> _bAra){
		p=_p; f=_f; bAra=_bAra;
		myBoid tmp = bAra.get(0);
		O_FWD = tmp.O_FWD;
		O_RHT = tmp.O_RHT;  
		O_UP = tmp.O_UP;  
		rt2 = .5f*p.fsqrt2; 
		epsValCalc = p.epsValCalc;
		epsValCalcSq = epsValCalc * epsValCalc;
		spawnPct = f.fv.spawnPct[f.type];
		killPct = f.fv.killPct[f.type];
	}	

	public void reproduce(myBoid b){
		float chance;
		for(myBoid ptWife : b.ptnWife.values()){
			chance = ThreadLocalRandom.current().nextFloat();
			if(chance < spawnPct){
				b.haveChild(new myPointf(ptWife.coords,.5f,b.coords), new myVectorf(ptWife.velocity[0],.5f,b.velocity[0]), new myVectorf(ptWife.forces[0],.5f,b.forces[0]));
				ptWife.hasSpawned();	
				b.hasSpawned();	return;
			}
		}
	}
	
	private float[] toAxisAngle(myBoid b) {
		float angle,x=rt2,y=rt2,z=rt2,s;
		if (((b.orientation[O_FWD].y-b.orientation[O_RHT].x)*(b.orientation[O_FWD].y-b.orientation[O_RHT].x) < epsValCalcSq)
		  && ((b.orientation[O_UP].x-b.orientation[O_FWD].z)*(b.orientation[O_UP].x-b.orientation[O_FWD].z) < epsValCalcSq)
		  && ((b.orientation[O_RHT].z-b.orientation[O_UP].y)*(b.orientation[O_RHT].z-b.orientation[O_UP].y) < epsValCalcSq)) {			//checking for rotational singularity
			// angle == 0
			if (((b.orientation[O_FWD].y+b.orientation[O_RHT].x) *(b.orientation[O_FWD].y+b.orientation[O_RHT].x) < 1) 
				&& ((b.orientation[O_FWD].z+b.orientation[O_UP].x)*(b.orientation[O_FWD].z+b.orientation[O_UP].x) < 1) 
				&& ((b.orientation[O_RHT].z+b.orientation[O_UP].y)*(b.orientation[O_RHT].z+b.orientation[O_UP].y) < 1)
			  && ((b.orientation[O_FWD].x+b.orientation[O_RHT].y+b.orientation[O_UP].z-3)*(b.orientation[O_FWD].x+b.orientation[O_RHT].y+b.orientation[O_UP].z-3) < 1)) {	return new float[]{0,1,0,0}; }
			// angle == pi
			angle = p.PI;
			float fwd2x = (b.orientation[O_FWD].x+1)/2.0f,rht2y = (b.orientation[O_RHT].y+1)/2.0f,up2z = (b.orientation[O_UP].z+1)/2.0f,
				fwd2y = (b.orientation[O_FWD].y+b.orientation[O_RHT].x)/4.0f, fwd2z = (b.orientation[O_FWD].z+b.orientation[O_UP].x)/4.0f, rht2z = (b.orientation[O_RHT].z+b.orientation[O_UP].y)/4.0f;
			if ((fwd2x > rht2y) && (fwd2x > up2z)) { // b.orientation[O_FWD].x is the largest diagonal term
				if (fwd2x< p.epsValCalc) {	x = 0;} else {			x = (float) Math.sqrt(fwd2x);y = fwd2y/x;z = fwd2z/x;} 
			} else if (rht2y > up2z) { 		// b.orientation[O_RHT].y is the largest diagonal term
				if (rht2y< p.epsValCalc) {	y = 0;} else {			y = (float) Math.sqrt(rht2y);x = fwd2y/y;z = rht2z/y;}
			} else { // b.orientation[O_UP].z is the largest diagonal term so base result on this
				if (up2z< p.epsValCalc) {	z = 0;} else {			z = (float) Math.sqrt(up2z);	x = fwd2z/z;y = rht2z/z;}
			}
			return new float[]{angle,x,y,z}; // return 180 deg rotation
		}
		//no singularities - handle normally
		myVectorf tmp = new myVectorf((b.orientation[O_UP].y - b.orientation[O_RHT].z), (b.orientation[O_FWD].z - b.orientation[O_UP].x), (b.orientation[O_RHT].x - b.orientation[O_FWD].y));
		s = tmp.magn;
		//		s = Math.sqrt((b.orientation[O_UP].y - b.orientation[O_RHT].z)*(b.orientation[O_UP].y - b.orientation[O_RHT].z)
//						+(b.orientation[O_FWD].z - b.orientation[O_UP].x)*(b.orientation[O_FWD].z - b.orientation[O_UP].x)
//						+(b.orientation[O_RHT].x - b.orientation[O_FWD].y)*(b.orientation[O_RHT].x - b.orientation[O_FWD].y)); // used to normalise
		if (s < p.epsValCalc){ s=1; }
		tmp._scale(s);
			// prevent divide by zero, should not happen if matrix is orthogonal -- should be caught by singularity test above
		angle = (float) -Math.acos(( b.orientation[O_FWD].x + b.orientation[O_RHT].y + b.orientation[O_UP].z - 1)/2);
//		x = (b.orientation[O_UP].y - b.orientation[O_RHT].z)/s;
//		y = (b.orientation[O_FWD].z - b.orientation[O_UP].x)/s;
//		z = (b.orientation[O_RHT].x - b.orientation[O_FWD].y)/s;
//	   return new float[]{angle,x,y,z};
	   return new float[]{angle,tmp.x,tmp.y,tmp.z};
	}//toAxisAngle
	
	private myVectorf getFwdVec(myBoid b){
		if(b.velocity[0].magn==0){			return b.orientation[O_FWD]._normalize();		}
		else {		
			myVectorf tmp = b.velocity[0].cloneMe();			
			tmp._normalize();return new myVectorf(b.orientation[O_FWD], p.delT, tmp);		
		}
	}
	
	private myVectorf getUpVec(myBoid b){	
		float fwdUpDotm1 = b.orientation[O_FWD]._dot(myVectorf.UP) -1;
		if (fwdUpDotm1 * fwdUpDotm1 < epsValCalcSq){
			return myVectorf._cross(b.orientation[O_RHT], b.orientation[O_FWD]);
		}
		return myVectorf.UP.cloneMe();
	}	
	
	public void setOrientation(myBoid b){
		//find new orientation at new coords - creature is oriented in local axes as forward being positive z and up being positive y vectors correspond to columns, x/y/z elements correspond to rows
		b.orientation[O_FWD].set(getFwdVec(b));
		b.orientation[O_UP].set(getUpVec(b));	
		b.orientation[O_RHT] = b.orientation[O_UP]._cross(b.orientation[O_FWD]); //sideways is cross of up and forward - backwards(righthanded)
		b.orientation[O_RHT].set(b.orientation[O_RHT]._normalize());
		//need to recalc up?  may not be perp to normal
		if(Math.abs(b.orientation[O_FWD]._dot(b.orientation[O_UP])) > p.epsValCalc){
			b.orientation[O_UP] = b.orientation[O_FWD]._cross(b.orientation[O_RHT]); //sideways is cross of up and forward
			b.orientation[O_UP].set(b.orientation[O_UP]._normalize());
		}
		b.O_axisAngle = toAxisAngle(b);
	}

	//check kill chance, remove boid if succeeds
	public void hunt(myBoid b){
		float chance;
		for(myBoid dinner : b.preyFlk.values()){
			chance = ThreadLocalRandom.current().nextFloat();
			if(chance < killPct){b.eat(dinner.mass);dinner.killMe("Eaten by predator : "+b.ID);return;}//kill him next update by setting dead flag
		}
	}//kill
	
	public void run(){	
		for(myBoid b : bAra){
			if(b.bd_flags[myBoid.isDead]){
				//System.out.println("Dead boid in bAra in myUpdateStencil integrate : ID : " + b.ID);
				continue;
			}
			b.velocity[0].set(integrate(myVectorf._mult(b.forces[0], (1.0f/b.mass)), b.velocity[0]));			//myVectorf._add(velocity[0], myVectorf._mult(forces[1], p.delT/(1.0f * mass)));	divide by  mass, multiply by delta t
			if(b.velocity[0].magn > f.fv.maxVelMag[b.type]){b.velocity[0]._scale(f.fv.maxVelMag[b.type]);}
			if(b.velocity[0].magn < f.fv.minVelMag[b.type]){b.velocity[0]._scale(f.fv.minVelMag[b.type]);}
			b.coords.set(integrate(b.velocity[0], b.coords));												// myVectorf._add(coords[0], myVectorf._mult(velocity[1], p.delT));	
			setValWrapCoordsForDraw(b.coords);
			setOrientation(b);
			//b.setOrientation();
		}
		if (p.flags[p.flkSpawn]) {
			for(myBoid b : bAra){//check every boid to reproduce
				if(b.bd_flags[myBoid.isDead]){
					//System.out.println("Dead boid in bAra in myUpdateStencil reproduce : ID : " + b.ID);
					continue;
				}
				reproduce(b);
			}
			for(myBoid b : bAra){//update spa
				if(b.bd_flags[myBoid.isDead]){
					//System.out.println("Dead boid in bAra in myUpdateStencil update : ID : " + b.ID);
					continue;
				}			
				b.updateSpawnCntr();
			}
		}
		if (p.flags[p.flkHunt]) {//see if near enough to prey to eat it
			for(myBoid b : bAra){			
				if(b.bd_flags[myBoid.isDead]){
					//System.out.println("Dead boid in bAra in myUpdateStencil hunt : ID : " + b.ID);
					continue;
				}
				hunt(b);
			}
		}
		if (p.flags[p.flkHunger]){
			for(myBoid b : bAra){
				if(b.bd_flags[myBoid.isDead]){
					//System.out.println("Dead boid in bAra in myUpdateStencil updHunger : ID : " + b.ID);
					continue;
				}			
				b.updateHungerCntr();
			}			
		}
	}
	
	//integrator
	public myPointf integrate(myVectorf stateDot, myPointf state){		return myPointf._add(state, myVectorf._mult(stateDot, p.delT));}
	public myVectorf integrate(myVectorf stateDot, myVectorf state){	return myVectorf._add(state, myVectorf._mult(stateDot, p.delT));}
	
	public void setValWrapCoordsForDraw(myPointf _coords){_coords.set(((_coords.x+p.gridDimX) % p.gridDimX),((_coords.y+p.gridDimY) % p.gridDimY),((_coords.z+p.gridDimZ) % p.gridDimZ));	}//findValidWrapCoords	

	@Override
	public Boolean call() throws Exception {
		run();
		return true;
	}
}
