/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *  
 * Contributors:
 *      www.n-side.com
 ******************************************************************************/
package scampi.reversible;

/**
 * Represents a trailable set to be used in the DomainWithHoles implementation
 * @author Pierre Schaus pschaus@gmail.com
 */


public interface ReversibleSet {
	
	/**
	 * @param val is the value to remove
	 * @return true if the value was present, false otherwise
	 */
	public boolean removeValue(int val);
	
	/**
	 * remove every value except v
	 * @param v
	 */
	public void removeAllBut(int v);
	
	
	/**
	 * @return the number of elements in the set
	 */
	public int getSize();
	
	/**
	 * @param val
	 * @return true if val is present
	 */
	public boolean hasValue(int val);
	
	/**
	 * @param val
	 * @return the smallest value >= val in the domain, if no such value, val-1 is returned
	 */
	public int getNextValue(int val);
	
	/**
	 * @param val
	 * @return returns the largest value <= val in the domain, if no such value, val+1 is returned 
	 */
	public int getPreValue(int val);
	
	/**
	 * @param all values under min must be removed
	 * @return the smallest values >= min still in the domain or Integer.MIN_VALUE if the set is empty
	 */
	public int setMinVal(int min);
	
	/**
	 * all values under max have been removed
	 * @param max
	 * @return the largest value <= max still in the domain or Integer.MAX_VALUE if the set is empty
	 */
	public int setMaxVal(int max);
	
	/**
	 * 
	 * @return the values in increasing order
	 */
	public Integer[] getValues();

}
