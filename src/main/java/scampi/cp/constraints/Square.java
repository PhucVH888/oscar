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

import scampi.cp.util.NumberUtils;
import scampi.cp.core.CPOutcome;
import scampi.cp.core.CPPropagStrength;
import scampi.cp.core.Constraint;
import scampi.cp.core.Store;
import scampi.cp.core.CPVarInt;

/**
 * @author Pierre Schaus pschaus@gmail.com
 */
public class Square extends Constraint {

	private CPVarInt x;
	private CPVarInt y;

	/**
	 * x*x == y
	 * @param x
	 * @param y
	 * @see  CPVarInt#mul(cp.core.CPVarInt)
	 */
	public Square(CPVarInt x, CPVarInt y) {
		super(x.getStore(),"Square");
		this.x = x;
		this.y = y;
	}

	@Override
	public CPOutcome setup(CPPropagStrength l) {
		if (y.updateMin(0) == CPOutcome.Failure) { 
			return CPOutcome.Failure;
		}
		CPOutcome ok = propagate();
		if (ok != CPOutcome.Suspend) {
			return ok;
		}
		if (!x.isBound()) {
			x.callPropagateWhenBoundsChange(this);
		}
		if (!y.isBound()) {
			y.callPropagateWhenBoundsChange(this);
		}
		return CPOutcome.Suspend;
	}

	@Override
	public CPOutcome propagate() {
		
		    // propagation of y
		    
	
			int mx = x.getMin();
			int Mx = x.getMax();
			int mx2 = mx*mx;
			int Mx2 = Mx*Mx;

			//propagate y (which is not bound)
			if (mx >=0) {
				if (y.updateMin(mx2) == CPOutcome.Failure) {
					return CPOutcome.Failure;
				}
				if (y.updateMax(Mx2) == CPOutcome.Failure) {
					return CPOutcome.Failure;
				}
			} else if (Mx <= 0) {
				if (y.updateMin(Mx2) == CPOutcome.Failure) {
					return CPOutcome.Failure;
				}
				if (y.updateMax(mx2) == CPOutcome.Failure) {
					return CPOutcome.Failure;
				}
			} else if (x.hasValue(0)) {
				//y min is already >= 0 (post does it)
				if (y.updateMax(Math.max(mx2, Mx2)) == CPOutcome.Failure) {
					return CPOutcome.Failure;
				}
			} else {
				int a = x.getValueBefore(0);
				int b = x.getValueAfter(0);
				int a2 = a*a;
				int b2 = b*b;
				if (y.updateMin(Math.min(a2, b2)) == CPOutcome.Failure) {
					return CPOutcome.Failure;
				}
				if (y.updateMax(Math.max(a2, b2)) == CPOutcome.Failure) {
					return CPOutcome.Failure;
				}
			}
			//propagate x (which is not bound)
			int my = y.getMin();
			int My = y.getMax();
			int my2 = my*my;
			int My2 = My*My;

			int rootm = (int) Math.sqrt(my);
			int rootM = (int) Math.sqrt(My);

			if (mx >= 0) {
				if (x.updateMin(rootm) == CPOutcome.Failure) {
					return CPOutcome.Failure;
				}
				if (x.updateMax(rootM) == CPOutcome.Failure) {
					return CPOutcome.Failure;
				}
			} else if (Mx <= 0) {
				if (x.updateMax(rootm) == CPOutcome.Failure) {
					return CPOutcome.Failure;
				}
				if (x.updateMin(rootM) == CPOutcome.Failure) {
					return CPOutcome.Failure;
				}
			} else {
				if (x.updateMin(-rootM) == CPOutcome.Failure) {
					return CPOutcome.Failure;
				}

				if (x.updateMax(rootM) == CPOutcome.Failure) {
					return CPOutcome.Failure;
				}
				/*
				for (int v = -rootm+1; v < rootm; v++) {
					if (x.removeValue(v) == CPOutcome.Failure) {
						return CPOutcome.Failure;
					}
				}*/
			}
		
		return CPOutcome.Suspend;
	}

}
