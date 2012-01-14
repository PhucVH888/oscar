/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *  
 * Contributors:
 *      www.n-side.com
 ******************************************************************************/
package scampi.cp.constraints;

import java.util.Arrays;
import java.util.Comparator;

import scampi.cp.core.CPOutcome;
import scampi.cp.core.CPPropagStrength;
import scampi.cp.core.Constraint;
import scampi.cp.core.CPVarBool;
import scampi.cp.core.CPVarInt;
import scampi.cp.scheduling.Activity;

public class UnaryResource extends Constraint {

	private int nbAct;
	private Activity [] activities;
	private ThetaTree thetaTree;
	private LambdaThetaTree lamdaThetaTree;

	private ActivityWrapper [] wrappers;
	private ActivityWrapper [] ect;
	private ActivityWrapper [] est;
	private ActivityWrapper [] lct;
	private ActivityWrapper [] lst;
	private int [] new_est;

	//mirror activities
	private ActivityWrapper [] mwrappers;
	private ActivityWrapper [] mect;
	private ActivityWrapper [] mest;
	private ActivityWrapper [] mlct;
	private ActivityWrapper [] mlst;
	private int [] new_lct;

	private ESTComparator estComp = new ESTComparator();
	private LSTComparator lstComp = new LSTComparator();
	private ECTComparator ectComp = new ECTComparator();
	private LCTComparator lctComp = new LCTComparator();

	private boolean failure;

	private CPVarInt [] positions; //position[i] = index of activity in positions i
	private CPVarInt [] ranks; //rank[i] position of activity i => positions[ranks[i]] == i
	

	public UnaryResource(Activity [] activities) {
		this(activities, "UnaryResource");
	}

	public UnaryResource(Activity [] activities, String name) {
		super(activities[0].getStart().getStore(),name);
		this.name = name;
		this.activities = activities;
		this.nbAct = activities.length;

		positions = CPVarInt.getArray(s, nbAct, 0, nbAct-1,"pos");
		ranks = CPVarInt.getArray(s, nbAct, 0, nbAct-1,"rank");

		this.thetaTree = new ThetaTree(activities.length);
		this.lamdaThetaTree = new LambdaThetaTree(activities.length);

		wrappers = new ActivityWrapper[nbAct];
		ect = new ActivityWrapper[nbAct];
		est = new ActivityWrapper[nbAct];
		lct = new ActivityWrapper[nbAct];
		lst = new ActivityWrapper[nbAct];
		new_est = new int [nbAct];

		//mirror activities
		mwrappers = new ActivityWrapper[nbAct];
		mect  = new ActivityWrapper[nbAct];
		mest = new ActivityWrapper[nbAct]; 
		mlct = new ActivityWrapper[nbAct];
		mlst = new ActivityWrapper[nbAct];
		new_lct = new int [nbAct];

		for (int i = 0; i < activities.length; i++) {
			ActivityWrapper w = new ActivityWrapper(i,activities[i]);
			wrappers[i] = w;
			ect[i] = w;
			est[i] = w;
			lct[i] = w;
			lst[i] = w;
			new_est[i] = Integer.MIN_VALUE;

			w = new ActivityWrapper(i,new MirrorActivity(activities[i]));
			mwrappers[i] = w;
			mect[i] = w;
			mest[i] = w;
			mlct[i] = w;
			mlst[i] = w;
			new_lct[i] = Integer.MAX_VALUE;
		}		

		failure = false;
	}

	public int getMinTotDur() {
		int d = 0;
		for (int i = 0; i < activities.length; i++) {
			d += activities[i].getMinDuration();
		}
		return d;
	}

	public Activity [] getActivities() {
		return activities;
	}



	@Override
	protected CPOutcome setup(CPPropagStrength l) {

		for (int i = 0; i < nbAct; i++) {
			activities[i].getStart().callPropagateWhenBoundsChange(this);
			activities[i].getEnd().callPropagateWhenBoundsChange(this);
		}

		if (s.post(new AllDifferent(positions),CPPropagStrength.Strong) == CPOutcome.Failure) { //should not fail
			return CPOutcome.Failure;
		}

		
		if (s.post(new AllDifferent(ranks),CPPropagStrength.Strong) == CPOutcome.Failure) { //should not fail
			return CPOutcome.Failure;
		}
		
		CPVarInt [] starts = new CPVarInt[nbAct];
		CPVarInt [] ends = new CPVarInt[nbAct];
		for (int i = 0; i < nbAct; i++) {
			starts[i] = activities[i].getStart();
			ends[i] = activities[i].getEnd();
		}
		
		
		for (int i = 0; i < nbAct-1; i++) {
			if (s.post(new ElementVar(positions, ranks[i], i)) == CPOutcome.Failure) {
				return CPOutcome.Failure;
			}
			CPVarInt endi = Element.get(ends, positions[i]);
			CPVarInt starti1 = Element.get(starts, positions[i+1]);
			if (s.post(new LeEq(endi, starti1)) == CPOutcome.Failure) {
				return CPOutcome.Failure;
			}
		}

		for (int i = 0; i < nbAct; i++) {
			for (int j = i+1; j < nbAct; j++) {
				if (notOverlap(activities[i], activities[j]) == CPOutcome.Failure) {
					return CPOutcome.Failure;
				}
			}
		}

		if (propagate() == CPOutcome.Failure) {
			return CPOutcome.Failure;
		}

		return CPOutcome.Suspend;
	}


	/**
	 * 
	 * @return rank[i] is the position of the activity y in the sequence
	 */
	public CPVarInt[] getRanks() {
		return ranks;
	}


	private CPOutcome notOverlap(Activity act1, Activity act2) {
		CPVarBool b1 = act2.getStart().isGrEq(act1.getEnd());
		CPVarBool b2 = act1.getStart().isGrEq(act2.getEnd());
		if (s.post(new Sum(new CPVarBool [] {b1,b2}, 1)) == CPOutcome.Failure) {
			return CPOutcome.Failure;
		}
		return CPOutcome.Suspend;
	}


	@Override
	protected CPOutcome propagate() {
		//System.out.println("propagate "+getName());	
		failure = false;
		do {
			do {

				do {
					if(!overloadChecking()) {
						return CPOutcome.Failure;
					}
				} while (!failure && detectablePrecedences());
			} while (!failure && notFirstNotLast() && !failure);
		} while (!failure && edgeFinder());
		if (failure) {
			return CPOutcome.Failure;
		} else {
			return CPOutcome.Suspend;
		}

	}



	private void updateEst() {
		Arrays.sort(est, estComp);
		for (int i = 0; i < est.length; i++) {
			est[i].setESTPos(i);
		}
		Arrays.sort(mest, estComp);
		for (int i = 0; i < mest.length; i++) {
			mest[i].setESTPos(i);
		}		
	}


	private boolean overloadChecking() {
		// Init
		updateEst();

		// One direction
		Arrays.sort(lct, lctComp);
		thetaTree.reset();
		for (int i = 0; i < nbAct; i++) {
			ActivityWrapper aw = lct[i];
			thetaTree.insert(aw.getActivity(), aw.getESTPos());
			if (thetaTree.getECT() > aw.getActivity().getLCT()) {
				return false;
			}
		}

		// Other direction
		Arrays.sort(mlct, lctComp);
		thetaTree.reset();
		for (int i = 0; i < nbAct; ++i) {
			ActivityWrapper aw = mlct[i];
			thetaTree.insert(aw.getActivity(), aw.getESTPos());
			if (thetaTree.getECT() > aw.getActivity().getLCT()) {
				return false;
			}
		}
		return true;
	}


	private boolean detectablePrecedences() {
		// Init
		updateEst();
		// Propagate in one direction
		Arrays.sort(ect, ectComp);
		Arrays.sort(lst, lstComp);
		thetaTree.reset();
		int j = 0;
		for (int i = 0; i < nbAct; i++) {
			ActivityWrapper awi = ect[i];
			if (j < nbAct) {
				ActivityWrapper awj = lst[j];
				while (awi.getActivity().getECT() > awj.getActivity().getLST()) {
					thetaTree.insert(awj.getActivity(), awj.getESTPos());
					j++;
					if (j == nbAct)
						break;
					awj = lst[j];
				}
			}
			int esti = awi.getActivity().getEST();
			boolean inserted = thetaTree.isInserted(awi.getESTPos());
			if (inserted) {
				thetaTree.remove(awi.getESTPos());
			}
			int oesti = thetaTree.getECT();
			if (inserted) {
				thetaTree.insert(awi.getActivity(), awi.getESTPos());
			}
			if (oesti > esti) {
				new_est[awi.getIndex()] = oesti;
			} else {
				new_est[awi.getIndex()] = Integer.MIN_VALUE;
			}
		}

		// Propagate in other direction
		Arrays.sort(mect, ectComp);
		thetaTree.reset();
		j = 0;
		for (int i = 0; i < nbAct; ++i) {
			ActivityWrapper awi = mect[i];
			if (j < nbAct) {
				ActivityWrapper awj = mlst[j];
				while (awi.getActivity().getECT() > awj.getActivity().getLST()) {
					thetaTree.insert(awj.getActivity(), awj.getESTPos());
					j++;
					if (j == nbAct)
						break;
					awj = mlst[j];
				}
			}
			int lcti = awi.getActivity().getEST();
			boolean inserted = thetaTree.isInserted(awi.getESTPos());
			if (inserted) {
				thetaTree.remove(awi.getESTPos());
			}
			int olcti = thetaTree.getECT();
			if (inserted) {
				thetaTree.insert(awi.getActivity(), awi.getESTPos());
			}
			if (olcti > lcti) {
				new_lct[awi.getIndex()] = -olcti;
			} else {
				new_lct[awi.getIndex()] = Integer.MAX_VALUE;
			}
		}

		// Apply modifications
		boolean modified = false;
		for (int i = 0; i < nbAct; i++) {
			if (new_est[i] != Integer.MIN_VALUE) {
				modified = true;
				if (activities[i].getStart().updateMin(new_est[i]) == CPOutcome.Failure) {
					failure = true;
				}
			}
			if (new_lct[i] != Integer.MAX_VALUE) {
				modified = true;
				if (activities[i].getEnd().updateMax(new_lct[i]) == CPOutcome.Failure) {
					failure = true;
				}
			}
		}
		return modified;
	}

	private boolean notFirstNotLast() {

		// Init
		updateEst();
		for (int i = 0; i < nbAct; ++i) {
			new_lct[i] = activities[i].getLCT();
			new_est[i] = activities[i].getEST();
		}

		// Push in one direction.
		Arrays.sort(lst, lstComp);
		Arrays.sort(lct, lctComp);

		thetaTree.reset();
		int j = 0;

		for (int i = 0; i < nbAct; ++i) {
			ActivityWrapper awi = lct[i];
			while (j < nbAct && awi.getActivity().getLCT() > lst[j].getActivity().getLST()) {
				if (j > 0 && thetaTree.getECT() > lst[j].getActivity().getLST()) {
					new_lct[lst[j].getIndex()] = lst[j - 1].getActivity().getLST();
				}
				thetaTree.insert(lst[j].getActivity(), lst[j].getESTPos());
				j++;
			}
			boolean inserted = thetaTree.isInserted(awi.getESTPos());
			if (inserted) {
				thetaTree.remove(awi.getESTPos());
			}
			int ect_theta_less_i = thetaTree.getECT();
			if (inserted) {
				thetaTree.insert(awi.getActivity(), awi.getESTPos());
			}
			if (ect_theta_less_i > awi.getActivity().getLCT() && j > 0) {
				new_lct[awi.getIndex()] = Math.min(new_lct[awi.getIndex()], lst[j - 1].getActivity().getLCT());
			}
		}

		// Push in other direction.
		Arrays.sort(mlst,lstComp);
		Arrays.sort(mlct,lctComp);
		thetaTree.reset();
		j = 0;
		for (int i = 0; i < nbAct; i++) {
			ActivityWrapper awi = mlct[i];
			while (j < nbAct && awi.getActivity().getLCT() > mlst[j].getActivity().getLST()) {
				if (j > 0 && thetaTree.getECT() > mlst[j].getActivity().getLST()) {
					new_est[mlst[j].getIndex()] = -mlst[j - 1].getActivity().getLST();
				}
				thetaTree.insert(mlst[j].getActivity(), mlst[j].getESTPos());
				j++;
			}
			boolean inserted = thetaTree.isInserted(awi.getESTPos());
			if (inserted) {
				thetaTree.remove(awi.getESTPos());
			}
			int mect_theta_less_i = thetaTree.getECT();
			if (inserted) {
				thetaTree.insert(awi.getActivity(), awi.getESTPos());
			}
			if (mect_theta_less_i > awi.getActivity().getLCT() && j > 0) {
				new_est[awi.getIndex()] = Math.max(new_est[awi.getIndex()], -mlst[j - 1].getActivity().getLCT());
			}
		}

		// Apply modifications
		boolean modified = false;
		for (int i = 0; i < nbAct; ++i) {
			if (activities[i].getLCT() > new_lct[i] || activities[i].getEST() < new_est[i]) {
				modified = true;

				if (activities[i].getStart().updateMin(new_est[i]) == CPOutcome.Failure) {
					failure = true;
				}

				if (activities[i].getEnd().updateMax(new_lct[i]) == CPOutcome.Failure) {
					failure = true;
				}
			}
		}
		return modified;

	} //end of notFirstNotLast



	private boolean edgeFinder() {
		// Init
		updateEst();
		for (int i = 0; i < nbAct; i++) {
			new_est[i] = activities[i].getEST();
			new_lct[i] = activities[i].getLCT();
		}

		// Push in one direction.
		Arrays.sort(lct,lctComp);
		lamdaThetaTree.reset();
		for (int i = 0; i < nbAct; i++) {

			lamdaThetaTree.insert(est[i].getActivity(), i);
		}
		int j = nbAct - 1;
		ActivityWrapper awj = lct[j];
		do {
			lamdaThetaTree.grey(awj.getESTPos());
			if (--j < 0) {
				break;
			}
			awj = lct[j];
			if (lamdaThetaTree.getECT() > awj.getActivity().getLCT()) {
				failure = true;  // Resource is overloaded
				return false;
			}
			while (lamdaThetaTree.getECT_OPT() > awj.getActivity().getLCT()) {
				int i = lamdaThetaTree.getResponsible_ECT();
				assert(i >= 0);
				int act_i = est[i].getIndex();
				if (lamdaThetaTree.getECT() > new_est[act_i]) {
					new_est[act_i] = lamdaThetaTree.getECT();
				}
				lamdaThetaTree.remove(i);
			}
		} while (j >= 0);

		// Push in other direction.
		Arrays.sort(mlct,lctComp);
		lamdaThetaTree.reset();
		for (int i = 0; i < nbAct; ++i) {
			lamdaThetaTree.insert(mest[i].getActivity(), i);
		}
		j = nbAct - 1;
		awj = mlct[j];
		do {
			lamdaThetaTree.grey(awj.getESTPos());
			if (--j < 0) {
				break;
			}
			awj = mlct[j];
			if (lamdaThetaTree.getECT() > awj.getActivity().getLCT()) {
				failure = true;  // Resource is overloaded
				return false;
			}
			while (lamdaThetaTree.getECT_OPT() > awj.getActivity().getLCT()) {
				int i = lamdaThetaTree.getResponsible_ECT();
				assert(i >= 0);
				int act_i = mest[i].getIndex();
				if (-lamdaThetaTree.getECT() < new_lct[act_i]) {
					new_lct[act_i] = -lamdaThetaTree.getECT();
				}
				lamdaThetaTree.remove(i);
			}
		} while (j >= 0);

		// Apply modifications.
		boolean modified = false;
		for (int i = 0; i < nbAct; i++) {
			if (activities[i].getEST() < new_est[i]) {
				modified = true;
				if (activities[i].getStart().updateMin(new_est[i]) == CPOutcome.Failure) {
					failure = true;
					return false;
				}
			}

			if (activities[i].getLCT() > new_lct[i] ) {
				modified = true;
				if (activities[i].getEnd().updateMax(new_lct[i]) == CPOutcome.Failure) {
					failure = true;
					return false;
				}
			}


		}
		return modified;
	}



}


class MirrorActivity extends Activity {

	private Activity act;

	public MirrorActivity(Activity act) {
		this.act = act;
	}

	/**
	 * earliest starting time
	 */
	public int getEST() {
		return - act.getLCT();
	}

	/**
	 * latest starting time
	 */
	public int getLST() {
		return - act.getECT();
	}

	/**
	 * earliest completion time assuming the smallest duration
	 */
	public int getECT() {
		return - act.getLST();
	}

	/**
	 * latest completion time assuming the smallest duration
	 */
	public int getLCT() {
		return - act.getEST();
	}

	/**
	 * current minimal duration of this activity
	 */
	public int getMinDuration() { 
		return act.getMinDuration();
	}


	/**
	 * current maximal duration of this activity
	 */
	public int getMaxDuration() { 
		return act.getMaxDuration(); 
	}
}


class ActivityWrapper {

	Activity act;
	int index;
	int est_pos;

	protected ActivityWrapper(int index, Activity act) {
		this.act = act;
		this.index = index;
		this.est_pos = -1;
	}

	Activity getActivity() {
		return act;
	}

	int getIndex() {
		//System.out.println("->"+index);
		return index;
	}

	int getESTPos() {
		return est_pos;
	}

	void setESTPos(int pos) {
		est_pos = pos;
	}	
}


class ESTComparator implements Comparator<ActivityWrapper> {
	public int compare(ActivityWrapper act0, ActivityWrapper act1) {
		return act0.getActivity().getEST()-act1.getActivity().getEST();
	}
}

class LCTComparator implements Comparator<ActivityWrapper> {
	public int compare(ActivityWrapper act0, ActivityWrapper act1) {
		return act0.getActivity().getLCT()-act1.getActivity().getLCT();

	}
}

class LSTComparator implements Comparator<ActivityWrapper> {
	public int compare(ActivityWrapper act0, ActivityWrapper act1) {
		return act0.getActivity().getLCT()-act1.getActivity().getLCT();
	}
}

class ECTComparator implements Comparator<ActivityWrapper> {
	public int compare(ActivityWrapper act0, ActivityWrapper act1) {
		return act0.getActivity().getECT()-act1.getActivity().getECT();
	}
}
