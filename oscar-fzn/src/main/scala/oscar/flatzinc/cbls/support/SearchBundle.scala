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
/**
 * @author Gustav Björdal
 * @author Jean-Noël Monette
 */
package oscar.flatzinc.cbls.support

import scala.util.Random

import oscar.cbls.invariants.core.computation.CBLSIntVar
import oscar.cbls.invariants.core.computation.CBLSIntConst
import oscar.cbls.invariants.core.computation.CBLSSetVar
import oscar.cbls.invariants.lib.logic._
import oscar.cbls.invariants.lib.minmax._
import oscar.cbls.invariants.core.computation.Store
import oscar.cbls.invariants.core.computation.SetInvariant.toIntSetVar
import oscar.cbls.constraints.core.ConstraintSystem
import oscar.cbls.objective.{Objective => CBLSObjective}
import oscar.cbls.search.SearchEngine
import oscar.flatzinc.cbls.Log//TODO: Move this somewhere else
import oscar.flatzinc.model.Domain
import oscar.flatzinc.model.DomainRange
import oscar.flatzinc.model.DomainSet


class CBLSIntVarDom(model: Store, val dom: Domain, private var Value: Int, n: String = null)
  extends CBLSIntVar(model, dom.min to dom.max, Value, n) {
  def getDomain():Iterable[Int] = {
    dom match {
      case DomainRange(min, max) => min to max
      case DomainSet(values) => values
    }
  }
  def getRandValue():Int = {
    dom match {
      case DomainRange(min, max) => (min to max)(Random.nextInt(max-min+1))
      case DomainSet(values) => values.toIndexedSeq(Random.nextInt(values.size))
    }
  }
  def domainSize = dom match {
    case DomainRange(min, max) => math.max(max-min+1,0)
    case DomainSet(values) => values.size
  }
}
case class CBLSIntConstDom(ConstValue:Int, override val model:Store = null)
  extends CBLSIntVarDom(model, new DomainRange(ConstValue,ConstValue), ConstValue, "IntConst("+ ConstValue + ")"){
  override def getValue(NewValue:Boolean=false):Int = ConstValue //pour pas avoir de propagation
  override def toString:String = "IntConst("+ ConstValue + ")"
}

object CBLSIntVarDom {
  def apply(model: Store, domain: Domain, value: Int, name: String) = {
    new CBLSIntVarDom(model, domain, value, name)
  }
}

abstract class SearchProcedure extends SearchEngine {
  
  def run(): List[(Long, Int, Int, String)]
  
  
  def showViolatedConstraints(c: ConstraintSystem){
    for(cc <- c.violatedConstraints){
      println(cc + " "+cc.violation.value)
    }
  }
}
class Chain(val a: SearchProcedure, val b: SearchProcedure) extends SearchProcedure {
  def run(): List[(Long, Int, Int, String)] = {
    val sols = a.run()
    sols ++ b.run()
  }
}

class FakeSearch extends SearchProcedure {
  def run() = {List.empty[(Long, Int, Int, String)]}
}

class SimpleLocalSearch(val c: ConstraintSystem, val vars: List[CBLSIntVarDom], val objective: CBLSObjective, val m: Store, val handleSolution: () => Unit, 
    val MaxTimeMilli: Int, val getWatch: () => Long, val log:Log) extends SearchProcedure {
  val violation: Array[CBLSIntVar] = vars.map(c.violation(_)).toArray;
  
  def run(): List[(Long, Int, Int, String)]={
    var solutionList: List[(Long, Int, Int, String)] = List.empty[(Long, Int, Int, String)]
    var improving = 3;
    var lastImproved = 0;
    var i = 0;
    var it = 0;
    log("Starting Simple Local Search")
    log("Starting Violation: "+objective.value)
    if(vars.length>0){
      while(improving > 0 && getWatch() < MaxTimeMilli){
        val currentVar = vars(i);
        if(violation(i).value > 0){
          val k = selectMin(currentVar.getDomain())(k=> objective.assignVal(currentVar,k))
          if(k!=currentVar.value){
            val obj = objective.value
            currentVar := k;
            it+=1;
            if(objective.value < obj){
              lastImproved = i;
              improving = 3
            }
          }
        }
        i+=1;
        if(i==vars.length){
          i=0;
          log("turned around "+objective.value)
        }
        if(i==lastImproved)improving -= 1;
      }
    }
    if(c.violation.value==0){
      handleSolution()
      log("Found Solution in "+getWatch());
    }
    log("Done Simple Local Search")
    log("Ending Violation: "+objective.value)
    log("Nb Moves: "+it)
    
    solutionList
  }
}
class NeighbourhoodSearchOPT(neighbourhoods: Array[Neighbourhood], c: ConstraintSystem, objective: CBLSObjective, realObj: CBLSIntVar, objLB:Int,
    violationWeight: CBLSIntVar, objectiveWeight: CBLSIntVar, MaxTimeMilli: Int, m: Store, getSolution: () => String, 
    handleSolution: () => Unit, getWatch: () => Long, log:Log) extends SearchProcedure {
  //def apply = {
    var solutionList: List[(Long, Int, Int, String)] = List.empty[(Long, Int, Int, String)]
    val searchVariables = neighbourhoods.foldLeft(Set.empty[CBLSIntVar])((acc: Set[CBLSIntVar], x: Neighbourhood) => acc ++ x.getVariables()).toArray
    val variableMap = (0 until searchVariables.length).foldLeft(Map.empty[CBLSIntVar, Int])((acc, x) => acc + (searchVariables(x) -> x));
    val violationArray: Array[CBLSIntVar] = searchVariables.map(c.violation(_)).toArray;
    val tabu: Array[CBLSIntVar] = searchVariables.map(v => CBLSIntVar(m, 0, Int.MaxValue, 0, "Tabu_" + v.name)).toArray;
    val it = CBLSIntVar(m, 0, Int.MaxValue, 1, "it");
    val nonTabuVariables: CBLSSetVar = SelectLEHeapHeap(tabu, it);

    val MaxTenure = (searchVariables.length * 0.6).toInt;
    val MinTenure = 2 + 0 * (searchVariables.length * 0.1).toInt;
    val tenureIncrement = Math.max(1, (MaxTenure - MinTenure) / 10);
    var tenure = MinTenure //searchVariables.length / 8;

    val baseSearchSize = 100;
    val searchFactor = 20;
  //}
    // println(searchVariables.length);
    //m.close();
  override def run(): List[(Long, Int, Int, String)] = {
    var extendedSearch = true;

    var roundsWithoutSat = 0;
    val maxRounds = 2;

    var bestNow = Int.MaxValue;
    var best = bestNow;
    var itSinceBest = 0;
    var numOfMaxTenure = 0;
    var hasBeenSatisfied = false;
    var bestViolation = Int.MaxValue;

    var timeOfBest = getWatch();
    var itOfBalance = 0;
    var minViolationSinceBest = Int.MaxValue;
    var minObjectiveSinceBest = Int.MaxValue;
    var lastMinObjective = Int.MinValue;
    var hasWaited = false;
    objectiveWeight := 0;

    var wait = 0;
    val waitDec = 1;
    def getObjectiveValue(): Int = {
      return c.violation.value + ((objective.objective.value - (c.violation.value * violationWeight.value)) / objectiveWeight.value);
    }

    while (!(c.violation.value==0 && realObj.value==objLB) && getWatch() < MaxTimeMilli) {
      if(it.value%10==0){
        log("it: "+it.value+" violation: "+c.violation.value+" objective: "+realObj.value)
       // log("viol:" + c.violatedConstraints.mkString("\n"))
      }
      //
        //showViolatedConstraints(c);
      //}
      val oldViolation = objective.objective.value;
      val nonTabuSet = nonTabuVariables.value.map(searchVariables(_));
      val bestNeighbour = selectMin(neighbourhoods.map((n: Neighbourhood) =>
        if (extendedSearch) {
          n.getExtendedMinObjective(it.value, nonTabuSet/*, Int.MinValue*/)
        } else {
          n.getMinObjective(it.value, nonTabuSet)
        }))(_.value)
        /*if(bestNeighbour.isInstanceOf[NoMove]){
          log("%%% NO MOVE "+(-bestNeighbour.value))
        }*/
     // log("Move: "+bestNeighbour.toStringMove(it.value))
        bestNeighbour.commit();
      val modifiedVars = bestNeighbour.getModified
      for (v <- modifiedVars) {
        val index = variableMap(v);
        //This could be it.value + tenure + random(tenureIncrement) to introduce more randomness
        //tabu(index) := it.value + tenure;
        tabu(index) := it.value + Math.min(MaxTenure, tenure + RandomGenerator.nextInt(tenureIncrement));
      }
      it ++;
      //println(best + " " + bestNow + " " + bestViolation + " " + c.violation.value + " " + objective.objective.value + " - " + tenure + " " + itSinceBest)
      if (wait > 0) {
        wait -= waitDec;
        hasWaited = true;
        //  println("Waiting : " + wait)
      } else {
        itSinceBest += 1;
      }
      if (!hasBeenSatisfied) {
        //The first priority is to satisfy the problem, then minimize it.

        if (c.violation.value < bestViolation) {
          bestViolation = c.violation.value
          roundsWithoutSat = 0;
          if (c.violation.value == 0) {
            objectiveWeight := 1;
            violationWeight := 1;
            itOfBalance = it.value
            hasBeenSatisfied = true;
            bestNow = objective.objective.value;
            lastMinObjective = bestNow
            best = bestNow;
            timeOfBest = getWatch();
            val solution = getSolution();
            solutionList +:= (getWatch(), best, it.value, solution)
            handleSolution();
          }
          itSinceBest = 0;
          tenure = Math.max(MinTenure, tenure - 1)
          if (tenure == MinTenure) {
            extendedSearch = false;
          }
        }
        if (c.violation.value > bestViolation * 10) {
          extendedSearch = true;
        }
        if (itSinceBest > tenure + baseSearchSize + searchFactor * (tenure / tenureIncrement)) {
          extendedSearch = true;
          itSinceBest = 0;
          tenure = Math.min(MaxTenure, tenure + tenureIncrement);
          if (tenure == MaxTenure) {
            //Wait will be long enough to clear the tabu list.
            wait = tenure + baseSearchSize;
            bestViolation = Int.MaxValue
            tenure = MinTenure;
            roundsWithoutSat += 1;
            if (roundsWithoutSat >= maxRounds) {
              val maxViolatingNeighbourhood = selectMax(neighbourhoods, (n: Neighbourhood) => n.violation())
              maxViolatingNeighbourhood.reset();
              roundsWithoutSat = 0;
            }
          }
        }
      } else {
        // Minimize the problem
        // There are two special cases to look out for here.
        // 1) The violation is within such a small range (compared with the objective) that the violation is ignored by the search.
        //	- This shows when the violation is above 0 for a long time (possibly forever) and the objective is at a "good" low value
        // 2) The violation can grow so quickly that it overshadows the objective (ie the opposite of 1).
        //  - This shows when the violation is 0 for a long time (possibly forever) and the objective does not decrease
        //
        // There is of course also the problem of the dynamic tenure behaving badly but that is waaaaay harder to detect and do something about.
        minViolationSinceBest = Math.min(minViolationSinceBest, c.violation.value)
        minObjectiveSinceBest = Math.min(minObjectiveSinceBest, getObjectiveValue())
        if (getObjectiveValue() < bestNow || (c.violation.value == 0 && getObjectiveValue() < best)) {
          bestNow = getObjectiveValue()
          tenure = Math.max(MinTenure, tenure - 1)
          if (c.violation.value == 0 && bestNow < best) {
            best = bestNow;
            timeOfBest = getWatch();
            itOfBalance = it.value
            minViolationSinceBest = Int.MaxValue
            minObjectiveSinceBest = Int.MaxValue
            lastMinObjective = bestNow;
            tenure = Math.max(MinTenure, tenure / 2)
            val solution = getSolution();
            solutionList +:= (getWatch(), best, it.value, solution)
            handleSolution();
          }
          itSinceBest = 0;
        }
        //println(it.value - itOfBalance + " " + objectiveWeight.value + " " + violationWeight.value)
        if (it.value - itOfBalance > baseSearchSize * 2 && wait == 0) {
          var changed = false;
          if (minViolationSinceBest > 0) { // 1)
            if (objectiveWeight.value > 1) {
              objectiveWeight := objectiveWeight.value / 2;
            } else {
              violationWeight := (violationWeight.value + Math.max(10, Math.abs(minObjectiveSinceBest / 2))).toInt
            }
            changed = true;
          } else if (bestNow <= lastMinObjective) { // 2)
            if (violationWeight.value > 1) {
              violationWeight := violationWeight.value / 2;
            } else {
              objectiveWeight := (objectiveWeight.value + Math.max(10, Math.abs(minObjectiveSinceBest / 2))).toInt
            }
            changed = true;
          }
          if (changed) {
            val minWeight = Math.min(objectiveWeight.value, violationWeight.value)
            objectiveWeight := objectiveWeight.value / minWeight;
            violationWeight := violationWeight.value / minWeight;
            objectiveWeight := Math.min(objectiveWeight.value,
                10000000/Math.max(1,Math.abs(minObjectiveSinceBest)))
            violationWeight := Math.min(violationWeight.value,
                10000000/Math.max(1,Math.abs(minViolationSinceBest))) 
            hasWaited = false
          }
          lastMinObjective = bestNow;
          minViolationSinceBest = Int.MaxValue
          minObjectiveSinceBest = Int.MaxValue

          itOfBalance = it.value;
        }
        if (itSinceBest > tenure + baseSearchSize + searchFactor * (tenure / tenureIncrement)) {
          extendedSearch = true;
          itSinceBest = 0;
          tenure = Math.min(MaxTenure, tenure + tenureIncrement);
          if (tenure == MaxTenure) {
            //Wait will be long enough to clear the tabu list.
            if (getWatch() - timeOfBest > MaxTimeMilli / 4) {
              println("% Reset");
              timeOfBest = getWatch();
              for (n <- neighbourhoods)
                n.reset();
            }
            wait = tenure + baseSearchSize;
            tenure = MinTenure;
            bestNow = getObjectiveValue()
          }
        }
      }

    }
    solutionList +:= (getWatch(), -1, it.value, "Done")
    solutionList;
  }
}

class NeighbourhoodSearchSAT(neighbourhoods: Array[Neighbourhood], c: ConstraintSystem, objective: CBLSObjective,
  violationWeight: CBLSIntVar, objectiveWeight: CBLSIntVar, MaxTimeMilli: Int, m: Store, getSolution: () => String, handleSolution: () => Unit, 
  getWatch: () => Long, log:Log) extends SearchProcedure {

  var solutionList: List[(Long, Int, Int, String)] = List.empty[(Long, Int, Int, String)]
  val searchVariables = neighbourhoods.foldLeft(Set.empty[CBLSIntVar])((acc: Set[CBLSIntVar], x: Neighbourhood) => acc ++ x.getVariables().filterNot(_.isInstanceOf[CBLSIntConstDom])).toArray
  val variableMap = (0 until searchVariables.length).foldLeft(Map.empty[CBLSIntVar, Int])((acc, x) => acc + (searchVariables(x) -> x));
  val violationArray: Array[CBLSIntVar] = searchVariables.map(c.violation(_)).toArray;
  val tabu: Array[CBLSIntVar] = searchVariables.map(v => CBLSIntVar(m, 0, Int.MaxValue, 0, "Tabu_" + v.name)).toArray;
  val it = CBLSIntVar(m, 0, Int.MaxValue, 1, "it");
  val nonTabuVariables: CBLSSetVar = SelectLEHeapHeap(tabu, it);

  val MaxTenure = (searchVariables.length * 0.6).toInt;
  val MinTenure = 2 + 0 * (searchVariables.length * 0.1).toInt;
  val tenureIncrement = Math.max(1, (MaxTenure - MinTenure) / 10);
  var tenure = MinTenure //searchVariables.length / 8;
  //    println(searchVariables.length);
  //m.close();
  //log("Closed Model")
    
  override def run(): List[(Long, Int, Int, String)] = {
    var extendedSearch = true;

    var roundsWithoutSat = 0;
    val maxRounds = 5;

    var bestNow = Int.MaxValue;
    var best = bestNow;
    var itSinceBest = 0;
    var itSinceMaxTenure = 0;
    var bestViolation = Int.MaxValue
    var hasWaited = false;

    val baseSearchSize = 100;
    val searchFactor = 20;
    var wait = 0;
    val waitDec = 1;
    def getObjectiveValue(): Int = {
      return c.violation.value + ((objective.objective.value - (c.violation.value * violationWeight.value)) / objectiveWeight.value);
    }
    while (c.violation.value != 0 && getWatch() < MaxTimeMilli) {
      if(it.value%10==0){
        log("it: "+it.value+" violation: "+c.violation.value)
      }
      val oldViolation = objective.objective.value;
      val nonTabuSet = nonTabuVariables.value.map(searchVariables(_));
      val bestNeighbour = selectMin(neighbourhoods.map((n: Neighbourhood) =>
        if (extendedSearch) {
          n.getExtendedMinObjective(it.value, nonTabuSet/*, bestNow*/)
        } else {
          n.getMinObjective(it.value, nonTabuSet)
        }))(_.value)
      bestNeighbour.commit()
      val modifiedVars = bestNeighbour.getModified;
      for (v <- modifiedVars) {
        val index = variableMap(v);
        //This could be it.value + tenure + random(tenureIncrement) to introduce more randomness
        //tabu(index) := it.value + tenure;
        tabu(index) := it.value + Math.min(MaxTenure, tenure + RandomGenerator.nextInt(tenureIncrement));
      }
      it ++;
      //println(best + " " + bestNow + " " + bestViolation + " " + c.violation.value + " " + objective.objective.value + " - " + tenure + " " + itSinceBest)

      if (wait > 0) {
        wait -= waitDec;
        hasWaited = true;
      } else {
        itSinceBest += 1;
      }
      if (c.violation.value < bestViolation) {
        bestViolation = c.violation.value
        if (c.violation.value == 0) {
          bestNow = objective.objective.value;
          best = bestNow;
          val solution = getSolution();
          solutionList +:= (getWatch(), best, it.value, solution)
          handleSolution();
        }
        itSinceBest = 0;
        tenure = Math.max(MinTenure, tenure - 1)
        if (tenure == MinTenure) {
          extendedSearch = false;
        }
      }
      if (c.violation.value > bestViolation * 10) {
        extendedSearch = true;
      }
      if (itSinceBest > tenure + baseSearchSize + searchFactor * (tenure / tenureIncrement)) {
        extendedSearch = true;
        itSinceBest = 0;
        tenure = Math.min(MaxTenure, tenure + tenureIncrement);
        if (tenure == MaxTenure) {
          //Wait will be long enough to clear the tabu list.
          wait = tenure + baseSearchSize;
          bestViolation = Int.MaxValue
          tenure = MinTenure;
          roundsWithoutSat += 1;
          println("% "+roundsWithoutSat)
          if (roundsWithoutSat >= maxRounds) {
            println("% ------------------------------------------------")
            //val maxViolating = selectMax(neighbourhoods, (n: Neighbourhood) => n.violation())
            //maxViolating.reset();
            for (n <- neighbourhoods)
              n.reset();
            roundsWithoutSat = 0;
            bestViolation = c.violation.value
            println("% "+bestViolation);
          }
        }
      }
    }
    solutionList +:= (getWatch(), -1, it.value, "Done")
    solutionList;
  }
}