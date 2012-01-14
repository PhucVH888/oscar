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
import scampi.cp.core.Constraint;
import scampi.cp.core.CPVarInt;

/**
 * Element Constraint on a 2D array
 * @author Pierre Schaus pschaus@gmail.com
 */

public class ElementCst2D extends Constraint {
	
	private final int [][] T;
	private CPVarInt x;
	private CPVarInt y;
	private CPVarInt z;
	
	private static int [][] prevT;
	private static TableData prevData;
	
	//T[x][y] = z
	public ElementCst2D(final int [][] T, CPVarInt x, CPVarInt y, CPVarInt z) {
		super(x.getStore(),"ElementCst2D");
		this.y = y;
		this.x = x;
		this.z = z;
		this.T = T;	
		generateData();
	}
	
	private void generateData() {
		if (prevT != T) {
			prevT = T;
			prevData = new TableData(3);
			for (int i = 0; i < T.length; i++) {
				for (int j = 0; j < T[i].length; j++) {
					prevData.addTuple(new int[]{i,j,T[i][j]});
				}
			}
		}
	}
	
	@Override
	public CPOutcome setup(CPPropagStrength l) {
		if (s.post(new Table(prevData,x,y,z)) == CPOutcome.Failure) {
			return CPOutcome.Failure;
		}
		return CPOutcome.Success;
	}
	
	public static CPVarInt get(final int [][] T, CPVarInt x, CPVarInt y){
		CPVarInt z = new CPVarInt(x.getStore(),ArrayUtils.min(T),ArrayUtils.max(T));
		if (x.getStore().post(new ElementCst2D(T,x,y,z)) == CPOutcome.Failure) {
			throw new RuntimeException("ElementCst2D failure when posting");
		}
		return z;
	}

}
