/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *  
 * Contributors:
 *      www.n-side.com
 ******************************************************************************/
package scampi.cp.core;

/**
 * @author Pierre Schaus pschaus@gmail.com
 */
public class PropagEventUpdateMinIdx extends PropagEvent {
	
	private CPVarInt var;
	private int val;
	private int idx;
	
	public PropagEventUpdateMinIdx(Constraint cstr,CPVarInt var, int idx, int val) {
		super(cstr);
		this.var = var;
		this.val = val;
		this.idx = idx;
	}
	
	int getPrior() {
		return cstr.getPriorityBoundsL1();
	}
	
	@Override
	public CPOutcome notifyConstraint() {
		return cstr.updateMinIdx(var,idx, val);
	}

}
