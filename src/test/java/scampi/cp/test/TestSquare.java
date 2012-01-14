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
import java.util.Random;


/**
 * @author Pierre Schaus pschaus@gmail.com
 */
public class TestSquare extends TestCase {
	
    public TestSquare(String name) {
        super(name);
    }
    	
	 /**
     * setUp() method that initializes common objects
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
     * tearDown() method that cleanup the common objects
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    

    
    public void testSquare1() {  
    	Store s = new Store();
    	CPVarInt x = new CPVarInt(s,-5,5);
    	CPVarInt y = new CPVarInt(s,-5,16);
    	s.post(new Square(x, y));
    	assertTrue(!s.isFailed());
    	assertTrue(x.getMin() == -4);
    	assertTrue(x.getMax() == 4);
    	assertTrue(y.getMax() == 16);
    	assertTrue(y.getMin() == 0);
    }

}
