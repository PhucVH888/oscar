/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *  
 * Contributors:
 *      www.n-side.com
 ******************************************************************************/
package scampi.search;

/**
 * @author Pierre Schaus pschaus@gmail.com
 */
public class SearchLimitException extends Exception {
	public SearchLimitException(String message){
		super(message);
	}
}
