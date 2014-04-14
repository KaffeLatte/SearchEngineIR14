/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 *   Additions: Hedvig Kjellström, 2012-14
 */

package ir;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {

	/** The index as a hashtable. */
	private HashMap<String, PostingsList> index = new HashMap<String, PostingsList>();
	private HashMap<Integer, Double> pageRanks;
	private PostingsList scoresToClear = new PostingsList();
	HashMap<Integer, PostingsEntry> scores = new HashMap<Integer, PostingsEntry>();

	BiWordIndex myBiWordIndex = new BiWordIndex();

	int oldDocID = -1;
	final int K = 10;

	// Strängen under måste sluta på '/'
	String directoryPath = "/tmp/eaalto/savedIndex/";

	/**
	 * Inserts this token in the index.
	 */

	@Override
	public void insert(String token, int docID, int offset) {
		//
		// YOUR CODE HERE
		//

		// if (oldDocID == -1) {
		myBiWordIndex.insert(token, docID, offset);
		// }

		// oldDocID = 0;

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

		// long startTime = System.nanoTime();
		//
		// System.out.println("------------------");
		// System.out.println("# of unique elements in index " + index.size());
		// System.out.println("------------------");

		if (scoresToClear != null) {
			for (int i = 0; i < scoresToClear.size(); i++) {
				scoresToClear.get(i).clearScore();
			}
			scoresToClear = new PostingsList();
		}

		return makeQuery(query, queryType, rankingType, structureType, false);
	}

	private PostingsList makeQuery(Query query, int queryType, int rankingType,
			int structureType, Boolean dummy) {

		PostingsList answer = new PostingsList();

		if (structureType == 1) {
			return myBiWordIndex.search(query, queryType, rankingType,
					structureType);
		}

		if (structureType == 2) {
			if (query.terms.size() < 2) {
				return makeQuery(query, 2, rankingType, 0, false);
			} else {
				answer = myBiWordIndex.search(query, queryType, rankingType,
						structureType);
				if (answer.size() > K) {
					answer.sort();
					return answer;
				} else {
					PostingsList secondAnswer = makeQuery(query, 2,
							rankingType, 0, true);

					for (int i = 0; i < answer.size(); i++) {
						PostingsEntry pE = answer.get(i);
						double localScore = pE.getScore();
						pE.setScore(1 + localScore);
					}
					answer = union(answer, secondAnswer);
					answer.sort();
					return answer;
				}
			}

		}

		// Gör om queries till en länkad lista med strängar
		LinkedList<String> queries = query.terms;
		// Skapa en ArrayList med alla träffar
		ArrayList<PostingsList> queryResponses = new ArrayList<PostingsList>();
		for (String s : queries) {
			PostingsList tmpList = getPostings(s);
			if (tmpList == null) {
				getPostingsListFromFile(s);
				tmpList = getPostings(s);
			}

			queryResponses.add(tmpList);
		}

		answer = queryResponses.get(0);
		queryResponses.remove(0);

		int weigthCounter = 0;
		if (queryType == 2) {
			scores = new HashMap<Integer, PostingsEntry>();
			fastCosineScores(answer, query.weights.get(weigthCounter));
			weigthCounter++;

		}

		Integer distance = 1;

		while (queryResponses.size() != 0) {
			PostingsList listToAddToAnswer = queryResponses.get(0);
			queryResponses.remove(0);

			if (queryType == 0) {
				answer = intersect(answer, listToAddToAnswer);
			}
			if (queryType == 1) {
				answer = positionalIntersect(answer, listToAddToAnswer,
						distance);
				distance++;
			}
			if (queryType == 2) {
				fastCosineScores(listToAddToAnswer, weigthCounter);
				weigthCounter++;
			}

		}

		if (queryType == 2) {
			Object[] keysInHashMap = scores.keySet().toArray();
			answer = new PostingsList();
			for (Object o : keysInHashMap) {
				PostingsEntry pE = scores.get(o);
				pE.setScore(pE.getScore()
						/ docLengths.get(Integer.toString(pE.docID)));
				answer.add(pE);
			}
			if (!dummy) {
				answer.sort();
			}

		}

		if (rankingType == 1) {
			if (pageRanks == null) {
				getPageRanksFromFile();
			}

			for (int i = 0; i < answer.size(); i++) {
				PostingsEntry pE = answer.get(i);
				pE.clearScore();
				pE.addValueToScore(pageRanks.get(pE.docID));
			}

			answer.sort();
		}

		if (rankingType == 2) {
			if (pageRanks == null) {
				getPageRanksFromFile();
			}

			for (int i = 0; i < answer.size(); i++) {
				double pageRankWeight = 0.75;
				double tfIdfWeight = 0.25;

				PostingsEntry pE = answer.get(i);
				pE.setScore(pE.getScore() * tfIdfWeight);
				pE.addValueToScore(pageRanks.get(pE.docID) * pageRankWeight);
			}

			answer.sort();
		}

		scoresToClear = answer;

		return answer;
	}

	private void fastCosineScores(PostingsList addToAnswer, double weight) {

		PostingsEntry pE;
		for (int i = 0; i < addToAnswer.size(); i++) {
			pE = addToAnswer.get(i);
			double score = weight
					* getTfIdf(pE.getTf(), addToAnswer.size(),
							docLengths.get(Integer.toString(pE.docID)));
			if (scores.containsKey(pE.docID)) {
				scores.get(pE.docID).addValueToScore(score);
			} else {
				pE.addValueToScore(score);
				scores.put(pE.docID, pE);
			}
		}
	}

	private PostingsList positionalIntersect(PostingsList p1, PostingsList p2,
			int distance) {
		PostingsList intersectedList = new PostingsList();

		int p1index = 0;
		int p2index = 0;

		PostingsEntry tmp1;
		PostingsEntry tmp2;

		while (p1index < p1.size() && p2index < p2.size()) {

			tmp1 = p1.get(p1index);
			tmp2 = p2.get(p2index);

			if (tmp1.docID == tmp2.docID) {
				LinkedList<Integer> p1Offsets = tmp1.getOffsets();
				LinkedList<Integer> p2Offsets = tmp2.getOffsets();

				outerloop: for (int i = 0; i < p1Offsets.size(); i++) {
					for (int j = 0; j < p2Offsets.size(); j++) {
						if (p2Offsets.get(j) - p1Offsets.get(i) == distance) {
							intersectedList.insert(tmp1);
							break outerloop;
						}
					}
				}
				p1index++;
				p2index++;

			} else {
				if (tmp1.docID < tmp2.docID) {
					p1index++;
				} else {
					p2index++;
				}
			}

		}
		return intersectedList;
	}

	private PostingsList intersect(PostingsList p1, PostingsList p2) {

		PostingsList intersectedList = new PostingsList();

		int p1index = 0;
		int p2index = 0;

		while (p1index < p1.size() && p2index < p2.size()) {
			PostingsEntry tmp1 = p1.get(p1index);
			PostingsEntry tmp2 = p2.get(p2index);

			if (tmp1.docID == tmp2.docID) {
				intersectedList.insert(tmp1);
				p1index++;
				p2index++;
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

	private PostingsList union(PostingsList p1, PostingsList p2) {

		Boolean isAdd = true;
		for (int i = 0; i < p1.size(); i++) {
			for (int j = 0; j < p2.size(); j++) {
				if (p1.get(i).docID == p2.get(j).docID) {
					p2.get(j).addValueToScore(p1.get(i).getScore());
					isAdd = false;
					break;
				}
			}
			if (isAdd == true) {
				p2.add(p1.get(i));
			}
			isAdd = true;
		}
		return p2;
	}

	private double getTfIdf(double tf, double df, double len) {
		double idf = 0;
		double tfIdf = 0;

		len = 1;
		idf = Math.log(docLengths.size() / df);
		tfIdf = tf * idf / len;

		return tfIdf;
	}

	private void getPageRanksFromFile() {
		File file = new File(
				"/afs/nada.kth.se/home/3/u1qvl923/ir/labb2/pageRanks/pageRankAllLinks");
		System.out.println("Före try!");
		try {
			FileInputStream f = new FileInputStream(file);
			ObjectInputStream s = new ObjectInputStream(f);
			pageRanks = (HashMap<Integer, Double>) s.readObject();
			s.close();
			System.out.println("End of try");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void getPostingsListFromFile(String token) {
		String firstLetterOfToken = String.valueOf(token.charAt(0));
		try {
			FileInputStream fis = new FileInputStream(directoryPath
					+ firstLetterOfToken + "/" + token);
			ObjectInputStream ois = new ObjectInputStream(fis);
			PostingsList injectIntoHashMap = (PostingsList) ois.readObject();
			index.put(token, injectIntoHashMap);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * No need for cleanup in a HashedIndex.
	 */

	@Override
	public void cleanup() {
		for (String key : index.keySet()) {
			try {
				if (key.length() < 1) {
					continue;
				}
				String newDirectoryName = String.valueOf(key.charAt(0));
				new File(directoryPath + newDirectoryName).mkdir();

				FileOutputStream fout = new FileOutputStream(directoryPath
						+ newDirectoryName + "/" + key);
				ObjectOutputStream oos = new ObjectOutputStream(fout);
				oos.writeObject(getPostings(key));
				fout.close();
				oos.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
