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

import scampi.cp.core.CPOutcome;
import scampi.cp.core.CPPropagStrength;
import scampi.cp.core.Constraint;
import scampi.cp.core.CPVarInt;
import scampi.reversible.ReversibleBool;
import scampi.reversible.ReversibleInt;

/**
 * @author Pierre Schaus pschaus@gmail.com
 */
public class AtLeastNValueFWC extends Constraint {
	
	private CPVarInt [] x;
	
	private CPVarInt nValueVar;
	
	private ReversibleBool [] isValueUsed; //for each value if it is used or not
	private ReversibleInt nbValueUsed; //number of value used
	private ReversibleInt nbBound; //number of bound variables


	private int min;
	private int max;
	private int valSize;
	
	
	public AtLeastNValueFWC(CPVarInt [] x, CPVarInt nval) {
		super(x[0].getStore(),"AtLeastNValueFWC");
		this.x = x;
		this.nValueVar = nval;
	}

	@Override
	public CPOutcome setup(CPPropagStrength l) {
	    
	     findValueRange();

	     //initialize trails and counters
	     isValueUsed   = new ReversibleBool[valSize];
	     for (int v = 0; v < valSize; v++) {
	    	 isValueUsed[v] = new ReversibleBool(s);
	    	 isValueUsed[v].setValue(false);
	     }
	     nbValueUsed = new ReversibleInt(s);
	     nbValueUsed.setValue(0);
	     nbBound = new ReversibleInt(s);
	     nbBound.setValue(0);
	     	    
	     for (int k = 0; k < x.length; k++) {
	       if (x[k].isBound()) {
	    	 int v = x[k].getValue();
	         nbBound.incr();
	         if (!isValueUsed[v-min].getValue()) {
	           nbValueUsed.incr();
	           isValueUsed[v-min].setValue(true);
	         }
	       }
	     }

	     //update lower bound on the number of values
	     if (nValueVar.updateMin(Math.max(nbValueUsed.getValue(), x.length>0 ? 1:0)) == CPOutcome.Failure) {
	       return CPOutcome.Failure;
	     }

	     //update upper bound on the number of values
	     if (nValueVar.updateMax(nbValueUsed.getValue()+x.length-nbBound.getValue()) == CPOutcome.Failure) {
	       return CPOutcome.Failure;
	     }

	     for (int k=0; k < x.length; k++) {
	       if (!x[k].isBound())
	         x[k].callValBindIdxWhenBind(this,k);
	       	 x[k].callPropagateWhenBind(this);
	     }
	     if (!nValueVar.isBound()) {
	       nValueVar.callPropagateWhenBoundsChange(this);
	     }

	     int ubNbValueUsed = nbValueUsed.getValue() + (x.length -nbBound.getValue());
	     if(ubNbValueUsed <= nValueVar.getMin()){
	       return prune();
	     }

	     return CPOutcome.Suspend;
	}
	
	@Override
	public CPOutcome valBindIdx(CPVarInt var, int idx) {
		
		int val = var.getValue();
		nbBound.incr();
		if(!isValueUsed[val-min].getValue()){
			nbValueUsed.incr();
			isValueUsed[val-min].setValue(true);
		}

		int ubNbValueUsed = nbValueUsed.getValue() + (x.length-nbBound.getValue());

		if(nValueVar.updateMin(nbValueUsed.getValue()) == CPOutcome.Failure){
			return CPOutcome.Failure;
		}
		if(nValueVar.updateMax(ubNbValueUsed) == CPOutcome.Failure){
			return CPOutcome.Failure;
		}

		if(ubNbValueUsed == nValueVar.getMin()){
			return prune();
		}

		return CPOutcome.Suspend;
	}
	
	@Override
	public CPOutcome propagate() {
		//_nValueVar has changed
		int ubNbValueUsed = nbValueUsed.getValue() + (x.length - nbBound.getValue());
		if(ubNbValueUsed == nValueVar.getMin()){
			return prune();
		}
		return CPOutcome.Suspend;
	}
	
	public CPOutcome prune(){
		  //remove used values from unbound variables
		  int [] values = new int[x.length];
		  int nb = 0;
		  for(int k = 0; k < x.length; k++){
		    if(x[k].isBound()){
		      values[nb] = x[k].getValue();
		      nb++;
		    }
		  }
		  for(int k = 0; k < x.length; k++){
		    if(!x[k].isBound()){
		      for(int i = 0; i < nb; i++){
		        if(x[k].removeValue(values[i]) == CPOutcome.Failure){
		          return CPOutcome.Failure;
		        }
		      }
		    }
		  }
		  return CPOutcome.Suspend;
	}
	
	private void findValueRange(){
		min = Integer.MAX_VALUE;
		max = Integer.MIN_VALUE;
		for(int i = 0; i < x.length; i++) {
			min = Math.min(min, x[i].getMin());
			max = Math.max(max, x[i].getMax());
		}
		valSize = max - min + 1;
	}

}
