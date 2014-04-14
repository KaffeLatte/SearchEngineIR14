/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Hedvig Kjellström, 2012
 */

package ir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.StringTokenizer;

public class Query {

	public LinkedList<String> terms = new LinkedList<String>();
	public LinkedList<Double> weights = new LinkedList<Double>();

	/**
	 * Creates a new empty Query
	 */
	public Query() {
	}

	/**
	 * Creates a new Query from a string of words
	 */
	public Query(String queryString) {
		StringTokenizer tok = new StringTokenizer(queryString);
		while (tok.hasMoreTokens()) {
			terms.add(tok.nextToken());
			weights.add(new Double(1));
		}
	}

	/**
	 * Returns the number of terms
	 */
	public int size() {
		return terms.size();
	}

	/**
	 * Returns a shallow copy of the Query
	 */
	public Query copy() {
		Query queryCopy = new Query();
		queryCopy.terms = (LinkedList<String>) terms.clone();
		queryCopy.weights = (LinkedList<Double>) weights.clone();
		return queryCopy;
	}

	/**
	 * Expands the Query using Relevance Feedback
	 */
	public void relevanceFeedback(PostingsList results,
			boolean[] docIsRelevant, Indexer indexer) {
		// results contain the ranked list from the current search
		// docIsRelevant contains the users feedback on which of the 10 first
		// hits are relevant

		//
		// YOUR CODE HERE
		//
		double alpha = 1;
		double beta = 0.75;

		HashMap<String, Double> optimalQueryVector = new HashMap<String, Double>();
		HashMap<String, Integer> documentVector;
		HashMap<String, Double> documentVectorTfIdf;
		HashMap<String, Double> queryVectorTfIdf = new HashMap<String, Double>();

		for (int i = 0; i < docIsRelevant.length; i++) {
			if (docIsRelevant[i]) {
				PostingsEntry pE = results.get(i);
				documentVector = tokenize(results.get(i), indexer);
				// System.out.println("documentVector.size() "
				// + documentVector.size());
				documentVectorTfIdf = tfIdfDocVector(documentVector, indexer,
						pE.docID);
				// System.out.println("documentVectorTfIdf.size() "
				// + documentVectorTfIdf.size());
				optimalQueryVector = aggregateVectors(optimalQueryVector,
						documentVectorTfIdf);

			}
		}

		for (String s : optimalQueryVector.keySet()) {
			// System.out.println(s);
			double betaTfIdf = beta * optimalQueryVector.get(s);
			optimalQueryVector.put(s, betaTfIdf);
		}

		for (int i = 0; i < terms.size(); i++) {
			double tf = 1;
			double df = indexer.index.getPostings(terms.get(i)).size();
			double len = terms.size();
			int N = indexer.index.docLengths.size();
			double tfIdf = getTfIdf(tf, df, len, N);
			queryVectorTfIdf.put(terms.get(i), tfIdf * alpha);
		}

		for (String s : queryVectorTfIdf.keySet()) {
			double newScore;
			if (optimalQueryVector.containsKey(s)) {
				newScore = optimalQueryVector.get(s) + queryVectorTfIdf.get(s);

			} else {
				newScore = queryVectorTfIdf.get(s);
			}
			optimalQueryVector.put(s, newScore);
		}

		terms = new LinkedList<String>();
		weights = new LinkedList<Double>();
		for (String s : optimalQueryVector.keySet()) {
			System.out.println(s);
			// System.out.println(optimalQueryVector.get(s));
			terms.add(s);
			weights.add(optimalQueryVector.get(s));
		}
		System.out.println("----------------------");
		System.out.println("Terms " + terms.size());
		System.out.println("Weights " + weights.size());
		System.out.println("optimal " + optimalQueryVector.size());
		System.out.println();

	}

	private HashMap<String, Integer> tokenize(PostingsEntry pE, Indexer indexer) {

		HashMap<String, Integer> documentVector = new HashMap<String, Integer>();

		Reader reader;
		try {
			reader = new FileReader(new File(indexer.index.docIDs.get(""
					+ pE.docID)));
			SimpleTokenizer tok = new SimpleTokenizer(reader);
			while (tok.hasMoreTokens()) {
				String token = tok.nextToken();
				if (documentVector.containsKey(token)) {
					int i = documentVector.get(token);
					documentVector.put(token, i + 1);
				} else {
					documentVector.put(token, 1);
				}
			}

			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return documentVector;
	}

	private HashMap<String, Double> tfIdfDocVector(
			HashMap<String, Integer> documentVector, Indexer indexer, int docID) {

		HashMap<String, Double> documentVectorTfIdf = new HashMap<String, Double>();

		Object[] allKeys = documentVector.keySet().toArray();

		for (Object o : allKeys) {
			double tf = documentVector.get(o);
			double df = indexer.index.getPostings(o.toString()).size();
			// System.out.println("Her komma docLengths.size()"
			// + indexer.index.docLengths.size());
			// System.out.println("Her komma toString" + o.toString());
			double len = indexer.index.docLengths.get(Integer.toString(docID));
			int N = indexer.index.docLengths.size();
			double tfIdf = getTfIdf(tf, df, len, N);
			documentVectorTfIdf.put(o.toString(), tfIdf);
		}

		return documentVectorTfIdf;
	}

	private double getTfIdf(double tf, double df, double len, int N) {
		double idf = 0;
		double tfIdf = 0;

		// len = 1;
		idf = Math.log(N / df);
		tfIdf = tf * idf / len;

		return tfIdf;
	}

	private HashMap<String, Double> aggregateVectors(
			HashMap<String, Double> optimalQueryVector,
			HashMap<String, Double> documentVector) {

		Object[] allDocumentKeys = documentVector.keySet().toArray();

		for (Object o : allDocumentKeys) {
			String oStr = o.toString();
			double score = documentVector.get(oStr);
			if (optimalQueryVector.containsKey(oStr)) {
				double currentScore = optimalQueryVector.get(oStr);
				optimalQueryVector.put(oStr, (score + currentScore));
			} else {
				optimalQueryVector.put(oStr, score);
			}
		}

		return optimalQueryVector;
	}
}
