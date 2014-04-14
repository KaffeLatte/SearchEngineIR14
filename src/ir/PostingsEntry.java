/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */

package ir;

import java.io.Serializable;
import java.util.LinkedList;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {

	public int docID;
	public double score;
	public LinkedList<Integer> offsetList;

	/**
	 * PostingsEntries are compared by their score (only relevant in ranked
	 * retrieval).
	 * 
	 * The comparison is defined so that entries will be put in descending
	 * order.
	 */
	@Override
	public int compareTo(PostingsEntry other) {
		return Double.compare(other.score, score);
	}

	//
	// YOUR CODE HERE
	//

	public PostingsEntry(int pDocID, int offset) {
		offsetList = new LinkedList<Integer>();
		offsetList.add(offset);
		this.docID = pDocID;
		this.score = 0;
	}

	public PostingsEntry(int docID, int offset, double score) {
		offsetList = new LinkedList<Integer>();
		offsetList.add(offset);
		this.docID = docID;
		this.score = score;
	}

	public void addOffset(int offset) {
		offsetList.add(offset);
	}

	public LinkedList<Integer> getOffsets() {
		return offsetList;
	}

	public void addValueToScore(double score) {
		this.score += score;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public void clearScore() {
		score = 0;
	}

	public Integer getTf() {
		return offsetList.size();
	}

	public Boolean equals(PostingsEntry pE) {
		if (this.docID == pE.docID) {
			return true;
		}
		return false;
	}

}
