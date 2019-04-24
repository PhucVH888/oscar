package oscar.cbls.business.geometry.model

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

import org.locationtech.jts.geom.{Coordinate, Geometry, Point}
import oscar.cbls.Store
import oscar.cbls.core.computation._
import oscar.cbls.core.propagation.{Checker, PropagationElement}

class GeometryValue(val geometry:Geometry)(
  var inputCentroid:Option[Point] = None,
  var inputOverApproximatingRadius:Option[Double] = None) {

  //une value dérivée est soit:
  // donnée
  // Calculée à partir de geometry
  //on ne considère pas le cas de calculé à partir des va leurs d'origines; on doit alors juste la doner en entrée.

  def centroid:Point = inputCentroid match{
    case Some(c) => c
    case None =>
      val c = geometry.getCentroid
      inputCentroid = Some(c)
      c
  }

  def overApproximatingRadius:Double = inputOverApproximatingRadius match{
    case Some(r) => r
    case None =>
      val c = centroid

      var maxD:Double = 0
      for(coordinate:Coordinate <- geometry.getCoordinates()){
        val d = c.distance(new Point(coordinate))
        if (d < maxD) maxD = d
      }
      inputOverApproximatingRadius = Some(maxD)
      maxD
  }
}

class CBLSGeometryVar(store: Store,
                      initialValue: GeometryValue,
                      givenName: String = null)
  extends CBLSAtomicVar[GeometryValue](store: Store,
    initialValue,
    givenName: String ){

  def createClone:CBLSGeometryVar = {
    val clone = new CBLSGeometryVar(
      store,
      this.value,
      "clone of " + this.name)

    clone <== this
    clone
  }

  def <== (g: ChangingAtomicValue[GeometryValue]): Unit ={
    new IdentityGeometry(this, g)
  }


  override def performNotificationToListeningInv(inv: PropagationElement, id: Int, oldVal: GeometryValue, newVal: GeometryValue): Unit = {
    val target = inv.asInstanceOf[GeometryNotificationTarget]
    target.notifyGeometryChange(this,id,oldVal,newVal)
  }

}

/** an invariant that is the identity function
  * @author renaud.delandtsheer@cetic.be
  */
class IdentityGeometry(toValue:CBLSGeometryVar, fromValue:ChangingAtomicValue[GeometryValue])
  extends Invariant with GeometryNotificationTarget{

  registerStaticAndDynamicDependency(fromValue)
  toValue.setDefiningInvariant(this)
  finishInitialization()

  toValue := fromValue.value


  override def notifyGeometryChange(a: ChangingAtomicValue[GeometryValue], id: Int, oldVal: GeometryValue, newVal: GeometryValue): Unit = {
    toValue := newVal
  }

  override def checkInternals(c:Checker){
    c.check(toValue.value == fromValue.value)
  }
}

trait GeometryNotificationTarget{
  def notifyGeometryChange(a:ChangingAtomicValue[GeometryValue],id:Int,oldVal:GeometryValue,newVal:GeometryValue)
}

class CBLSGeometryConst(store:Store, override val value:GeometryValue, givenName:String = "")
  extends CBLSAtomicConst[GeometryValue](value){
  override def name = if (givenName == null) value.toString else givenName
  override def toString:String = if (givenName == null) value.toString else givenName
}

class CBLSGeometryInvariant(store:Store,
                            initialValue:GeometryValue)
  extends AtomicInvariant[GeometryValue](initialValue){

  def createClone:CBLSGeometryVar = {
    val clone = new CBLSGeometryVar(
      store,
      this.value,
      "clone of " + this.name)

    clone <== this
    clone
  }


  override def performNotificationToListeningInv(inv: PropagationElement, id: Int, oldVal: GeometryValue, newVal: GeometryValue): Unit = {
    val target = inv.asInstanceOf[GeometryNotificationTarget]
    target.notifyGeometryChange(this,id,oldVal,newVal)
  }
}



