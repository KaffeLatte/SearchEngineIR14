/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */

package ir;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;

/**
 * A list of postings for a given word.
 */
public class PostingsList implements Serializable {

	/** The postings list as a linked list. */
	private LinkedList<PostingsEntry> list = new LinkedList<PostingsEntry>();
	double lowerThreshold = 10;
	boolean sorted = false;

	/** Number of postings in this list */
	public int size() {
		return list.size();
	}

	/** Returns the ith posting */
	public PostingsEntry get(int i) {
		return list.get(i);
	}

	//
	// YOUR CODE HERE
	//

	// constructor
	public PostingsList() {

	}

	// constructor
	public PostingsList(PostingsEntry myPostingsEntry) {
		list.add(myPostingsEntry);
		lowerThreshold = myPostingsEntry.score;
	}

	public void insert(PostingsEntry myPostingsEntry) {

		// if (list.size() < 50) {

		if (list.isEmpty()) {
			list.add(myPostingsEntry);
			return;
		}
		if (!(list.getLast().docID == myPostingsEntry.docID)) {
			list.add(myPostingsEntry);
		} else {
			list.getLast().addOffset(myPostingsEntry.offsetList.get(0));
		}
		// } else if (!sorted) {
		//
		// Comparator<PostingsEntry> docIDComparator = new
		// Comparator<PostingsEntry>() {
		// @Override
		// public int compare(PostingsEntry p1, PostingsEntry p2) {
		// return p1.docID - p2.docID;
		// }
		// };
		//
		// Collections.sort(list, docIDComparator);
		// sorted = true;
		// }
	}

	public void add(PostingsEntry myPostingsEntry) {
		list.add(myPostingsEntry);
	}

	public void clear() {
		list.clear();
	}

	public void sort() {
		Collections.sort(list);
	}

	public boolean contains(PostingsEntry pE) {
		if (list.contains(pE)) {
			return true;
		} else {
			return false;
		}
	}

}
