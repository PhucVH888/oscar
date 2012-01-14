/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *  
 * Contributors:
 *      www.n-side.com
 ******************************************************************************/
package scampi.des.engine

/**
 * Every simulated object taking part in the simulation should extend this class.
 * @author pschaus
 */
class Process (m: Model, name : String = "Process"){

	private var suspending = false
	private var suspended = {}
	
	def suspend(block : => Unit) {
		if (suspending) {
			//throw new RuntimeException("The process " + name + " is already suspending");
		}
		suspending = true
		suspended = block
	}
	
	def resume(){
		if (!suspending){
			//throw new RuntimeException("The process " + name + " is not suspending");
		}
		suspending = false
		suspended
	}
}