/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *  
 * Contributors:
 *      www.n-side.com
 ******************************************************************************/
package scampi.cp.constraints;

import java.util.Arrays;
import java.util.Comparator;

import scampi.cp.util.ArrayUtils;


import scampi.cp.core.CPOutcome;
import scampi.cp.core.CPPropagStrength;
import scampi.cp.core.Constraint;
import scampi.cp.core.CPVarInt;
import scampi.reversible.ReversibleInt;

/**
 * Element Constraint on an array of constants
 * @author Pierre Schaus pschaus@gmail.com
 */
public class ElementCst extends Constraint {
	

	private final int [] y;
	private CPVarInt x;
	private CPVarInt z;
	private Integer [] sortedPerm; //y[sortedPerm[0]] is the min element of y... y[sortedPerm[y.length]] is the largest element of y 
	private ReversibleInt minIndSupp;
	private ReversibleInt maxIndSupp;


    /**
     * @param y
     * @param x
     * @param z linked with y and x by the relation y[x] == z
     * @see  Element#get(int[], cp.core.CPVarInt)
     */
	public ElementCst(final int [] y, CPVarInt x, CPVarInt z) {
		super(x.getStore(),"ElementCst");
		this.y = y;
		this.x = x;
		this.z = z;
		
		sortedPerm = new Integer [y.length];
		for (int i = 0; i < y.length; i++) {
			sortedPerm[i] = i;	
		}
		Arrays.sort(sortedPerm, new Comparator<Integer>(){
	
			public int compare(Integer i1, Integer i2) {
				return (y[i1]-y[i2]);
			}});
		minIndSupp = new ReversibleInt(s);
		minIndSupp.setValue(0);
		maxIndSupp = new ReversibleInt(s);
		maxIndSupp.setValue(y.length-1);
	}

	@Override
	protected CPOutcome setup(CPPropagStrength l) {

		if (x.updateMin(0) == CPOutcome.Failure) {
			return CPOutcome.Failure;
		}
		if (x.updateMax(y.length-1) == CPOutcome.Failure) {
			return CPOutcome.Failure;
		}
		
		if (propagate() == CPOutcome.Failure) {
			return CPOutcome.Failure;
		}

		z.callPropagateWhenBoundsChange(this);
		x.callPropagateWhenDomainChanges(this);		
		x.callValBindWhenBind(this);

		return CPOutcome.Suspend;
	}

	@Override
	protected CPOutcome propagate() {
		// z = y[x] 
		int i = minIndSupp.getValue();
		while (i<y.length && (y[sortedPerm[i]] < z.getMin() || !x.hasValue(sortedPerm[i]))) {
			if (x.removeValue(sortedPerm[i]) == CPOutcome.Failure) {
				return CPOutcome.Failure;
			}
			i++;
		}
		minIndSupp.setValue(i);
		
		if (z.updateMin(y[sortedPerm[i]]) == CPOutcome.Failure){
			return CPOutcome.Failure;
		}
		
		i = maxIndSupp.getValue();
		while (i>=0 && (y[sortedPerm[i]] > z.getMax() || !x.hasValue(sortedPerm[i]))) {
			if (x.removeValue(sortedPerm[i]) == CPOutcome.Failure) {
				return CPOutcome.Failure;
			}
			i--;
		}
		maxIndSupp.setValue(i);
		
		if (z.updateMax(y[sortedPerm[i]]) == CPOutcome.Failure){
			return CPOutcome.Failure;
		}
		return CPOutcome.Suspend;
	}
	
	protected CPOutcome valBind(CPVarInt x) {
		// x is bound
		if (z.assign(y[x.getValue()]) == CPOutcome.Failure)
			return CPOutcome.Failure;
		return CPOutcome.Success;
	}

}
