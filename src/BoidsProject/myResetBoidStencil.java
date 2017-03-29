package BoidsProject;

import java.util.List;
import java.util.concurrent.Callable;

//reset all values at start of timestep
public class myResetBoidStencil implements Callable<Boolean> {
	public Project2 p;
	public List<myBoid> bAra;								//boid ara being worked on
	public myBoidFlock f, pry;

	public myResetBoidStencil(Project2 _p, myBoidFlock _f,myBoidFlock _pry, List<myBoid> _bAra) {
		p = _p;	f = _f; pry = _pry; bAra=_bAra;
		
	}
	public void run(){	
		for(myBoid b:bAra){	b.forces[0].set(myPointf.ZEROPT); b.clearNeighborMaps();	}
		if (p.flags[p.flkHunt] && (f!=pry)){	for(myBoid b:bAra){b.clearHuntMaps();			}}
		if (p.flags[p.flkSpawn]) {	for(myBoid b : bAra){	b.ptnWife.clear();}}	
	}//run()	
	
	@Override
	public Boolean call() throws Exception {
		run(); return true;
	}
}