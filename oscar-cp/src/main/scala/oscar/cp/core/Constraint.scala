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

package oscar.cp.core

import oscar.algo.reversible.ReversibleBoolean
import oscar.cp.constraints.Garded
import scala.collection.mutable.ArrayBuffer
import oscar.algo.reversible.MagicBoolean
import oscar.cp.core.variables.CPSetVar
import oscar.cp.core.variables.CPBoolVar
import oscar.cp.core.variables.CPIntVar
import scala.collection.JavaConversions.mapAsScalaMap
import oscar.cp.core.delta.DeltaSetVar
import oscar.cp.core.delta.DeltaIntVar
import oscar.cp.core.delta.Delta




class Watcher {
  def shouldEnqueue(): Boolean = true
}

/**
 * Abstract class extended by any CP constraints
 * @author Pierre Schaus pschaus@gmail.com
 * @author Renaud Hartert ren.hartert@gmail.com
 */
abstract class Constraint(val s: CPStore, val name: String = "cons") {

  private[this] val active = new ReversibleBoolean(s,true)
  private[this] val inQueue = new MagicBoolean(s, false)
  
  // FIXME variables should have an id 
  val snapshotsVarSet = new java.util.HashMap[CPSetVar, DeltaSetVar]
  
  // Snapshots
  private[this] var snapshots = new Array[Delta](10)
  private[this] var nSnapshots = 0
  private[this] var _mustSnapshot = false
  
  final def registerDelta(delta: DeltaIntVar): Unit = {
    if (nSnapshots == snapshots.length) growSnapshots()
    snapshots(nSnapshots) = delta
    nSnapshots += 1   
    delta.update() 
    if (!_mustSnapshot) { s.onPop { updateSnapshots() }; _mustSnapshot = true }
  }

  @inline private def growSnapshots(): Unit = {
    val newStack = new Array[Delta](nSnapshots*2)
    System.arraycopy(snapshots, 0, newStack, 0, nSnapshots)
    snapshots = newStack
  }
   
  @inline private def updateSnapshots(): Unit = {
    var i = nSnapshots
    while (i > 0) {
      i -= 1
      snapshots(i).update()
    }
  }

  def addSnapshot(x: CPSetVar): Unit = {
    snapshotsVarSet(x) = new DeltaSetVar(x) // FIXME avoid the implicit cast
    
    if (nSnapshots == snapshots.length) growSnapshots()
    snapshots(nSnapshots) = snapshotsVarSet(x) // FIXME avoid the implicit cast
    nSnapshots += 1  
    
    snapshotsVarSet(x).update() // FIXME avoid the implicit cast
    if (!_mustSnapshot) {
      s.onPop { updateSnapshots() }
      _mustSnapshot = true
    }    
  } 
  
  @inline protected def snapshotVarSet(): Unit = {
    var i = 0
    while (i < nSnapshots) {
      snapshots(i).update()
      i += 1
    } 
  } 

  private[this] var priorL2 = CPStore.MaxPriorityL2 - 2
  private[this] var priorBindL1 = CPStore.MaxPriorityL1 - 1
  private[this] var priorBoundsL1 = CPStore.MaxPriorityL1 - 2
  private[this] var priorRemoveL1 = CPStore.MaxPriorityL1 - 2
  private[this] var priorRequireL1 = CPStore.MaxPriorityL1 - 1
  private[this] var priorExcludeL1 = CPStore.MaxPriorityL1 - 2

  // Set to true when it is currently executing the propagate method
  private[this] var _inPropagate = false

  /**
   * True if the constraint is idempotent i.e. calling two times propagate is useless if no other changes occurred
   * sigma(store) = sigma(sigma(store))
   */
  private[this] var _idempotent = false
  @inline final def idempotent: Boolean = _idempotent
  final def idempotent_=(b: Boolean): Unit = _idempotent = b

  /**
   * @return true if it is currently executing the propagate method.
   */
  @inline final def inPropagate(): Boolean = _inPropagate
  
  
  
  @inline final def isEnqueuable: Boolean = {
    active.value && !inQueue.value && (!_inPropagate || !_idempotent)
  }
  

  /**
   * @param b
   * @return a garded version of this constraint i.e. that will only be posted when b is true
   */
  def when(b: CPBoolVar): Constraint = new Garded(b, this, true)

  /**
   * @param b
   * @return a garded version of this constraint i.e. that will only be posted when b is false
   */
  def whenNot(b: CPBoolVar) = new Garded(b, this, false)

  override def toString: String = "constraint:" + name

  /**
   * setup the constraint, typically this is the place where
   * - the constraint registers to modifications of the domains of variables in its scope
   * - a first consistency check and propagation is done
   * @param l
   * @return The outcome of the first propagation and consistency check
   */
  def setup(l: CPPropagStrength): CPOutcome

  final def priorityL2: Int = priorL2
  final def priorityBindL1: Int = priorBindL1
  final def priorityRemoveL1: Int = priorRemoveL1
  final def priorityBoundsL1: Int = priorBoundsL1
  final def priorityRequireL1: Int = priorRequireL1
  final def priorityExcludeL1: Int = priorExcludeL1
  
  final def priorityL2_=(priority: Int): Unit = priorL2 = priority
  final def priorityBindL1_=(priority: Int): Unit = priorBindL1 = checkL1Prior(priority)
  final def priorityRemoveL1_=(priority: Int): Unit = priorRemoveL1 = checkL1Prior(priority)
  final def priorityBoundsL1_=(priority: Int): Unit = priorBoundsL1 = checkL1Prior(priority)
  final def priorityRequireL1_=(priority: Int): Unit = priorRequireL1 = checkL1Prior(priority)
  final def priorityExcludeL1_=(priority: Int): Unit = priorExcludeL1 = checkL1Prior(priority)
  
  @inline private def checkL1Prior(priority: Int): Int = {
    if (priority > CPStore.MaxPriorityL1) CPStore.MaxPriorityL1
    else if (priority < 0) 0
    else priority
  }

  /**
   * @return true if the constraint is still active
   */
  final def isActive = active.value

  /**
   * @return true if the constraint is still in the propagation queue, false otherwise
   */
  final def isInQueue = inQueue.value

  /**
   * Disable the constraint such that it is not propagated any more (will not enter into the propagation queue).
   * Note that this state is reversible (trailable).
   */
  def deactivate(): Unit = active.setFalse()

  /**
   * Reactivate the constraint
   */
  def activate(): Unit = active.setTrue()

  /**
   * Propagation method of Level L2 that is called if variable x has asked to do so with
   * any one of these methods: <br>
   * - callPropagateWhenMaxChanges <br>
   * - callPropagateWhenMinChanges <br>
   * - callPropagateWhenDomainChanges <br>
   * - callPropagateWhenBind <br>
   * The (variable,domain) change that has triggered the call to propagate depends of course
   * on which of the method(s) above was used
   * @return the outcome i.e. Failure, Success or Suspend
   */
  def propagate(): CPOutcome = CPOutcome.Suspend

  /**
   * Propagation method of Level L1 that is called if variable x has asked to do so
   * with the method call x.callUpdateBoundsIdxWhenBoundsChange(this,idx)
   * @param x has a new minimum and/or maximum value in its domain since last call
   * @return the outcome i.e. Failure, Success or Suspend
   */
  def updateBounds(x: CPIntVar) = CPOutcome.Suspend

  /**
   * Propagation method of Level L1 that is called if variable x has asked to do so
   * with the method call x.callUpdateBoundsIdxWhenBoundsChange(this,idx)
   * @param x has a new minimum and/or maximum value in its domain since last call
   * @param idx is a key value that was given to callUpdateMaxIdxWhenMaxChanges(x,this,idx) attached to variable x.
   *        This is typically used to retrieve the index of x in an array of variables in constant time
   * @return the outcome i.e. Failure, Success or Suspend
   */
  def updateBoundsIdx(x: CPIntVar, idx: Int) = CPOutcome.Suspend

  /**
   * Propagation method of Level L1 that is called if variable x has asked to do so
   * with the method call x.callValBind(this)
   * @param x is bind
   * @return the outcome i.e. Failure, Success or Suspend
   */
  def valBind(x: CPIntVar) = CPOutcome.Suspend

  /**
   * Propagation method of Level L1 that is called if variable x has asked to do so
   * with the method call x.callValBindIdx(this,idx)
   * @param x is bind
   * @param idx is a key value that was given to callValBindIdx(x,idx) attached to variable x.
   *        This is typically used to retrieve the index of x in an array of variables in constant time
   * @return the outcome i.e. Failure, Success or Suspend
   */
  def valBindIdx(x: CPIntVar, idx: Int) = CPOutcome.Suspend

  /**
   * Propagation method of Level L1 that is called if variable x has asked to do so
   * with the method call x.callValRemoveWhenValueRemoved(this)
   * @param value is a value that has been removed from the domain of x since last call
   * @return the outcome i.e. Failure, Success or Suspend
   */
  def valRemove(x: CPIntVar, value: Int) = CPOutcome.Suspend

  /**
   * Propagation method of Level L1 that is called if variable x has asked to do so
   * with the method call x.callValRemoveIdxWhenValueRemoved(this)
   * @param value is a value that has been removed from the domain of x since last call
   * @param idx is a key value that was given to callValBind(x,idx) attached to variable x.
   *        This is typically used to retrieve the index of x in an array of variables in constant time
   * @return the outcome i.e. Failure, Success or Suspend
   */
  def valRemoveIdx(x: CPIntVar, idx: Int, value: Int) = CPOutcome.Suspend

  /**
   * Propagation method of Level L1 that is called if variable x has asked to do so
   * with the method call x.callValRequiredWhenValueRequired(this)
   * @param val is a value that has been put as required in the domain of x since last call
   * @return the outcome i.e. Failure, Success or Suspend
   */
  def valRequired(x: CPSetVar, value: Int) = CPOutcome.Suspend

  /**
   * Propagation method of Level L1 that is called if variable x has asked to do so
   * with the method call x.callValRequiredIdxWhenValueRemovedIdx(this)
   * @param val is a value that has been put as required in the domain of x since last call
   * @param idx is a key value that was given to callValRequiredIdxWhenValueRemovedIdx(x,idx) attached to variable x.
   *        This is typically used to retrieve the index of x in an array of variables in constant time
   * @return the outcome i.e. Failure, Success or Suspend
   */
  def valRequiredIdx(x: CPSetVar, idx: Int, value: Int) = CPOutcome.Suspend

  /**
   * Propagation method of Level L1 that is called if variable x has asked to do so
   * with the method call x.callValExcludeddWhenValueRequired(this)
   * @param val is a value that has been excluded in the domain of x since last call
   * @return the outcome i.e. Failure, Success or Suspend
   */
  def valExcluded(x: CPSetVar, value: Int) = CPOutcome.Suspend

  /**
   * Propagation method of Level L1 that is called if variable x has asked to do so
   * with the method call x.callValExcludedIdxWhenValueRemovedIdx(this)
   * @param val is a value that has been put as required in the domain of x since last call
   * @param idx is a key value that was given to callValExcludedIdxWhenValueRemovedIdx(x,idx) attached to variable x.
   *        This is typically used to retrieve the index of x in an array of variables in constant time
   * @return the outcome i.e. Failure, Success or Suspend
   */
  def valExcludedIdx(x: CPSetVar, idx: Int, value: Int) = CPOutcome.Suspend

  def execute(): CPOutcome = {
    inQueue.value = false
    _inPropagate = true
    val oc = propagate()
    if (oc != CPOutcome.Failure) {
      updateSnapshots()
    }
    _inPropagate = false
    if (oc == CPOutcome.Success) {
      deactivate()
    }
    oc
  }

  @inline private[cp] def setInQueue(): Unit = inQueue.setTrue()
}
