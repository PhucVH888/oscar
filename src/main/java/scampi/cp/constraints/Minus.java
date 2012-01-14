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
 * Minus Constraint
 * @author Pierre Schaus pschaus@gmail.com
 */
public class Minus extends Constraint {
	
	private CPVarInt x;
	private CPVarInt y;
	private CPVarInt z;

    /**
     * x - y == z
     * @param x
     * @param y
     * @param z
     * @see CPVarInt#minus(cp.core.CPVarInt)
     */
	public Minus(CPVarInt x, CPVarInt y, CPVarInt z) {
		super(x.getStore());
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	protected CPOutcome setup(CPPropagStrength l) {
			
		if (propagate() == CPOutcome.Failure) {
			return CPOutcome.Failure;
		}
		if (!x.isBound()) {
			x.callPropagateWhenBoundsChange(this);
		}
		if (!y.isBound()) {
			y.callPropagateWhenBoundsChange(this);
		}
		if (!z.isBound()) {
			z.callPropagateWhenBoundsChange(this);
		}
		return CPOutcome.Suspend;
	}
	
	
	private CPOutcome prune(CPVarInt A, CPVarInt B, CPVarInt C) {
		//prune var C = A-B 
		if (C.updateMax(A.getMax()-B.getMin()) == CPOutcome.Failure) {
			return CPOutcome.Failure;
		}
		if (C.updateMin(A.getMin()-B.getMax()) == CPOutcome.Failure) {
			return CPOutcome.Failure;
		}
		return CPOutcome.Suspend;
	}
	
	@Override
	protected CPOutcome propagate() {
		//x-y=z		
		
		//prune z (= x -y)
		if (prune(x,y,z) == CPOutcome.Failure) {
			return CPOutcome.Failure;
		}
		//prune y (=x-z)
		if (prune(x,z,y) == CPOutcome.Failure) {
			return CPOutcome.Failure;
		}
		
		
		//prune x (=z+y)
		if (x.updateMax(z.getMax()+y.getMax()) == CPOutcome.Failure) {
			return CPOutcome.Failure;
		}
		if (x.updateMin(z.getMin()+y.getMin()) == CPOutcome.Failure) {
			return CPOutcome.Failure;
		}
					
		return CPOutcome.Suspend;
	}
}
