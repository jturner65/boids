package BoidsProject;

import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;


public class myUpdateStencil implements Callable<Boolean> {
	public Project2 p;
	public myBoid b;
	public myBoid[] bAra;
	public myBoidFlock f;
	public final double rt2;
	public final int O_FWD, O_RHT,  O_UP;
	public final double epsValCalc, epsValCalcSq, spawnPct, killPct;
	
	myUpdateStencil(Project2 _p, myBoidFlock _f, myBoid[] _bAra){
		p=_p; f=_f; bAra=_bAra; 
		O_FWD = _bAra[0].O_FWD;
		O_RHT =  _bAra[0].O_RHT;  
		O_UP =  _bAra[0].O_UP;  
		rt2 = .5f*Math.sqrt(2); 
		epsValCalc = p.epsValCalc;
		epsValCalcSq = epsValCalc * epsValCalc;
		spawnPct = f.fv.spawnPct[f.type];
		killPct = f.fv.killPct[f.type];
	}	

	public void reproduce(){
		double chance;
		for(myBoid ptWife : b.ptnWife.values()){
			chance = ThreadLocalRandom.current().nextDouble();
			if(chance < spawnPct){
				b.haveChild(new myPoint(ptWife.coords[1],.5f,b.coords[1]), new myVector(ptWife.velocity[1],.5f,b.velocity[1]), new myVector(ptWife.forces[1],.5f,b.forces[1]));
				ptWife.hasSpawned();	
				b.hasSpawned();	return;
			}
		}
	}
	
	public double[] toAxisAngle() {
		double angle,x=rt2,y=rt2,z=rt2,s;
		if (((b.orientation[O_FWD].y-b.orientation[O_RHT].x)*(b.orientation[O_FWD].y-b.orientation[O_RHT].x) < epsValCalcSq)
		  && ((b.orientation[O_UP].x-b.orientation[O_FWD].z)*(b.orientation[O_UP].x-b.orientation[O_FWD].z) < epsValCalcSq)
		  && ((b.orientation[O_RHT].z-b.orientation[O_UP].y)*(b.orientation[O_RHT].z-b.orientation[O_UP].y) < epsValCalcSq)) {			//checking for rotational singularity
			// angle == 0
			if (((b.orientation[O_FWD].y+b.orientation[O_RHT].x) *(b.orientation[O_FWD].y+b.orientation[O_RHT].x) < 1) 
				&& ((b.orientation[O_FWD].z+b.orientation[O_UP].x)*(b.orientation[O_FWD].z+b.orientation[O_UP].x) < 1) 
				&& ((b.orientation[O_RHT].z+b.orientation[O_UP].y)*(b.orientation[O_RHT].z+b.orientation[O_UP].y) < 1)
			  && ((b.orientation[O_FWD].x+b.orientation[O_RHT].y+b.orientation[O_UP].z-3)*(b.orientation[O_FWD].x+b.orientation[O_RHT].y+b.orientation[O_UP].z-3) < 1)) {	return new double[]{0,1,0,0}; }
			// angle == pi
			angle = 3.1415927f;
			double fwd2x = (b.orientation[O_FWD].x+1)/2.0f,rht2y = (b.orientation[O_RHT].y+1)/2.0f,up2z = (b.orientation[O_UP].z+1)/2.0f,
				fwd2y = (b.orientation[O_FWD].y+b.orientation[O_RHT].x)/4.0f, fwd2z = (b.orientation[O_FWD].z+b.orientation[O_UP].x)/4.0f, rht2z = (b.orientation[O_RHT].z+b.orientation[O_UP].y)/4.0f;
			if ((fwd2x > rht2y) && (fwd2x > up2z)) { // b.orientation[O_FWD].x is the largest diagonal term
				if (fwd2x< p.epsValCalc) {	x = 0;} else {			x = Math.sqrt(fwd2x);y = fwd2y/x;z = fwd2z/x;}
			} else if (rht2y > up2z) { 		// b.orientation[O_RHT].y is the largest diagonal term
				if (rht2y< p.epsValCalc) {	y = 0;} else {			y = Math.sqrt(rht2y);x = fwd2y/y;z = rht2z/y;}
			} else { // b.orientation[O_UP].z is the largest diagonal term so base result on this
				if (up2z< p.epsValCalc) {	z = 0;} else {			z = Math.sqrt(up2z);	x = fwd2z/z;y = rht2z/z;}
			}
			return new double[]{angle,x,y,z}; // return 180 deg rotation
		}
		//no singularities - handle normally
		s = Math.sqrt((b.orientation[O_UP].y - b.orientation[O_RHT].z)*(b.orientation[O_UP].y - b.orientation[O_RHT].z)
						+(b.orientation[O_FWD].z - b.orientation[O_UP].x)*(b.orientation[O_FWD].z - b.orientation[O_UP].x)
						+(b.orientation[O_RHT].x - b.orientation[O_FWD].y)*(b.orientation[O_RHT].x - b.orientation[O_FWD].y)); // used to normalise
		if (s < p.epsValCalc){ s=1; }
			// prevent divide by zero, should not happen if matrix is orthogonal -- should be caught by singularity test above
		angle = -Math.acos(( b.orientation[O_FWD].x + b.orientation[O_RHT].y + b.orientation[O_UP].z - 1)/2);
		x = (b.orientation[O_UP].y - b.orientation[O_RHT].z)/s;
		y = (b.orientation[O_FWD].z - b.orientation[O_UP].x)/s;
		z = (b.orientation[O_RHT].x - b.orientation[O_FWD].y)/s;
	   return new double[]{angle,x,y,z};
	}//toAxisAngle
	
	public myVector getFwdVec(){
		if(b.velocity[0].magn==0){			return b.orientation[O_FWD]._normalize();		}
		else {		
			myVector tmp = b.velocity[0].cloneMe();			
			tmp._normalize();return new myVector(b.orientation[O_FWD], f.delT, tmp);		
		}
	}
	
	public myVector getUpVec(){	
		double fwdUpDotm1 = b.orientation[O_FWD]._dot(myVector.UP) -1;
		if (fwdUpDotm1 * fwdUpDotm1 < epsValCalcSq){
			return myVector._cross(b.orientation[O_RHT], b.orientation[O_FWD]);
		}
		return myVector.UP.cloneMe();
	}	
	
	public void setOrientation(){
		//find new orientation at new coords - creature is oriented in local axes as forward being positive z and up being positive y vectors correspond to columns, x/y/z elements correspond to rows
		b.orientation[O_FWD].set(getFwdVec());
		b.orientation[O_UP].set(getUpVec());	
		b.orientation[O_RHT] = b.orientation[O_UP]._cross(b.orientation[O_FWD]); //sideways is cross of up and forward - backwards(righthanded)
		b.orientation[O_RHT].set(b.orientation[O_RHT]._normalize());
		//need to recalc up?  may not be perp to normal
		if(Math.abs(b.orientation[O_FWD]._dot(b.orientation[O_UP])) > p.epsValCalc){
			b.orientation[O_UP] = b.orientation[O_FWD]._cross(b.orientation[O_RHT]); //sideways is cross of up and forward
			b.orientation[O_UP].set(b.orientation[O_UP]._normalize());
		}
		b.O_axisAngle = toAxisAngle();
	}

	//check kill chance, remove boid if succeeds
	public void hunt(){
		double chance;
		if(b.starveCntr<=0){return;}
		for(myBoid dinner : b.preyFlk.values()){
			chance = ThreadLocalRandom.current().nextDouble();
			if((chance < killPct)&&(dinner.starveCntr>0)){b.eat(dinner.mass);dinner.starveCntr=0;return;}//kill him
		}
	}//kill
	
	public void run(){	
		for(int i=0;i<bAra.length;++i){
			b=bAra[i];
			if(b==null){continue;}
			//move to use-now idx (0) - these are the values that are used by sim.
			b.forces[0].set(b.forces[1]);
			b.velocity[0].set(b.velocity[1]);
			b.coords[0].set(b.coords[1]);		
			reproduce();
			setOrientation();
			//b.O_axisAngle = toAxisAngle(b.orientation);
			b.updateBoidCountersMT();	
			hunt();
		}
	}

	@Override
	public Boolean call() throws Exception {
		run();
		return true;
	}
}
