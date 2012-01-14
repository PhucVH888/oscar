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

/**
 * Difference Constraint x != y
 * @author Pierre Schaus pschaus@gmail.com
 */
public class Diff extends Constraint {

	Constraint diffImpl;

    /**
     * Ask that x and y takes different values (x != y)
     * @param x
     * @param y
     * @see AllDifferent
     */
	public Diff(CPVarInt x, int y) {
		super(x.getStore(),"Diff");
		diffImpl = new DiffVal(x,y);
	}

    /**
     * Ask that x and y takes different values (x != y)
     * @param x
     * @param y
     * @see AllDifferent
     */
	public Diff(CPVarInt x, CPVarInt y) {
		super(x.getStore());
		diffImpl = new DiffVar(x,y);
	}
	
 
	@Override
	protected CPOutcome setup(CPPropagStrength l) {
		if (s.post(diffImpl) == CPOutcome.Failure) {
			return CPOutcome.Failure;
		}
		return CPOutcome.Success;
	}

}


class DiffVal extends Constraint {

	CPVarInt x;
	int y;
	
	public DiffVal(CPVarInt x, int y) {
		super(x.getStore(),"DiffVal");
		this.x = x;
		this.y = y;
	}
		
	@Override
	protected CPOutcome setup(CPPropagStrength l) {
		if (x.removeValue(y) ==  CPOutcome.Failure) {
			return CPOutcome.Failure;
		}
		return CPOutcome.Success;
	}
	
	

}

class DiffVar extends Constraint {

	CPVarInt x, y;
	
	public DiffVar(CPVarInt x, CPVarInt y) {
		super(x.getStore(),"DiffVar");
		this.x = x;
		this.y = y;
	}
	
	@Override
	protected CPOutcome setup(CPPropagStrength l) {
		CPOutcome oc = propagate();
		if(oc != CPOutcome.Success){
			x.callPropagateWhenBind(this);
			y.callPropagateWhenBind(this);
		}
		return oc;
	}
	
	@Override
	protected CPOutcome propagate() {
		if(x.isBound()){
			//System.out.println("x bound to "+x.getValue()+" remove from "+y);
			if(y.removeValue(x.getValue()) == CPOutcome.Failure){
				return CPOutcome.Failure;
			}
			return CPOutcome.Success;
		}
		if(y.isBound()){
			if(x.removeValue(y.getValue()) == CPOutcome.Failure){
				return CPOutcome.Failure;
			}
			return CPOutcome.Success;
		}
		return CPOutcome.Suspend;
	}

}
