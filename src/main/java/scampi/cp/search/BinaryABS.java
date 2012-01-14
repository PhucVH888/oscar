/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *  
 * Contributors:
 *      www.n-side.com
 ******************************************************************************/
package scampi.cp.search;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import scampi.cp.util.ArrayUtils;
import scampi.cp.util.IncrementalStatistics;

import scampi.cp.constraints.Diff;
import scampi.cp.constraints.Eq;
import scampi.cp.core.CPVarInt;

/**
 * Activity Based Search Heuristic
 * @author Pierre Schaus pschaus@gmail.com
 */
public class BinaryABS extends Nary {
	
	private double [] activity;
	private int [] domSize;
	private double decay = 0.999;
	private int [] randomVals;
	private IncrementalStatistics [] stats; // one stat for each variables
	
	public BinaryABS(CPVarInt ...x) {
		super(x);
		activity = new double[x.length];
		domSize = new int[x.length];
		
		for (int i = 0; i < x.length; i++) {
			activity[i] = 0;
			domSize[i] = x[i].getSize();
		}
		randomVals = new int[x.length];
	}
	
	
	@Override
	public void initialize() {
		if (ArrayUtils.getFirstNotBound(x) == -1) {
			return; // all variables are bound, not point to probe
		}
		stats = new IncrementalStatistics[x.length];
		for (int i = 0; i < x.length; i++) {
			stats[i] =  new IncrementalStatistics();
			domSize[i] = x[i].getSize();
		}
		for (int i = 0; i < 1000; i++) {
			probe();
			updateStatisticsAndReset();
		}
		for (int i = 0; i < x.length; i++) {
			activity[i] = stats[i].getAverage();
		}
		
		System.out.println(Arrays.toString(activity));
		/*
		for (int i = 0; i< 7; i++) {
			for (int j = 0; j < 7; j++) {
				System.out.print((int)(100*activity[i*7+j])+"\t");
			}System.out.println("");
		}
		*/
	}
	
	/**
	 * Value Heuristic of the ABS search
	 * @param var
	 * @return the value to instantiate
	 */
	public int getVal(CPVarInt var) {
		return var.getRandomValue();
	}

	private void updateStatisticsAndReset() {
		for (int i = 0; i < x.length; i++) {
			stats[i].addPoint(activity[i]);
			activity[i] =  0;
			domSize[i] = x[i].getSize();
		}
	}
	
	private void probe() {
		s.pushState();
		int i = ArrayUtils.getRandomNotBound(x);
		while (!s.isFailed() &&  i != -1) {
			CPVarInt var = x[i];
			s.post(new Eq(var, getVal(var)));
			if (!s.isFailed()) {
				updateActivity();
			}
			i = ArrayUtils.getRandomNotBound(x);
		}
		s.pop();
	}
	

	private void updateActivity() {
		for (int i = 0; i < x.length; i++) {
			if (x[i].getSize() < domSize[i]) {
				activity[i] += 1;
			}
			domSize[i] = x[i].getSize();
		}
	}
	
	
	@Override
	public int getVar() {
		for (int i = 0; i < x.length; i++) {
			randomVals[i] =  s.getRandom().nextInt(x.length);
		}
		return ArrayUtils.getVarNotBound(x, new Comparator<Integer>() {
			public int compare(Integer o1, Integer o2) {
				double a = - activity[o1]/x[o1].getSize() + activity[o2]/x[o2].getSize();
				if (a < 0) {
					return -1;
				} else if (a > 0) {
					return 1;
				} else { // equality
					return randomVals[o1]-randomVals[o2];
				}
			}
			
		});
	}
		
	@Override
	public CPAlternative[] getAlternatives() {	
		for (int i = 0; i < x.length; i++) {
			if (x[i].getSize() < domSize[i]) {
				activity[i] += 1;
			} else if (!x[i].isBound()) {
				activity[i] *= decay;
			}
			domSize[i] = x[i].getSize();
		}
		int j = getVar();
		if (j == -1) return null;
		CPVarInt var = x[j];
		int val = getVal(var);
		CPAlternative left = new CPAlternative(s, new Eq(var, val));
		CPAlternative right = new CPAlternative(s, new Diff(var, val));
		return new CPAlternative[] {left,right};
	}
}
