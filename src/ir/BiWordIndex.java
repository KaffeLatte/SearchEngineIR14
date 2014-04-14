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
public class BiWordIndex implements Index {

	/** The index as a hashtable. */
	private HashMap<String, PostingsList> index = new HashMap<String, PostingsList>();
	HashMap<Integer, PostingsEntry> scores = new HashMap<Integer, PostingsEntry>();

	String lastWord;
	int lastDoc = -1;

	/**
	 * Inserts this token in the index.
	 */
	@Override
	public void insert(String token, int docID, int offset) {
		//
		// YOUR CODE HERE
		//
		if (lastWord == null) {
			lastWord = token;
			lastDoc = docID;
		} else {
			if (lastDoc == docID) {
				String biGram = lastWord + " " + token;
				lastWord = token;

				PostingsEntry myPostingsEntry = new PostingsEntry(docID, offset);
				PostingsList myPostingsList;

				if (index.containsKey(biGram)) {
					myPostingsList = index.get(biGram);
					myPostingsList.insert(myPostingsEntry);
				} else {
					myPostingsList = new PostingsList(myPostingsEntry);
					index.put(biGram, myPostingsList);
				}
			} else {
				lastWord = null;
			}

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

		if (query.terms.size() < 2) {
			return null;
		}

		LinkedList<String> queryResponses = makeBiGramQueries(query);
		ArrayList<PostingsList> queryHits = getQueryHits(queryResponses);

		scores = new HashMap<Integer, PostingsEntry>();
		while (queryHits.size() != 0) {
			PostingsList pList = queryHits.get(0);
			if (pList != null) {
				fastCosineScores(pList);
			}
			queryHits.remove(0);
		}

		PostingsList answer = getAnswerFromHashMap();

		if (structureType == 1) {
			answer.sort();
		}

		return answer;
	}

	private PostingsList getAnswerFromHashMap() {
		Object[] keysInHashMap = scores.keySet().toArray();
		PostingsList answer = new PostingsList();

		for (Object o : keysInHashMap) {
			PostingsEntry pE = scores.get(o);
			pE.setScore(pE.getScore()
					/ (docLengths.get(Integer.toString(pE.docID)) - 1));
			answer.add(pE);
		}

		return answer;
	}

	private ArrayList<PostingsList> getQueryHits(
			LinkedList<String> queryResponses) {

		ArrayList<PostingsList> queryHits = new ArrayList<PostingsList>();

		for (String s : queryResponses) {
			PostingsList tmpList = getPostings(s);
			queryHits.add(tmpList);
		}

		return queryHits;
	}

	private LinkedList<String> makeBiGramQueries(Query query) {
		LinkedList<String> originalQueries = query.terms;
		LinkedList<String> newQueries = new LinkedList<String>();

		String lastWord = originalQueries.get(0);
		for (int i = 1; i < originalQueries.size(); i++) {
			String temp = originalQueries.get(i);
			newQueries.add(lastWord + " " + temp);
			lastWord = temp;
		}

		return newQueries;
	}

	private void fastCosineScores(PostingsList addToAnswer) {

		PostingsEntry pE;
		for (int i = 0; i < addToAnswer.size(); i++) {
			pE = addToAnswer.get(i);
			double score = getTfIdf(pE.getTf(), addToAnswer.size());
			if (scores.containsKey(pE.docID)) {
				scores.get(pE.docID).addValueToScore(score);
			} else {
				pE.addValueToScore(score);
				scores.put(pE.docID, pE);
			}
		}
	}

	private double getTfIdf(double tf, double df) {
		double idf = 0;
		double tfIdf = 0;

		idf = Math.log(docLengths.size() / df);
		tfIdf = tf * idf;

		return tfIdf;
	}

	/**
	 * No need for cleanup in a HashedIndex.
	 */
	@Override
	public void cleanup() {
	}
}