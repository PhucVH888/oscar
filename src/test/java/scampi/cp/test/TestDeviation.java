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

import scampi.cp.constraints.BinaryKnapsack;
import scampi.cp.constraints.Diff;
import scampi.cp.core.CPPropagStrength;
import scampi.cp.core.CPVarBool;
import scampi.cp.core.CPVarInt;
import scampi.cp.core.Store;
import scampi.cp.util.ArrayUtils;
import scampi.cp.constraints.AllDifferent;
import scampi.cp.constraints.Deviation;
import scampi.cp.constraints.GCC;
import scampi.cp.constraints.MulVar;
import scampi.cp.constraints.Sequence;
import scampi.cp.constraints.Sum;
import scampi.cp.constraints.WeightedSum;
import scampi.cp.core.CPPropagStrength;
import scampi.cp.core.CPVarInt;
import scampi.cp.core.Store;
import scampi.cp.search.*;
import scampi.cp.util.Counter;
import scampi.reversible.SetIndexedArray;
import scampi.search.Search;
import scampi.search.SolutionObserver;

import junit.framework.TestCase;
import junit.framework.TestCase;
import java.util.Arrays;



/**
 * @author Pierre Schaus pschaus@gmail.com
 */
public class TestDeviation extends TestCase {



    public TestDeviation(String name) {
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



    public void testDeviation1(){
        Store cp = new Store();
    	CPVarInt [] x = new CPVarInt[4];
    	for (int i = 0; i < x.length; i++) {
			x[i] = new CPVarInt(cp,-2,2);
		}
    	CPVarInt nd = new CPVarInt(cp,0,0);
    	cp.post(new Deviation(x,0,nd));
        assertTrue(!cp.isFailed());
        for (int i = 0; i < x.length; i++) {
            assertTrue(x[i].isBound() && x[i].getValue() == 0);
        }
    }

    public void testDeviation2(){
        Store cp = new Store();
    	CPVarInt [] x = new CPVarInt[4];
    	for (int i = 0; i < x.length; i++) {
			x[i] = new CPVarInt(cp,-2,2);
		}
    	CPVarInt nd = new CPVarInt(cp,0,6);
    	cp.post(new Deviation(x,1,nd));
        assertTrue(!cp.isFailed());

    }



    public void testDeviation3(){

        Store cp = new Store();
    	CPVarInt [] x = new CPVarInt[4];
    	x[0] = new CPVarInt(cp,3,7);
        x[1] = new CPVarInt(cp,0,5);
        x[2] = new CPVarInt(cp,5,6);
        x[3] = new CPVarInt(cp,5,7);

    	CPVarInt nd = new CPVarInt(cp,0,18);
    	cp.post(new Deviation(x,17,nd));
        assertTrue(!cp.isFailed());
        assertEquals(x[0].getMax(),5);

    }

    public void testDeviation4(){

        Store cp = new Store();
    	CPVarInt [] x = new CPVarInt[4];
    	x[0] = new CPVarInt(cp,3,7);
        x[1] = new CPVarInt(cp,0,5);
        x[2] = new CPVarInt(cp,5,6);
        x[3] = new CPVarInt(cp,5,7);

    	CPVarInt nd = new CPVarInt(cp,0,12);
    	cp.post(new Deviation(x,17,nd));
        assertTrue(!cp.isFailed());
        assertEquals(x[0].getMax(),4);

    }

    public void testDeviation5() {

        Store cp = new Store();
    	CPVarInt [] x = new CPVarInt[4];
    	x[0] = new CPVarInt(cp,3,10);
        x[1] = new CPVarInt(cp,4,5);
        x[2] = new CPVarInt(cp,3,6);
        x[3] = new CPVarInt(cp,0,2);

    	CPVarInt nd = new CPVarInt(cp,0,45);
    	cp.post(new Deviation(x,17,nd));
        assertTrue(!cp.isFailed());
        assertEquals(x[0].getMin(),4);
        assertEquals(x[0].getMax(),9);

    }

    public void testDeviation6() {

        Store cp = new Store();
    	CPVarInt [] x = new CPVarInt[4];
    	x[0] = new CPVarInt(cp,3,10);
        x[1] = new CPVarInt(cp,4,5);
        x[2] = new CPVarInt(cp,3,6);
        x[3] = new CPVarInt(cp,0,2);

    	CPVarInt nd = new CPVarInt(cp,0,22);
    	cp.post(new Deviation(x,17,nd));
        assertTrue(!cp.isFailed());
        assertEquals(x[0].getMin(),4);
        assertEquals(x[0].getMax(),7);

    }

    public void testDeviation7() {

        Store cp = new Store();
    	CPVarInt [] x = new CPVarInt[4];
    	x[0] = new CPVarInt(cp,3,10);
        x[1] = new CPVarInt(cp,4,5);
        x[2] = new CPVarInt(cp,3,6);
        x[3] = new CPVarInt(cp,0,2);

    	CPVarInt nd = new CPVarInt(cp,0,30);
    	cp.post(new Deviation(x,17,nd));
        assertTrue(!cp.isFailed());
        assertEquals(x[0].getMin(),4);
        assertEquals(x[0].getMax(),8);

    }

    public void testDeviation8() {

        Store cp = new Store();
    	CPVarInt [] x = new CPVarInt[4];

        x[0] = new CPVarInt(cp,4,5);
        x[1] = new CPVarInt(cp,3,6);
        x[2] = new CPVarInt(cp,0,2);
        x[3] = new CPVarInt(cp,3,10);

    	CPVarInt nd = new CPVarInt(cp,0,30);
    	cp.post(new Deviation(x,17,nd));
        assertTrue(!cp.isFailed());
        assertEquals(x[3].getMin(),4);
        assertEquals(x[3].getMax(),8);

    }

    public void testDeviation9() {

        Store cp = new Store();
    	CPVarInt [] x = new CPVarInt[6];

        x[0] = new CPVarInt(cp,11,16);
        x[1] = new CPVarInt(cp,9,11);
        x[2] = new CPVarInt(cp,12,14);
        x[3] = new CPVarInt(cp,13,14);
        x[4] = new CPVarInt(cp,10,12);
        x[5] = new CPVarInt(cp,12,15);


    	CPVarInt nd = new CPVarInt(cp,0,1000);
    	cp.post(new Deviation(x,74,nd));
        assertTrue(!cp.isFailed());
        assertEquals(nd.getMin(),24);
    }

     public void testDeviation10() {
    	 int nbDevi = nbSol(false);
    	 int nbDecomp = nbSol(true);
         assertEquals(nbDevi,nbDecomp);
    }


    private int nbSol(boolean decomp) {
        Store cp = new Store();


        int s = 74;
    	CPVarInt [] x = new CPVarInt[6];
        x[0] = new CPVarInt(cp,11,16);
        x[1] = new CPVarInt(cp,9,11);
        x[2] = new CPVarInt(cp,12,14);
        x[3] = new CPVarInt(cp,13,14);
        x[4] = new CPVarInt(cp,10,12);
        x[5] = new CPVarInt(cp,12,15);
    	CPVarInt nd = new CPVarInt(cp,0,34);

        if (decomp)
        	deviationDecomp(x,s,nd);
        else
        	cp.add(new Deviation(x, s, nd));

        Search search = new Search(cp,new BinaryFirstFail(x));
        final Counter cnt = new Counter();
        search.addSolutionObserver(new SolutionObserver() {
            public void solutionFound() {
                cnt.incr();
            }
        });
        search.solveAll();
        return  cnt.getValue();
    }

    private void deviationDecomp(CPVarInt [] x, int s, CPVarInt nd) {
        Store cp = x[0].getStore();
        CPVarInt [] dev = new CPVarInt[x.length];
		for (int i = 0; i < dev.length; i++) {
			dev[i] = x[i].mul(x.length).minus(s).abs();
		}
		cp.add(new Sum(dev, nd));
        cp.add(new Sum(x,s));
    }
    
    
    public void testDeviation11(){
        Store cp = new Store();
    	CPVarInt [] x = new CPVarInt[8];

        x[0] = new CPVarInt(cp,-27,-25);
        x[1] = new CPVarInt(cp,-27,-27);
		x[2] = new CPVarInt(cp,-27,-25);
        x[3] = new CPVarInt(cp,-27,-25);
		x[4] = new CPVarInt(cp,-30,-30);
        x[5] = new CPVarInt(cp,-27,-25);
		x[6] = new CPVarInt(cp,-27,-25);
        x[7] = new CPVarInt(cp,-27,-23);


    	CPVarInt nd = new CPVarInt(cp,0,75);
    	cp.post(new Deviation(x,-213,nd));
        assertTrue(!cp.isFailed());
        assertEquals(x[7].getMax(),-24);

    }
    
  public void testDeviation12(){
	Store cp = new Store();
	CPVarInt [] x = new CPVarInt[6];

	x[0] = new CPVarInt(cp,11,16);
	x[1] = new CPVarInt(cp,9,11);
	x[2] = new CPVarInt(cp,12,14);
	x[3] = new CPVarInt(cp,13,14);
	x[4] = new CPVarInt(cp,10,12);
	x[5] = new CPVarInt(cp,12,15);



	CPVarInt nd = new CPVarInt(cp,0,34);
	cp.post(new Deviation(x,74,nd));
	assertTrue(!cp.isFailed());
    assertEquals(x[0].getMax(),14);

 }
    
  public void testDeviation13(){
	Store cp = new Store();
	CPVarInt [] x = new CPVarInt[6];
	
	x[0] = new CPVarInt(cp,-14,-12);
	x[1] = new CPVarInt(cp,-11,-9);
	x[2] = new CPVarInt(cp,-14,-12);
	x[3] = new CPVarInt(cp,-14,-13);
	x[4] = new CPVarInt(cp,-12,-10);
	x[5] = new CPVarInt(cp,-14,-12);



	CPVarInt nd = new CPVarInt(cp,0,34);
	cp.post(new Deviation(x,-74,nd));
	assertTrue(!cp.isFailed());
    assertEquals(x[1].getMax(),-10);

 }
    

}
