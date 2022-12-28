package main.csp;

import java.time.LocalDate;
import java.util.*;

/**
 * CSP: Calendar Satisfaction Problem Solver
 * Provides a solution for scheduling some n meetings in a given
 * period of time and according to some unary and binary constraints
 * on the dates of each meeting.
 */
public class CSPSolver {

    // Backtracking CSP Solver
    // --------------------------------------------------------------------------------------------------------------
    
    /**
     * Public interface for the CSP solver in which the number of meetings,
     * range of allowable dates for each meeting, and constraints on meeting
     * times are specified.
     * @param nMeetings The number of meetings that must be scheduled, indexed from 0 to n-1
     * @param rangeStart The start date (inclusive) of the domains of each of the n meeting-variables
     * @param rangeEnd The end date (inclusive) of the domains of each of the n meeting-variables
     * @param constraints Date constraints on the meeting times (unary and binary for this assignment)
     * @return A list of dates that satisfies each of the constraints for each of the n meetings,
     *         indexed by the variable they satisfy, or null if no solution exists.
     */
    public static List<LocalDate> solve (int nMeetings, LocalDate rangeStart, LocalDate rangeEnd, Set<DateConstraint> constraints) {
        // [!] TODO!
        //CSP is a 3tuple CSP <X,D,C> x set of variables, domains Di , Constraints C
        //goal state is everything is assigned and assignment satisfies all constraints
        //depth first search performed on recursion tree generated, subtrees are pruned as 
        // soon as partial assignment violates any constraint
        List<MeetingDomain> varDomain = new ArrayList<MeetingDomain>();
        for (int i=0; i<nMeetings; i++) {
            varDomain.add(new MeetingDomain(rangeStart,rangeEnd));
        }
        //creates a set of var domains, Di for each index, so that we can shrink these

        nodeConsistency(varDomain, constraints);
        // our two methods for shrinking the Di's unary then binary
        arcConsistency(varDomain, constraints);
        //binary with switches
        return recursiveSolve(0,new ArrayList<LocalDate>(), nMeetings, varDomain, constraints);
    }

    public static List<LocalDate> recursiveSolve (int index, List<LocalDate> assignment, int nMeetings, List<MeetingDomain> domains, Set<DateConstraint> constraints){
        //check if assignment is complete, aka everything is assigned and assignment satisfies all constraints
        if(assignment.size() == nMeetings){
            return assignment;
        }
        MeetingDomain days = domains.get(index);
        //for each date in domain
        for (LocalDate date : days.domainValues){
            //have to add first, a little different than pseudo code but I don't see how you would do it else
            // otherwise
            assignment.add(date);
            if(consistent(assignment, constraints)){
                //helper funciton, then does recursive call, if result actually has stuff then return result
                List<LocalDate> result = recursiveSolve(index+1,assignment, nMeetings, domains, constraints);
                if(result!=null){
                    return result;
                    
                }
            }
            // remove the last Localdate essentially the index that we are currently working on
            assignment.remove(assignment.size() - 1);
        }
        //return null if there is no possible setup with the given constraints
        //should not get here for the given tests
        return null;
    }

    public static boolean consistent(List<LocalDate> assignment, Set<DateConstraint> constraints){
        //checks if assignment is consistent with constraints
        for (DateConstraint constraint : constraints) {
            if(constraint.arity() == 1 && assignment.size()-1 >= constraint.L_VAL && !(constraint.isSatisfiedBy(assignment.get(constraint.L_VAL), ((UnaryDateConstraint) constraint).R_VAL))) {
					return false;
            }
            //only checks up to current index, has checks
            // so that it doesnt check the constraints indices that dont exist yet in assignment are not checked, or else it would 
            // be null pointer exc
            else if(constraint.arity()==2 && assignment.size()-1 >= ((BinaryDateConstraint) constraint).R_VAL && assignment.size()-1 >= constraint.L_VAL && !(constraint.isSatisfiedBy(assignment.get(constraint.L_VAL), assignment.get(((BinaryDateConstraint) constraint).R_VAL)))) {
					return false;
			}
            //return true if passes all constraints currently
        }return true;
    }
    
    // Filtering Operations
    // --------------------------------------------------------------------------------------------------------------
    
    /**
     * Enforces node consistency for all variables' domains given in varDomains based on
     * the given constraints. Meetings' domains correspond to their index in the varDomains List.
     * @param varDomains List of MeetingDomains in which index i corresponds to D_i
     * @param constraints Set of DateConstraints specifying how the domains should be constrained.
     * [!] Note, these may be either unary or binary constraints, but this method should only process
     *     the *unary* constraints! 
     */
    public static void nodeConsistency (List<MeetingDomain> varDomains, Set<DateConstraint> constraints) {
        for (DateConstraint constraint : constraints) {
            if( constraint.arity() != 1) {
                //only want to check unary constraints
                continue;
                // then check next constraint
            }
            MeetingDomain dom = varDomains.get(constraint.L_VAL); 
            //constraint LVAL is index, so get domain associated with index
            for (LocalDate day: new HashSet<>(dom.domainValues)){ //fpr each in domain
                if (!(constraint.isSatisfiedBy( day, ((UnaryDateConstraint) constraint).R_VAL))) { //filtering domains
    				dom.domainValues.remove(day);
    			}
            }
            varDomains.set(constraint.L_VAL, dom); //adds meeting domain corresponding to Di constraints
        }
    }
    
    /**
     * Enforces arc consistency for all variables' domains given in varDomains based on
     * the given constraints. Meetings' domains correspond to their index in the varDomains List.
     * @param varDomains List of MeetingDomains in which index i corresponds to D_i
     * @param constraints Set of DateConstraints specifying how the domains should be constrained.
     * [!] Note, these may be either unary or binary constraints, but this method should only process
     *     the *binary* constraints using the AC-3 algorithm! 
     */
    public static void arcConsistency (List<MeetingDomain> varDomains, Set<DateConstraint> constraints) {
        // [!] TODO!
        
        Set<Arc> arcQueue = new HashSet<Arc>();
        //following the pseudo code, pseudo code is very vague 

        for (DateConstraint constraint : constraints) { 
            //for each constraint add arc + reverse , only checking for binary
            if (constraint.arity() != 2) {
                //binary
                continue;
            }
            arcQueue.add( new Arc(constraint.L_VAL, ((BinaryDateConstraint) constraint).R_VAL, constraint));
            //add arc
            Arc reverse = new Arc(((BinaryDateConstraint) constraint).R_VAL, constraint.L_VAL, ((BinaryDateConstraint) constraint).getReverse());
            // reverse here meaning > = < 
            arcQueue.add(reverse);
        }

        Set<Arc> copyArcQ = new HashSet<Arc>(arcQueue); //simply a copy
        Set<Arc> nArc = new HashSet<Arc>(); //empty

        while((arcQueue.isEmpty()) == false){
            for (Arc ark1  : arcQueue) {
        		if (removeInconsistentVals(ark1, varDomains)) {
                    //if it has removed vals then :
        			for (Arc ark2  : copyArcQ) {
        				if (ark2.HEAD == ark1.TAIL) {
        					
        					nArc.add(ark2);
        				}
        			}
        		}
        	}
    		arcQueue = new HashSet<Arc>(nArc);
            //set og arc to new arc
    		nArc.clear();
            //clear newset of arcs
        }
    }

    public static boolean removeInconsistentVals (Arc remArc, List<MeetingDomain> varDomain) {
        MeetingDomain domain = varDomain.get(remArc.TAIL); //meeting index of tail
        boolean result = false;


        Set<LocalDate> tailVals = new HashSet<>(domain.domainValues);
        Set<LocalDate> headVals = new HashSet<>(varDomain.get(remArc.HEAD).domainValues);
        //sets for tail and head
        boolean temp = false;
    	for (LocalDate tDate: tailVals) {
            //for each tail
    		boolean containsValue = false;
    		for(LocalDate hDate: headVals) {
                //for each head
    			if (remArc.CONSTRAINT.isSatisfiedBy(tDate, hDate)) {
                    //if tail day and head day isConsistent
                     containsValue = true; 
                }
    		}
    		if(containsValue == false) {  //if false remove, 
    			domain.domainValues.remove(tDate);
    			result = true;
    		}
    	}
        //arc
    	varDomain.set(remArc.TAIL, domain);

    	return result;
    }

    
    /**
     * Private helper class organizing Arcs as defined by the AC-3 algorithm, useful for implementing the
     * arcConsistency method.
     * [!] You may modify this class however you'd like, its basis is just a suggestion that will indeed work.
     */

    private static class Arc {
        
        public final DateConstraint CONSTRAINT;
        public final int TAIL, HEAD;
        
        /**
         * Constructs a new Arc (tail -> head) where head and tail are the meeting indexes
         * corresponding with Meeting variables and their associated domains.
         * @param tail Meeting index of the tail
         * @param head Meeting index of the head
         * @param c Constraint represented by this Arc.
         * [!] WARNING: A DateConstraint's isSatisfiedBy method is parameterized as:
         * isSatisfiedBy (LocalDate leftDate, LocalDate rightDate), meaning L_VAL for the first
         * parameter and R_VAL for the second. Be careful with this when creating Arcs that reverse
         * direction. You may find the BinaryDateConstraint's getReverse method useful here.
         */
        public Arc (int tail, int head, DateConstraint c) {
            this.TAIL = tail;
            this.HEAD = head;
            this.CONSTRAINT = c;
        }
        
        @Override
        public boolean equals (Object other) {
            if (this == other) { return true; }
            if (this.getClass() != other.getClass()) { return false; }
            Arc otherArc = (Arc) other;
            return this.TAIL == otherArc.TAIL && this.HEAD == otherArc.HEAD && this.CONSTRAINT.equals(otherArc.CONSTRAINT);
        }
        
        @Override
        public int hashCode () {
            return Objects.hash(this.TAIL, this.HEAD, this.CONSTRAINT);
        }
        
        @Override
        public String toString () {
            return "(" + this.TAIL + " -> " + this.HEAD + ")";
        }
        
    }
    
}
