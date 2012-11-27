/*******************************************************************************
 * This file is part of OscaR (Scala in OR).
 *  
 * OscaR is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.1 of the License, or
 * (at your option) any later version.
 * 
 * OscaR is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OscaR.
 * If not, see http://www.gnu.org/licenses/gpl-3.0.html
 ******************************************************************************/

package oscar.des.engine

import oscar.invariants._
//import scala.collection.mutable._
import java.util.PriorityQueue
import scala.util.continuations._

/**
 * @author Sebastien Mouthuy
 */
class PQCounter[A<%Ordered[A]](v: A) extends Signal[A](v){
	
  val pq = new PriorityQueue[WaitEvent[A]]
  
  def addEvent(ev: WaitEvent[A]) {
    require(ev.time >= this())
    pq.add(ev)
  }
  def removeEvent(ev: WaitEvent[A]) { pq.remove(ev)}
  override def ===(i:A) = {
    new PQCounterCond(this,i)
  }
  def nonEmpty = pq.size() > 0
  def generate(){
    while (nonEmpty){
      generateNext()
    }
  }
  def generateNext(){
    val ev = next
    println("counter " + ev.time)
    ev.process
  }
  def next = {
    val res = pq.poll()
    this emit res.time    
    res
  }
}

class PQCounterCond[A<%Ordered[A]](pqc: PQCounter[A], v: A) extends Occuring[A]{
  def foreach(f2:A=>Boolean) = {
    val a = new WaitEvent[A](v,f2)
    pqc addEvent(a)
    new PQEventBlock(pqc,a)
  }
}


class PQEventBlock[A<%Ordered[A]](pqc: PQCounter[A], ev: WaitEvent[A]) extends Reaction[A]({_=>false}, pqc){
  
  def dispose(){
    pqc.removeEvent(ev)
  }
}

object Counter{
  
  def main(args: Array[String]){
    
    val x = new VarInt(5)
    val y = new VarInt(8)
    
    val c = new PQCounter(0)
    
    
    reset{    
      waitFor[Int,Unit](x)
      println("500")
    }
    
    whenever ( c === 5 ){w:Int =>
      println("super")
    }
    println("here")
    x := 500
    
    
    c.generate()
  }
}
