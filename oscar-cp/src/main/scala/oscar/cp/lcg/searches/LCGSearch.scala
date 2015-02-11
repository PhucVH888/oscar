package oscar.cp.lcg.searches

import oscar.algo.search.DFSearchNode
import oscar.algo.search.DFSearch
import oscar.cp.lcg.core.LCGStore
import oscar.cp.core.CPStore
import oscar.cp.core.Constraint
import oscar.cp.core.CPOutcome
import oscar.cp.core.CPOutcome._
import oscar.cp.lcg.core.LCGSolver
import oscar.cp.lcg.core.LiftedBoolean
import oscar.cp.lcg.core.Unassigned
import oscar.cp.lcg.core.False
import oscar.cp.lcg.core.True

/** @author Renaud Hartert ren.hartert@gmail.com */
class LCGSearch(node: DFSearchNode, cpStore: CPStore, lcgStore: LCGStore) {

  // LCG constraint
  private[this] val lcgStoreConstraint: Constraint = new LCGSolver(cpStore, lcgStore)

  private[this] var depth: Int = 0
  private[this] var _nConflicts: Int = 0
  private[this] var _nSolutions: Int = 0
  private[this] var _nNodes: Int = 0
  private[this] var solutionActions = List.empty[() => Unit]
  private[this] var failureActions = List.empty[() => Unit]

  /** Returns the number of backtracks in the previous search */
  final def nConflicts: Int = _nConflicts

  /** Returns the number of solutions found in the previous search */
  final def nSolutions: Int = _nSolutions

  /** Returns the number nodes explored in the previous search */
  final def nNodes: Int = _nNodes

  /** Adds an action to execute when a failed node is found */
  final def onFailure(action: => Unit): Unit = failureActions = (() => action) :: failureActions

  /** Adds an action to execute when a solution node is found */
  final def onSolution(action: => Unit): Unit = solutionActions = (() => action) :: solutionActions

  /** Clear all actions executed when a solution node is found */
  final def clearOnSolution(): Unit = solutionActions = Nil

  /** Clear all actions executed when a failed node is found */
  final def clearOnFailure(): Unit = failureActions = Nil

  final def search(heuristic: Heuristic, stopCondition: () => Boolean): Unit = {

    depth = 0
    _nConflicts = 0
    _nSolutions = 0
    _nNodes = 0

    var state: LiftedBoolean = Unassigned
    var stop = false

    while (!stop) {

      // Propagation
      val outcome = cpStore.propagate(lcgStoreConstraint)

      // Handle conflict
      if (outcome == Failure) {
        _nConflicts += 1
        failureActions.foreach(_())
        if (depth == 0) {
          // Unfeasible
          stop = true
          state = False
        } else {
          // Backjump
          val level = lcgStore.backtrackLvl
          println("backjump from " + depth + " to " + level)
          while (depth > level) {
            depth -= 1 // backtrack
            node.pop
          }
        }
      } // No conflict
      else {

        val decision = heuristic.decision

        // New solution
        if (decision == null) {
          _nSolutions += 1
          solutionActions.foreach(_())
          stop = true
          state = True
        } 
        // Stop condition
        else if (stopCondition()) {
          stop = true
        } 
        // Apply decision
        else {
          // Expand
          _nNodes += 1
          depth += 1
          node.pushState()
          // Apply decision
          lcgStore.newLevel()
          println("\nlevel " + depth)
          decision()
        }
      }
    }

    // Pop the remaining nodes
    while (depth > 0) {
      node.pop
      depth -= 1
    }
    
    state
  }
}