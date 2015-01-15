/*******************************************************************************
 * OscaR is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 2.1 of the License, or
 * (at your option) any later version.
 *   
 * OscaR is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License  for more details.
 *   
 * You should have received a copy of the GNU Lesser General Public License along with OscaR.
 * If not, see http://www.gnu.org/licenses/lgpl-3.0.en.html
 ******************************************************************************/

package oscar.cp.modeling

import oscar.cp._
import oscar.algo.search.Branching
import oscar.algo.search.BranchingUtils
import oscar.cp.scheduling.search.SetTimesBranching
import oscar.cp.scheduling.search.RankBranching
import oscar.cp.searches.BinaryDomainSplitBranching
import oscar.cp.searches.BinarySetBranching
import oscar.cp.searches.BinaryMaxDegreeBranching
import oscar.cp.searches.BinaryBranching
import oscar.cp.searches.BinaryStaticOrderBranching
import oscar.cp.searches.BinaryFirstFailBranching

/**
 * @author Pierre Schaus pschaus@gmail.com
 */
trait Branchings extends BranchingUtils {
  
  def binary(vars: Seq[_ <: CPIntVar], varHeuris: (CPIntVar => Int), valHeuris: (CPIntVar => Int) = minVal): Branching = {
    new BinaryBranching(vars.toArray,varHeuris,valHeuris)
  }
 
  /**
   * Binary Search on the decision variables vars with fixed static ordering.
   * The next variable to assign is the first unbound variable in vars.
   * @param vars: the array of variables to assign during the search
   * @param valHeuris: gives the value v to try on left branch for the chosen variable, this value is removed on the right branch
   */  
  def binaryStatic(vars: Seq[_ <: CPIntVar], valHeuris: (CPIntVar => Int) = minVal): Branching = new BinaryStaticOrderBranching(vars.toArray,valHeuris)

  /**
   * Binary First Fail (min dom size) on the decision variables vars.
   * @param vars: the array of variables to assign during the search
   * @param valHeuris: gives the value v to try on left branch for the chosen variable, this value is removed on the right branch
   */
  def binaryFirstFail(x: Seq[CPIntVar], valHeuris: (CPIntVar => Int) = minVal): Branching = new BinaryFirstFailBranching(x.toArray, valHeuris)

  /**
   * Binary search on the decision variables vars, selecting first the variables having the max number of propagation methods attached to it.
   */
  def binaryMaxDegree(x: Seq[_ <: CPIntVar]): Branching = new BinaryMaxDegreeBranching(x.toArray)

  /**
   * Binary search on the decision variables vars, splitting the domain of the selected variable on the
   * median of the values (left : <= median, right : > median)
   */
  def binarySplit(x: Seq[_ <: CPIntervalVar], varHeuris: (CPIntervalVar => Int) = minVar, valHeuris: (CPIntervalVar => Int) = (x: CPIntervalVar) => (x.min + x.max) / 2): Branching = {
    val xa = x.toArray
    new BinaryDomainSplitBranching(xa, varHeuris, valHeuris)
  }
  
  /**
   * Binary Search on the set variable
   * forcing an arbitrary on the left, and removing it on the right until the variable is bound
   */   
  def binary(x: CPSetVar): Branching = {
    new BinarySetBranching(x)
  }
  
  /**
   * set times heuristic (for discrete resources) 
   * see: Time- versus-capacity compromises in project scheduling. (Le Pape et al.). 1994. 
   */  
  def setTimes(starts: IndexedSeq[_ <: CPIntervalVar], durations: IndexedSeq[_ <: CPIntervalVar], ends: IndexedSeq[_ <: CPIntervalVar], tieBreaker: Int => Int = (i: Int) => i): Branching = new SetTimesBranching(starts, durations, ends, tieBreaker) 
  
  /**
   * rank heuristic (for unary resources)
   */  
  def rank[T](starts: IndexedSeq[CPIntVar], durations: IndexedSeq[CPIntVar], ends: IndexedSeq[CPIntVar], by: Int => T)(implicit orderer: T => Ordered[T]): Branching = new RankBranching(starts,durations, ends, by) 
  
  def rank[T](starts: IndexedSeq[CPIntVar], durations: IndexedSeq[CPIntVar], ends: IndexedSeq[CPIntVar]): Branching = {
    rank(starts,durations,ends,(i: Int) => ends(i).max)
  }
}
