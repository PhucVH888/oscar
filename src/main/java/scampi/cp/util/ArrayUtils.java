/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *  
 * Contributors:
 *      www.n-side.com
 ******************************************************************************/

package scampi.cp.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import scampi.cp.core.CPVarInt;
import scampi.search.Objective;

/**
 * @author Pierre Schaus pschaus@gmail.com
 */
public class ArrayUtils {

    /**
     *
     * @param a
     * @return the maximum value in a
     */
	public static int max(int ... a) {
		int v = Integer.MIN_VALUE;
		for (int i = 0; i < a.length; i++) {
			v = Math.max(v, a[i]);
		}
		return v;
	}

    /**
     *
     * @param a
     * @return the maximum value in a
     */
	public static int max(int [][] a) {
		int v = Integer.MIN_VALUE;
		for (int i = 0; i < a.length; i++) {
			v = Math.max(v, max(a[i]));
		}
		return v;
	}

    /**
     *
     * @param a
     * @return the minimum value in a
     */
	public static int min(int ... a) {
		int v = Integer.MAX_VALUE;
		for (int i = 0; i < a.length; i++) {
			v = Math.min(v, a[i]);
		}
		return v;
	}

    /**
     *
     * @param a
     * @return  the minimum value in a
     */
	public static int min(int [][] a) {
		int v = Integer.MAX_VALUE;
		for (int i = 0; i < a.length; i++) {
			v = Math.min(v, min(a[i]));
		}
		return v;
	}

    /**
     *
     * @param a
     * @return  the index of a maximum value in a
     */
	public static int argMax(int [] a) {
		int v = Integer.MIN_VALUE;
		int ind = -1;
		for (int i = 0; i < a.length; i++) {
			if (a[i] > v) {
				v = a[i];
				ind = i;
			}
		}
		return ind;
	}

    /**
     *
     * @param a
     * @return  the index of a minimum value in a
     */
	public static int argMin(int [] a) {
		int v = Integer.MAX_VALUE;
		int ind = -1;
		for (int i = 0; i < a.length; i++) {
			if (a[i] < v) {
				v = a[i];
				ind = i;
			}
		}
		return ind;
	}

    /**
     *
     * @param a
     * @return the sum of the values in a
     */
	public static int sum(int [] a) {
		int v = 0;
		for (int i: a) {
			v += i;
		}
		return v;
	}
	
    /**
    *
    * @param a
    * @return the sum of the values in a
    */
	public static double sum(double [] a) {
		double v = 0;
		for (double i: a) {
			v += i;
		}
		return v;
	}	

    /**
     *
     * @param a
     * @return the sum of the values in a
     */
	public static int sum(int [][] a) {
		int s = 0;
		for (int i = 0; i < a.length; i++) {
			s += sum(a[i]);
		}
		return s;
	}

    /**
     *
     * @param a
     * @return the product of values in a
     */
	public static int prod(int [] a) {
		int v = 1;
		for (int i: a) {
			v *= i;
		}
		return v;
	}

	
	public  static  CPVarInt[] append(CPVarInt[] a1, CPVarInt[] a2) {
		CPVarInt [] res = new CPVarInt[a1.length + a2.length];
		int i = 0;
		for (CPVarInt x: a1) {
			res[i++] = x;
		}
		for (CPVarInt x: a2) {
			res[i++] = x;
		}
		return res;
	}
	
    /**
     *
     * @param a matrix array
     * @return  the column c that is [a[0][c] , ... , a[n-1][c]]
     */
    public static CPVarInt[] getSlice(CPVarInt [][] a, int c) {
        assert(c > 0);
		CPVarInt [] res = new CPVarInt[a.length];
		for (int i = 0; i < a.length; i++) {
            assert(c < a[i].length);
			res[i] = a[i][c];
		}
		return res;
	}
    
    /**
    *
    * @param a matrix array
    * @return  the column c that is [a[0][c] , ... , a[n-1][c]]
    */
   public static int[] getSlice(int[][] a, int c) {
       assert(c > 0);
		int [] res = new int[a.length];
		for (int i = 0; i < a.length; i++) {
           assert(c < a[i].length);
			res[i] = a[i][c];
		}
		return res;
	}
	
	/**
	 * 
	 * @param v
	 * @param n
	 * @return an array of length n each entry with value v
	 */
	public static int[] replicate(int v, int n) {
		int [] res = new int[n];
		for (int i = 0; i < n; i++) {
			res[i] = v;
		}
		return res;
	}

    /**
     *
     * @param a
     * @param <E>
     * @return a flattened array list of all elements in a
     */
	public static<E> ArrayList<E> flatten(E [][] a) {
		ArrayList<E> res = new ArrayList<E>();
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[i].length; j++) {
				res.add(a[i][j]);
			}
		}
		return res;
		
	}

    /**
     *
     * @param a
     * @return an array containing all element in matrix a
     */
	public static CPVarInt[] flattenvars(CPVarInt [][] a) {
		return flatten(a).toArray(new CPVarInt[0]);
	}

    /**
     *
     * @param a
     * @return the minimum domain value over all the variables in a
     */
	public static int getMinVal(CPVarInt [] a) {
		int res = a[0].getMin();
		for (CPVarInt x: a) {
			if (x.getMin() < res) {
				res = x.getMin();
			}
		}
		return res;
	}

    /**
     *
     * @param a
     * @return the maximum domain value over all the elements in a
     */
	public static int getMaxVal(CPVarInt [] a) {
		int res = a[0].getMax();
		for (CPVarInt x: a) {
			if (x.getMax() > res) {
				res = x.getMax();
			}
		}
		return res;
	}
	
	//------------------------general useful methods for heuristics--------------------

    /**
     *
     * @param x
     * @return the index of the first unbound variable, -1 if all variables are bound
     */
	public static int getFirstNotBound(CPVarInt [] x){
		for (int i = 0; i < x.length; i++) {
			if(!x[i].isBound()){
				return i;
			}
		}
		return -1;
	}

    /**
     *
     * @param x
     * @return  the index of the unbound variable having the smalles possible value in its domain, <br>
     *          -1 if all variables are bound.
     */
	public static int getMinValNotBound(CPVarInt [] x) {
		int val = Integer.MAX_VALUE;
		int var = -1;
		for (int i = 0; i < x.length; i++) {
			if (!x[i].isBound() && x[i].getMin() < val) {
				var = i;
				val = x[i].getMin();
			}
		}
		return var;
	}
	
	/**
	 * Returns the index in x of a randomly chosen unbound variable according to uniform distribution
	 * @param x
	 * @return the index in x of a randomly chosen unbound variable according to uniform distribution, -1 if every variable is bound
	 */
	public static int getRandomNotBound(final CPVarInt [] x) {
		assert(x.length > 0);
		Random rand = x[0].getStore().getRandom();
		int cpt = 0;
		int curr = -1;
		for (int i = 0; i < x.length; i++) {
			if (!x[i].isBound()) {
				cpt++;
				// replace the current one with a probability of 1/cpt
				if (rand.nextInt(cpt) == 0) {
					curr = i;
				}
			}
		}
		return curr;
	}
	
	

    /**
     *
     * @param x
     * @return the index of the unbound variable in x having the smallest domain (useful for first fail heuristics).
     */
	public static int getMinDomNotBound(final CPVarInt [] x) {
		return getVarNotBound(x, new Comparator<Integer>() {
			public int compare(Integer o1, Integer o2) {
				return x[o1].getSize()-x[o2].getSize();
			}
		});
	}
	
	/**
	 * @param x
	 * @param comp
	 * @return the index of the unbound variable in x which is the smallest one according  <br>
	 * 			 to comparators comp used lexicographically, -1 if all variables are bound.
	 */
	public static int getVarNotBound(CPVarInt [] x, Comparator<Integer> ...comp) {
		int ind = -1;
		for (int i = 0; i < x.length; i++) {
			if (! x[i].isBound()) {
				ind = i;
				break;
			}
		}
		if (ind == -1)  return -1;
		for (int i = ind+1; i < x.length; i++) {
			if (x[i].isBound()) continue;
			for(Comparator<Integer> c: comp) {
				if (c.compare(i, ind) > 0) { //larger
					break;
				} else if (c.compare(i, ind) < 0) { //smaller
					ind = i;
					break;
				}
				//else it is equal and we continue
			}
		}
		return ind;
	}

    /**
     * Find the larger index of a bound variable in x
     * @param x
     * @return the larger index of a bound variable in x, Integer.MIN_VALUE is not such variable
     */
	public static int getMaxBoundVal(CPVarInt [] x) {
		int v = Integer.MIN_VALUE;
		for (int i = 0; i < x.length; i++) {
			if(x[i].isBound()){
				v = Math.max(v, x[i].getValue());
			}
		}
		return v;
	}

    /**
     * Generates a random permutation
     * @param n
     * @param seed for the random number generator
     * @return  a random permutation from 0 to n-1
     */
    public static int[] getRandomPermutation(int n, int seed) {
        int [] perm = new int[n];
        for (int i = 0; i < perm.length; i++) {
            perm[i] = i;
        }
        Random rand = new Random(seed);
        for (int i = 0; i < perm.length; i++) {
            int ind1 = rand.nextInt(n);
            int ind2 = rand.nextInt(n);
            int temp = perm[ind1];
            perm[ind1] = perm[ind2];
            perm[ind2] = temp;
        }
        return perm;
    }

    /**
     *
     * @param x
     * @param permutation a valid permutation of x (all numbers from 0 to x.length-1 present), <br>
     *        permutation[i] represents the index of the entry of x that must come in position i in the permuted array
     * @param <E>
     */
    public static <E> void  applyPermutation(E [] x, int [] permutation) {
        assert (x.length  == permutation.length);
        Object [] objs = new Object[x.length];
        int sum = 0;
        for (int i = 0; i < permutation.length; i++) {
            sum += permutation[i];
            objs[i] = x[i];
        }
        assert (sum == (x.length-1)*(x.length-2)/2); // check the permutation is valid
        for (int i = 0; i < permutation.length; i++) {
           x[i] = (E) objs[permutation[i]];
        }
        objs = null;
    }

    /**
     *
     * @param x
     * @param permutation
     * @see ArrayUtils.applyPermutation
     */
    public static void  applyPermutation(int [] x, int [] permutation) {
        int [] xcopy = Arrays.copyOf(x,x.length);
        for (int i = 0; i < permutation.length; i++) {
           x[i] = xcopy[permutation[i]];
        }
    }

    /**
     *
     * @param w
     * @return the sorting permutation perm of w, i.e. w[perm[0]],w[perm[1]],...,w[perm[w.length-1]] is sorted increasingly
     */
    public static int []  sortPerm(final int [] w) {
        Integer [] perm = new Integer[w.length];
        for (int i = 0; i < perm.length; i++) {
            perm[i] = i;
        }
        Arrays.sort(perm,new Comparator<Integer>() {
            public int compare(Integer o1, Integer o2) {
                return w[o1]-w[o2];
            }
        });
        int [] res = new int[w.length];
        for (int i = 0; i < perm.length; i++) {
            res[i] = perm[i];
        }
        return res;
    }




    /**
     * sorts x increasingly according to the weights w
     * @param x
     * @param w
     */
    public static <E> void  sort(E [] x, int [] w) {
        assert (x.length == w.length);
        applyPermutation(x,sortPerm(w));
    }



}
