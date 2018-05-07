package oscar.cbls.core.propagation.draft


import oscar.cbls.algo.dag.DAGNode
import oscar.cbls.algo.dll.{DPFDLLStorageElement, DelayedPermaFilteredDoublyLinkedList}
import oscar.cbls.algo.quick.QList

trait PseudoPropagationElement {
  def registerStaticallyListeningElement(listeningElement: PropagationElement){}
  def registerDynamicallyListeningElement(listeningElement:PropagationElement,
                                          id:Int,
                                          determiningPermanentDependency:Boolean): DPFDLLStorageElement[_] = null
}

trait VaryingListenedPE extends PropagationElement{

  var permanentListenedPE:QList[PropagationElement] = null

  override protected[propagation] def registerPermanentDynamicDependencyListeningSide(listenedElement:PropagationElement){
    dynamicallyListenedElements.addElem(listenedElement)
    permanentListenedPE = QList(listenedElement,permanentListenedPE)
  }

  protected[propagation] def registerTemporaryDynamicDependencyListeningSide(listenedElement:PropagationElement):DPFDLLStorageElement[_] = {
    dynamicallyListenedElements.addElem(listenedElement)
  }

  val dynamicallyListenedElements: DelayedPermaFilteredDoublyLinkedList[PropagationElement]
  = new DelayedPermaFilteredDoublyLinkedList[PropagationElement]

  override protected def initiateDAGPrecedingNodesAfterSCCDefinition(){

    def filterForListened(listened: PropagationElement,
                          injector: (() => Unit),
                          isStillValid: (() => Boolean)){
      if (scc == listened.scc) {
        scc.registerOrCompleteWaitingDependency(listened, this, injector, isStillValid)
      }
    }
    getDAGPrecedingNodes = dynamicallyListenedElements.delayedPermaFilter(filterForListened)
  }

  override protected[propagation] def dropUselessGraphAfterClose(): Unit ={
    staticallyListenedElements = null
    staticallyListeningElements = null
  }
}

abstract class PropagationElement() extends DAGNode with PseudoPropagationElement{

  var uniqueID = -1 //DAG node already have this kind of stuff
  var isScheduled:Boolean = false
  var schedulingHandler:SimpleSchedulingHandler = null
  var model:PropagationStructure = null

  private[this] var myScc:StronglyConnectedComponent = null
  //We have a getter because a specific setter is define herebelow
  def scc:StronglyConnectedComponent = myScc

  var layer:Int = -1

  // //////////////////////////////////////////////////////////////////////
  //static propagation graph
  protected[this] var staticallyListeningElements:QList[PropagationElement] = null
  protected[this] var staticallyListenedElements:QList[PropagationElement] = null

  def addStaticallyListenedElement(listened:PropagationElement): Unit ={
    staticallyListenedElements = QList(listened,staticallyListenedElements)
  }

  /**
    * listeningElement call this method to express that
    * they might register with dynamic dependency to the invocation target
    * @param listeningElement
    */
  override def registerStaticallyListeningElement(listeningElement: PropagationElement) {
    staticallyListeningElements = QList(listeningElement,staticallyListeningElements)
    listeningElement.addStaticallyListenedElement(this)
  }

  // //////////////////////////////////////////////////////////////////////
  //dynamic propagation graph

  private[this] val dynamicallyListeningElements: DelayedPermaFilteredDoublyLinkedList[(PropagationElement, Int)]
  = new DelayedPermaFilteredDoublyLinkedList[(PropagationElement, Int)]

  protected[propagation] def registerTemporaryDynamicDependencyListenedSide(listeningElement:VaryingListenedPE,
                                                                            id:Int):  DPFDLLStorageElement[_] = {
    this.dynamicallyListeningElements.addElem((listeningElement,id))
  }

  protected[propagation] def registerPermanentDynamicDependencyListenedSide(listeningElement:PropagationElement ,
                                                                            id:Int) {
    this.dynamicallyListeningElements.addElem((listeningElement,id))
  }

  protected[propagation] def registerPermanentDynamicDependencyListeningSide(listenedElement:PropagationElement): Unit ={
    //nothing to do here, we always listen to the same elements,
    // so our dependencies are captures by statically listened elements
    //dynamic dependencies PE will need to do womthing here, actually and that's why there is this method
    //this call is not going to be performed many times because it is about permanent dependency,
    // so only called at startup and never during search
  }

  protected def registerPermanentDynamicDependency(listeningElement:PropagationElement,id:Int): Unit ={
    registerPermanentDynamicDependencyListenedSide(listeningElement,id)
    listeningElement.registerPermanentDynamicDependencyListeningSide(this)
  }

  protected def registerTemporaryDynamicDependency(listeningElement:VaryingListenedPE,id:Int): KeyForDynamicDependencyRemoval ={
    new KeyForDynamicDependencyRemoval(
      registerTemporaryDynamicDependencyListenedSide(listeningElement:VaryingListenedPE,id:Int),
      listeningElement.registerTemporaryDynamicDependencyListeningSide(this))
  }

  // //////////////////////////////////////////////////////////////////////
  //DAG stuff, for SCC sort

  def compare(that: DAGNode): Int = {
    assert(this.model == that.asInstanceOf[PropagationElement].model)
    assert(this.uniqueID != -1, "cannot compare non-registered PropagationElements this: [" + this + "] that: [" + that + "]")
    assert(that.uniqueID != -1, "cannot compare non-registered PropagationElements this: [" + this + "] that: [" + that + "]")
    this.uniqueID - that.uniqueID
  }

  final var getDAGPrecedingNodes: Iterable[DAGNode] = null
  final var getDAGSucceedingNodes: Iterable[DAGNode] = null

  def scc_=(scc:StronglyConnectedComponent): Unit = {
    require(this.scc == null)
    this.scc = scc
    initiateDAGSucceedingNodesAfterSccDefinition()
    initiateDAGPrecedingNodesAfterSCCDefinition()
  }

  protected def initiateDAGSucceedingNodesAfterSccDefinition() {
    //we have to create the SCC injectors that will maintain the filtered Perma filter of nodes in the same SCC
    //for the listening side
    def filterForListening(listeningAndPayload: (PropagationElement, Int),
                           injector: (() => Unit),
                           isStillValid: (() => Boolean)) {
      val listening = listeningAndPayload._1
      if (scc == listening.scc) {
        scc.registerOrCompleteWaitingDependency(this, listening, injector, isStillValid)
      }
    }
    getDAGSucceedingNodes = dynamicallyListeningElements.delayedPermaFilter(filterForListening, (e) => e._1)
  }

  protected def initiateDAGPrecedingNodesAfterSCCDefinition(){
    getDAGPrecedingNodes = staticallyListenedElements.filter(_.scc == scc)
  }

  // ////////////////////////////////////////////////////////////////////////
  // to spare on memory (since we have somewhat memory consuming PE
  def dropUselessGraphAfterClose(): Unit ={
    staticallyListenedElements = null
  }

  // ////////////////////////////////////////////////////////////////////////
  // api about scheduling and propagation

  def scheduleMyselfForPropagation(): Unit ={
    if(!isScheduled){
      isScheduled = true
      schedulingHandler.schedulePEForPropagation(this)
    }
  }

  def reScheduleIfScheduled(): Unit ={
    if(isScheduled){
      schedulingHandler.schedulePEForPropagation(this)
    }
  }

  def triggerPropagation(){
    model.triggerPropagation(this)
  }

  final def propagate(){
    if(isScheduled) {
      isScheduled = false
      performPropagation()
    }
  }

  protected def performPropagation():Unit = ???
}


/**
  * This is the node type to be used for bulking
  * @author renaud.delandtsheer@cetic.be
  */
trait BulkPropagationElement extends PropagationElement {

  override protected def initiateDynamicGraphFromSameComponentListened(stronglyConnectedComponent: StronglyConnectedComponent) {
    assert(stronglyConnectedComponent == schedulingHandler)
    //filters the list of staticallyListenedElements

    dynamicallyListenedElementsFromSameComponent = List.empty
  }
}

/**
  * This class is used in as a handle to register and unregister dynamically to variables
  * @author renaud.delandtsheer@cetic.be
  */
class KeyForDynamicDependencyRemoval(key1: DPFDLLStorageElement[_],
                                     key2: DPFDLLStorageElement[_]) {
  def performRemove(): Unit = {
    key1.delete()
    key2.delete()
  }
}
