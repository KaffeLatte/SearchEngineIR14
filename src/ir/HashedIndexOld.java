/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 *   Additions: Hedvig Kjellström, 2012-14
 */

package ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndexOld implements Index {

	/** The index as a hashtable. */
	private HashMap<String, PostingsList> index = new HashMap<String, PostingsList>();

	/**
	 * Inserts this token in the index.
	 */
	@Override
	public void insert(String token, int docID, int offset) {
		//
		// YOUR CODE HERE
		//
		PostingsEntry myPostingsEntry = new PostingsEntry(docID, offset);
		PostingsList myPostingsList;

		if (index.containsKey(token)) {
			myPostingsList = index.get(token);
			myPostingsList.insert(myPostingsEntry);
		} else {
			myPostingsList = new PostingsList(myPostingsEntry);
			index.put(token, myPostingsList);
		}
	}

	/**
	 * Returns all the words in the index.
	 */
	@Override
	public Iterator<String> getDictionary() {
		//
		// REPLACE THE STATEMENT BELOW WITH YOUR CODE
		//
		return index.keySet().iterator();
	}

	/**
	 * Returns the postings for a specific term, or null if the term is not in
	 * the index.
	 */
	@Override
	public PostingsList getPostings(String token) {
		//
		// REPLACE THE STATEMENT BELOW WITH YOUR CODE
		//

		return index.get(token);
	}

	/**
	 * Searches the index for postings matching the query.
	 */
	@Override
	public PostingsList search(Query query, int queryType, int rankingType,
			int structureType) {
		//
		// REPLACE THE STATEMENT BELOW WITH YOUR CODE
		//
		if (queryType == 0) {
			return intersectionQuery(query, false);
		}
		if (queryType == 1) {
			return intersectionQuery(query, true);
		}

		return null;

	}

	private PostingsList intersectionQuery(Query query, boolean phraseQuery) {

		// Gör om queries till en länkad lista med strängar
		LinkedList<String> queries = query.terms;

		// Skapa en ArrayList med alla träffar
		ArrayList<PostingsList> queryResponses = new ArrayList<PostingsList>();
		for (String s : queries) {
			queryResponses.add(getPostings(s));
		}

		// Sortera efter antal element
		// Collections.sort(queryResponses, new Comparator<PostingsList>() {
		// @Override
		// public int compare(PostingsList p1, PostingsList p2) {
		// return p2.size() - p1.size();
		// }
		// });
		// Collections.reverse(queryResponses);

		PostingsList answer = queryResponses.get(0);
		queryResponses.remove(0);

		while (queryResponses.size() != 0) {
			answer = intersect(answer, queryResponses.get(0), phraseQuery);
			queryResponses.remove(0);
		}

		return answer;
	}

	private PostingsList intersect(PostingsList p1, PostingsList p2,
			boolean phraseQuery) {

		PostingsList intersectedList = new PostingsList();

		int p1index = 0;
		int p2index = 0;

		while (p1index < p1.size() && p2index < p2.size()) {
			PostingsEntry tmp1 = p1.get(p1index);
			PostingsEntry tmp2 = p2.get(p2index);

			if (tmp1.docID == tmp2.docID) {
				if (phraseQuery) {
					LinkedList<Integer> p1Offsets = tmp1.getOffsets();
					LinkedList<Integer> p2Offsets = tmp2.getOffsets();
					for (Integer tmpInt : p1Offsets) {
						if (p2Offsets.contains(tmpInt.intValue() + 1)) {
							intersectedList.insert(tmp2);
							// p1index++;
							p2index++;
							break;
						} else {
							p1index++;
							p2index++;

						}
					}

				} else {
					intersectedList.insert(p1.get(p1index));
					p1index++;
					p2index++;
				}
			} else {
				if (p1.get(p1index).docID < p2.get(p2index).docID) {
					p1index++;
				} else {
					p2index++;
				}

			}
		}

		return intersectedList;
	}

	/**
	 * No need for cleanup in a HashedIndex.
	 */
	@Override
	public void cleanup() {
	}
}