/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *  
 * Contributors:
 *      www.n-side.com
 ******************************************************************************/
package scampi.cp.test;

import scampi.cp.constraints.*;
import scampi.cp.core.*;
import scampi.cp.util.*;
import scampi.cp.search.*;
import scampi.reversible.*;
import scampi.search.*;

import junit.framework.TestCase;
import java.util.Arrays;


/**
 * @author Pierre Schaus pschaus@gmail.com
 */
public class TestGrAbs extends TestCase {
	
	private Store s;	
	
    public TestGrAbs(String name) {
        super(name);
        
    }
    	
	 /**
     * setUp() method that initializes common objects
     */
    protected void setUp() throws Exception {
        super.setUp();
        s = new Store();
    }

    /**
     * tearDown() method that cleanup the common objects
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        s = null;
    }
    
    public void testGrAbs(){  	
    	CPVarInt [] x = new CPVarInt[2];
    	for (int i = 0; i < x.length; i++) {
			x[i] = new CPVarInt(s,1,256);
		}
    	
    	s.post(new GrEq((x[0].minus(x[1])).abs(),0));
    	
    	
    	
    	s.post(new Eq(x[0],1));
    	
    	assertTrue(s.getStatus()!= CPOutcome.Failure);


    }

}
