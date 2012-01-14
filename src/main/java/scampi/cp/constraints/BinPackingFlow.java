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

import scampi.cp.util.ArrayUtils;
import scampi.cp.core.CPOutcome;
import scampi.cp.core.CPPropagStrength;
import scampi.cp.core.CPVarInt;
import scampi.cp.core.Constraint;
import scampi.reversible.ReversibleInt;

/**
 * Redundant Bin-Packing Flow Constraint
 * @author pschaus@gmail.com
 */
public class BinPackingFlow extends Constraint {
	
	private CPVarInt [] x;
	private int [] sizes;
	private CPVarInt [] l;
	private CPVarInt [] c; // cardinalities

	private ReversibleInt [] l_t; // keep track of the current load of each bin
	private ReversibleInt [] c_t; // keep track of the number of items in each bin
	
	private int [] perm ; //permutation of sorted items i.e. s[perm[i]] <= s[perm[i+1]]
	
	public BinPackingFlow(CPVarInt [] x, int [] sizes, CPVarInt [] l) {
		super(x[0].getStore(),"BinPackingFlow");
		this.x = x;
		this.sizes = sizes;
		this.l = l;
		perm = ArrayUtils.sortPerm(sizes);
		c = CPVarInt.getArray(s, l.length, 0, sizes.length);
		l_t = new ReversibleInt[sizes.length];
		c_t = new ReversibleInt[sizes.length];
		for (int i = 0; i < l_t.length; i++) {
			l_t[i] = new ReversibleInt(s,0);
			c_t[i] = new ReversibleInt(s,0);
		}
	}
	
	@Override
	protected CPOutcome setup(CPPropagStrength strength) {
		for (CPVarInt var: x) {
			if (var.updateMax(l.length-1) == CPOutcome.Failure) {
				return CPOutcome.Failure;
			}
			if (var.updateMin(0) == CPOutcome.Failure) {
				return CPOutcome.Failure;
			}
		}
		if(s.post(new GCCVar(x, 0, c), CPPropagStrength.Strong) == CPOutcome.Failure) {
			return CPOutcome.Failure;
		}
		for (int j = 0; j < l.length; j++) {
			l[j].callPropagateWhenBoundsChange(this);
		}
		for (int i = 0; i < sizes.length; i++) {
			if (x[i].isBound()) {
				int j = x[i].getValue();
				l_t[j].setValue(l_t[j].getValue() + sizes[j]);
				c_t[j].incr();
			}
			else {
				x[i].callValBindIdxWhenBind(this,i);
				x[i].callPropagateWhenBind(this);
			}
		}
		return propagate();
	}
	
	@Override
	protected CPOutcome valBindIdx(CPVarInt x, int idx) {
		int j = x.getValue();
		int size = sizes[idx];
		l_t[j].setValue(l_t[j].getValue() + size);
		c_t[j].incr();
	    return CPOutcome.Suspend;
	}
	
	@Override
	protected CPOutcome propagate() {
		for (int j = 0; j < l.length; j++) {
			if (setCardinality(j) == CPOutcome.Failure){
		        return CPOutcome.Failure;
		    }
		}
		return CPOutcome.Suspend;
	}
	
	/**
	 * Adapt the cardinality of bin j
	 * @param j is the bin index
	 * @return Failure if fail detected when adapting cards, or Suspend otherwise
	 */
	private CPOutcome setCardinality(int j) {
	    int minVal = l[j].getMin();
	    int maxVal = l[j].getMax();
	    //how many items do I need at least to reach minVal ?
	    int v = l_t[j].getValue();
	    int i = x.length-1;
	    int nbAdded = 0;
	    while (v < minVal && i >= 0){
	      if (!x[perm[i]].isBound() && x[perm[i]].hasValue(j)) {
	        v += sizes[perm[i]];
	        nbAdded ++;
	      }
	      i--;
	    }
	    if(v < minVal) return CPOutcome.Failure; //not possible to reach the minimum level
	    int nbMin = nbAdded + c_t[j].getValue();
	    if (c[j].updateMin(nbMin) == CPOutcome.Failure){
	      return CPOutcome.Failure;
	    }
	    // how many items can I use at most before reaching maxVal ?
	    v = l_t[j].getValue();
	    i = 0;
	    nbAdded = 0;
	    while (i < x.length && v+sizes[perm[i]] <= maxVal) {
	      if (!x[perm[i]].isBound() && x[perm[i]].hasValue(j)) {
	        v += sizes[perm[i]];
	        nbAdded ++;
	      }
	      i++;
	    }
	    int nbMax = nbAdded + c_t[j].getValue();
	    if (c[j].updateMax(nbMax) == CPOutcome.Failure){
	      return CPOutcome.Failure;
	    }
		return CPOutcome.Suspend;
	}

}
