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
public class TestTrailSet extends TestCase {
	
	private Store s;
	
    public TestTrailSet(String name) {
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
    
    
    public boolean equal(Integer[] t1, Integer[] t2) {
    	if (t1.length != t2.length) return false;
    	for (int i = 0; i < t2.length; i++) {
			if (t1[i] != t2[i]) return false;
		}
    	return true;
    }
    
    public void test() {
    	ReversibleSet set1 = new ReversibleSetBitVector(s, 5, 100);
    	ReversibleSet set2 = new ReversibleSetIndexedArray(s, 5, 100);
    	
    	set1.setMaxVal(35);
    	set2.setMaxVal(35);
    	
    	assertEquals(set1.getSize(), 31);
    	assertEquals(set2.getSize(), 31);
    	
    	
    	set1.setMinVal(25);
    	set2.setMinVal(25);
    	
    	assertEquals(set1.getSize(), 11);
    	assertEquals(set2.getSize(), 11);
    	
    	
    	assertTrue(set1.hasValue(25));
    	assertTrue(set2.hasValue(25));
    	
    	assertTrue(!set1.hasValue(24));
    	assertTrue(!set2.hasValue(24));
    	
    	s.pushState();
    	
    	set1.removeAllBut(27);
    	set2.removeAllBut(27);
    	
    	assertEquals(set1.getSize(), 1);
    	assertEquals(set2.getSize(), 1);
    	
    	set1.removeValue(27);
    	set2.removeValue(27);
    	
    	assertEquals(set1.getSize(), 0);
    	assertEquals(set2.getSize(), 0);  	
    	
    	s.pop();
    	
    	assertEquals(set1.getSize(), 11);
    	assertEquals(set2.getSize(), 11);
    	
    	assertTrue(set1.removeValue(30));
    	assertTrue(set2.removeValue(30));
    	assertTrue(set1.removeValue(34));
    	assertTrue(set2.removeValue(34));
    	assertFalse(set1.removeValue(34));
    	assertFalse(set2.removeValue(34));
    	
    	//25-29, 31-33, 35
    	
    	assertEquals(set1.getSize(), 9);
    	assertEquals(set2.getSize(), 9);
    	
    	assertTrue(set1.getPreValue(35) == 35);
    	assertTrue(set2.getPreValue(35) == 35);
    	assertTrue(set1.getPreValue(34) == 33);
    	assertTrue(set2.getPreValue(34) == 33);
    	assertTrue(!set1.hasValue(34));
    	assertTrue(!set2.hasValue(34));
    	
    	assertTrue(set1.getNextValue(30) == 31);
    	assertTrue(set2.getNextValue(30) == 31);
    	
    	
    	
    	set1.removeValue(35);
    	set2.removeValue(35);
    	
    	//25-29, 31-33
    	
    	assertEquals(set1.getSize(), 8);
    	assertEquals(set2.getSize(), 8);
    	
    	for (int i : new int[]{33,32,31,30,28,26,25}) {
			set1.removeValue(i);
			set2.removeValue(i);
		}
    	
    	//27, 29
    	
    	assertEquals(set1.getSize(), 2);
    	assertEquals(set2.getSize(), 2);
    	
    	assertEquals(set1.getNextValue(27), 27);
    	assertEquals(set2.getNextValue(27), 27);
    	
    	assertEquals(set1.getNextValue(10), 27);
    	assertEquals(set2.getNextValue(10), 27);
    	
    	System.out.println("set1:"+Arrays.toString(set1.getValues()));
    	System.out.println("set2:"+Arrays.toString(set2.getValues()));
    	
    }
    
   
}

