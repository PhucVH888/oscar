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
import scampi.cp.core.Store;
import scampi.cp.core.CPVarBool;
import scampi.cp.core.CPVarInt;

/**
 * Reified Greater or Equal Constraint
 * @author Pierre Schaus pschaus@gmail.com
 */
public class GrEqVarReif extends Constraint {

	CPVarInt x, y;
	CPVarBool b;

    /**
     * Constraint x greater or equal to y if and only if b is true <br>
     * x >= y <=> b
     * @param x
     * @param y
     * @param b
     */
	public GrEqVarReif(CPVarInt x, CPVarInt y, CPVarBool b) {
		super(x.getStore(),"GrEqVarReif");
		this.x = x;
		this.y = y;
		this.b = b;
	}
	
	@Override
	protected CPOutcome setup(CPPropagStrength l) {
		CPOutcome oc = updateBounds(x);
		if(oc == CPOutcome.Suspend){
			b.callValBindWhenBind(this);
			x.callUpdateBoundsWhenBoundsChange(this);
			y.callUpdateBoundsWhenBoundsChange(this);
			if (b.isBound()) {
				oc = valBind(b);
			}
		}
		return oc;
	}
	
	@Override
	protected CPOutcome updateBounds(CPVarInt var) {
		if (x.getMin() >= y.getMax()) {
			if (b.assign(1) == CPOutcome.Failure) {
				return CPOutcome.Failure;
			}
			return CPOutcome.Success;
		} else if (x.getMax() < y.getMin()) {
			if (b.assign(0) == CPOutcome.Failure) {
				return CPOutcome.Failure;
			}
			return CPOutcome.Success;
		}
		else {
			return CPOutcome.Suspend;
		}
	}
	
	
	protected int getPriorityBindL1(){
		return Store.MAXPRIORL1-1;
	}
		
	@Override
	protected CPOutcome valBind(CPVarInt var) {
		if (b.getValue() == 0) {
			//x < y
			if (s.post(new Le(x,y)) == CPOutcome.Failure) {
				return CPOutcome.Failure;
			}
		} else {
			//x >= v
			if (s.post(new GrEq(x,y)) == CPOutcome.Failure) {
				return CPOutcome.Failure;
			}				
		}
		return CPOutcome.Success;
	}

}

