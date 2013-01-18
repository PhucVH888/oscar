package oscar.cbls


import oscar.cbls.invariants.core.computation.IntVar
import oscar.cbls.invariants.core.computation.IntSetVar

/**This package proposes an interface to the primitive of the CBLS engine.
 * that is as similar as possible to the one exhibited by the other engines of OScar.
 */
package object modeling extends Constraints with ClusterInvariants
with ComplexLogicInvariants
with AccessInvariants
with MinMaxInvariants
with NumericInvariants
with SetInvariants {

  type LSSolver = oscar.cbls.invariants.core.computation.Model
  
  type LSVarInt = IntVar
  
  type LSVarSetInt = IntSetVar

}
