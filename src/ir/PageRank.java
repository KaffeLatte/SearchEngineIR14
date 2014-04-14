//package ir;

/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2012
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Random;
import java.util.StringTokenizer;

public class PageRank {

	/**
	 * Maximal number of documents. We're assuming here that we don't have more
	 * docs than we can keep in main memory.
	 */
	final static int MAX_NUMBER_OF_DOCS = 2000000;

	/**
	 * Mapping from document names to document numbers.
	 */
	Hashtable<String, Integer> docNumber = new Hashtable<String, Integer>();

	/**
	 * Mapping from document numbers to document names
	 */
	String[] docName = new String[MAX_NUMBER_OF_DOCS];

	/**
	 * A memory-efficient representation of the transition matrix. The outlinks
	 * are represented as a Hashtable, whose keys are the numbers of the
	 * documents linked from.
	 * <p>
	 * 
	 * The value corresponding to key i is a Hashtable whose keys are all the
	 * numbers of documents j that i links to.
	 * <p>
	 * 
	 * If there are no outlinks from i, then the value corresponding key i is
	 * null.
	 */
	Hashtable<Integer, Hashtable<Integer, Boolean>> link = new Hashtable<Integer, Hashtable<Integer, Boolean>>();

	/**
	 * The number of outlinks from each node.
	 */
	int[] out = new int[MAX_NUMBER_OF_DOCS];

	/**
	 * The number of documents with no outlinks.
	 */
	int numberOfSinks = 0;

	/**
	 * The probability that the surfer will be bored, stop following links, and
	 * take a random jump somewhere.
	 */
	final static double BORED = 0.15;

	/**
	 * Convergence criterion: Transition probabilities do not change more that
	 * EPSILON from one iteration to another.
	 */
	final static double EPSILON = 0.0001;

	/**
	 * Never do more than this number of iterations regardless of whether the
	 * transistion probabilities converge or not.
	 */
	final static int MAX_NUMBER_OF_ITERATIONS = 1000;

	HashMap<Integer, Double> pageRanks;
	HashMap<Double, Integer> inversePageRanks;

	// double[] x;

	/* --------------------------------------------- */

	public PageRank(String filename) {
		int noOfDocs = readDocs(filename);
		computePagerank(noOfDocs);
	}

	/* --------------------------------------------- */

	/**
	 * Reads the documents and creates the docs table. When this method finishes
	 * executing then the @code{out} vector of outlinks is initialised for each
	 * doc, and the @code{p} matrix is filled with zeroes (that indicate direct
	 * links) and NO_LINK (if there is no direct link.
	 * <p>
	 * 
	 * @return the number of documents read.
	 */
	int readDocs(String filename) {
		int fileIndex = 0;
		try {
			System.err.print("Reading file... ");
			BufferedReader in = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = in.readLine()) != null
					&& fileIndex < MAX_NUMBER_OF_DOCS) {
				int index = line.indexOf(";");
				String title = line.substring(0, index);
				Integer fromdoc = docNumber.get(title);
				// Have we seen this document before?
				if (fromdoc == null) {
					// This is a previously unseen doc, so add it to the table.
					fromdoc = fileIndex++;
					docNumber.put(title, fromdoc);
					docName[fromdoc] = title;
				}
				// Check all outlinks.
				StringTokenizer tok = new StringTokenizer(
						line.substring(index + 1), ",");
				while (tok.hasMoreTokens() && fileIndex < MAX_NUMBER_OF_DOCS) {
					String otherTitle = tok.nextToken();
					Integer otherDoc = docNumber.get(otherTitle);
					if (otherDoc == null) {
						// This is a previousy unseen doc, so add it to the
						// table.
						otherDoc = fileIndex++;
						docNumber.put(otherTitle, otherDoc);
						docName[otherDoc] = otherTitle;
					}
					// Set the probability to 0 for now, to indicate that there
					// is
					// a link from fromdoc to otherDoc.
					if (link.get(fromdoc) == null) {
						link.put(fromdoc, new Hashtable<Integer, Boolean>());
					}
					if (link.get(fromdoc).get(otherDoc) == null) {
						link.get(fromdoc).put(otherDoc, true);
						out[fromdoc]++;
					}
				}
			}
			if (fileIndex >= MAX_NUMBER_OF_DOCS) {
				System.err
						.print("stopped reading since documents table is full. ");
			} else {
				System.err.print("done. ");
			}
			// Compute the number of sinks.
			for (int i = 0; i < fileIndex; i++) {
				if (out[i] == 0)
					numberOfSinks++;
			}
		} catch (FileNotFoundException e) {
			System.err.println("File " + filename + " not found!");
		} catch (IOException e) {
			System.err.println("Error reading file " + filename);
		}
		System.err.println("Read " + fileIndex + " number of documents");
		return fileIndex;
	}

	/* --------------------------------------------- */

	/*
	 * Computes the pagerank of each document.
	 */
	void computePagerank(int numberOfDocs) {
		//
		// YOUR CODE HERE
		//
		// double[] xxx = noSinksApproximation(numberOfDocs);
		double[] xxx = MCcompletePath(numberOfDocs);
		// double[] xxx = MCendPointWithRandomStart(numberOfDocs);

		Node[] pageRankings = new Node[numberOfDocs];
		pageRanks = new HashMap<Integer, Double>();

		for (int i = 0; i < pageRankings.length; i++) {
			pageRankings[i] = new Node(Integer.parseInt(docName[i]), xxx[i]);
			// pageRanks.put(i, xxx[i]);
			pageRanks.put(Integer.parseInt(docName[i]), xxx[i]);
		}

		// savePageRank();

		Arrays.sort(pageRankings);

		System.out.println("50 första");
		for (int i = 0; i < 50; i++) {
			System.out.println(pageRankings[i].docID + " "
					+ pageRankings[i].score);
		}

		System.out.println("50 sista");
		for (int i = numberOfDocs - 1; i >= (numberOfDocs - 50); i--) {
			System.out.println(pageRankings[i].docID + " "
					+ pageRankings[i].score);
		}

		// double[] x = powerIteration(numberOfDocs);
		// createPageRankHashMap(x, numberOfDocs);
		// printPageRank(x, numberOfDocs);
		// savePageRank();

	}

	private double[] powerIteration(int numberOfDocs) {
		double[][] probabilityMatrix = createProbabilityMatrix(link,
				numberOfDocs);

		double[] x = new double[numberOfDocs];
		x[0] = 0.1;
		x[1] = 0.1;
		x[2] = 0.1;
		x[3] = 0.1;
		x[4] = 0.1;
		x[5] = 0.1;
		x[6] = 0.1;
		x[7] = 0.1;
		x[8] = 0.1;
		x[9] = 0.1;

		double[] xprim = new double[numberOfDocs];

		int count = 0;
		while (arrayDiff(x, xprim) > EPSILON
				&& count < MAX_NUMBER_OF_ITERATIONS) {
			xprim = x;
			x = matrixMultiplication(x, probabilityMatrix);
			count++;
		}

		System.out.println("---------------------------------");
		System.out.println("Arraydiff " + arrayDiff(x, xprim));
		System.out.println("Number of iterations in while " + count);

		return x;

	}

	private double[] matrixMultiplication(double[] vector, double[][] matrix) {
		double[] result = new double[vector.length];

		for (int i = 0; i < 1; i++) {
			for (int j = 0; j < matrix.length; j++) {
				for (int k = 0; k < vector.length; k++) {
					result[j] += vector[k] * matrix[k][j];
					// System.out.println(result[i]);
				}
			}
		}

		return result;
	}

	private double[][] createProbabilityMatrix(
			Hashtable<Integer, Hashtable<Integer, Boolean>> link, int n) {
		double[][] probabilityMatrix = new double[n][n];

		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= n; j++) {
				if (link.get(i) == null) {
					probabilityMatrix[i - 1][j - 1] = (1 / (double) n)
							* (1 - BORED);
				} else if (link.get(i).containsKey(j)) {
					probabilityMatrix[i - 1][j - 1] = (1 / (double) link.get(i)
							.size()) * (1 - BORED);
				}
			}
		}

		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				probabilityMatrix[i][j] += BORED / n;
			}

		}

		return probabilityMatrix;

	}

	private double arrayDiff(double[] a1, double[] a2) {
		double sum = 0;
		double diff;
		for (int i = 0; i < a1.length; i++) {
			diff = a1[i] - a2[i];
			if (diff < 0) {
				diff = diff * (-1);
			}
			sum += (diff * diff);
		}

		sum = Math.sqrt(sum);

		return sum;

	}

	private void createPageRankHashMap(double[] ranks, int numberOfDocs) {
		pageRanks = new HashMap<Integer, Double>();
		inversePageRanks = new HashMap<Double, Integer>();

		for (int i = 0; i < numberOfDocs; i++) {
			pageRanks.put(i + 1, ranks[i]);
			inversePageRanks.put(ranks[i], i + 1);

		}

	}

	private void savePageRank() {
		try {
			File file = new File(
					"/afs/nada.kth.se/home/3/u1qvl923/ir/labb2/pageRanks/pageRankAllLinks2");
			FileOutputStream tFileOutputStream = new FileOutputStream(file);
			ObjectOutputStream tObjectOutputStream = new ObjectOutputStream(
					tFileOutputStream);
			tObjectOutputStream.writeObject(pageRanks);
			tObjectOutputStream.close();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	private void printPageRank(double[] x, int numberOfDocs) {
		Arrays.sort(x);
		int counter = 1;
		for (int i = numberOfDocs - 1; i > (numberOfDocs - 51); i--) {
			System.out.println(counter + "."
					+ docName[inversePageRanks.get(x[i])] + " " + x[i]);
			counter++;
		}
	}

	private double[] noSinksApproximation(int numberOfDocs) {

		// x = new double[numberOfDocs];
		// double[] xprim = new double[numberOfDocs];
		// Arrays.fill(xprim, (double) 1 / numberOfDocs);
		// Hashtable<Integer, Boolean> row = new Hashtable<Integer, Boolean>();
		//
		// int s = numberOfDocs - link.size();
		//
		// int count = 0;
		// while (arrayDiff(x, xprim) > EPSILON /* && count < 20 */) {
		// x = xprim.clone();
		// xprim = new double[numberOfDocs];
		// for (int i = 0; i < link.size(); i++) {
		// if (link.get(i) != null) {
		// row = link.get(i);
		// for (int j = 0; j < link.size(); j++) {
		// if (row.containsKey(j)) {
		// double var = x[i] * (1 - BORED) / row.size();
		// // System.out.println(var);
		// xprim[j] += var;
		// }
		// }
		// xprim[i] += BORED / numberOfDocs;
		// xprim[i] += s / numberOfDocs / numberOfDocs;
		// }
		// }
		//
		// count++;
		// }
		// System.out.println("Number of iterations: " + count);

		double[] xprime = new double[numberOfDocs];
		double[] x = new double[numberOfDocs];
		xprime[0] = 1;
		// TODO initiate x to something good and compare x to xprime to see if
		// they differ more than epsilon
		int iteration = 0;
		while (arrayDiff(x, xprime) > EPSILON
				&& iteration < MAX_NUMBER_OF_ITERATIONS) {
			iteration++;
			x = xprime.clone();
			xprime = new double[numberOfDocs];
			for (int i = 0; i < numberOfDocs; i++) {
				if (link.containsKey(i)) {
					Object[] allLinks = link.get(i).keySet().toArray();
					for (int j = 0; j < allLinks.length; j++) {
						int doc = (Integer) allLinks[j];
						xprime[doc] += x[i] * (1 - BORED) / out[i];
					}

				}

				xprime[i] += BORED / numberOfDocs;
			}
		}
		System.out.println("It has converged after " + iteration
				+ " iterations");
		// System.out.println("Found docs with no links: " + noLink +
		// " with documents " + numberOfDocs);
		x = xprime.clone();
		return x;
	}

	private class Node implements Comparable<Node> {
		int docID;
		double score;

		Node(int internalID, double score) {
			this.docID = internalID;
			this.score = score;
		}

		@Override
		public int compareTo(Node other) {
			return Double.compare(other.score, score);
		}
	}

	public double[] MCendPointWithRandomStart(int numberOfDocuments) {
		int count = 0;
		int n = 15;

		Random random = new Random(System.currentTimeMillis());

		double[] histogram = new double[numberOfDocuments];

		int current;
		while (count < numberOfDocuments) {
			current = random.nextInt(numberOfDocuments);
			int innercount = 0;
			while (innercount < n) {
				if (out[current] != 0) {
					int nextJumpIndex = random.nextInt(out[current]);
					Object[] outLinks = link.get(current).keySet().toArray();
					current = (Integer) outLinks[nextJumpIndex];
				} else {
					current = random.nextInt(numberOfDocuments);
				}

				innercount++;
			}
			histogram[current]++;
			count++;
			innercount = 0;

		}

		for (int i = 0; i < histogram.length; i++) {
			histogram[i] = histogram[i] / numberOfDocuments;
		}

		return histogram;

	}

	public double[] MCendPointWithCyclicStart(int numberOfDocuments) {
		int m = 10;
		int n = 100;

		Random random = new Random(System.currentTimeMillis());

		double[] histogram = new double[numberOfDocuments];

		int mcount = 0;
		int ncount = 0;
		for (int i = 0; i < numberOfDocuments; i++) {
			while (mcount < m) {
				int current = i;

				while (ncount < n) {
					if (random.nextDouble() > BORED && out[current] != 0) {
						int nextJumpIndex = random.nextInt(out[current]);
						Object[] outLinks = link.get(current).keySet()
								.toArray();
						current = (Integer) outLinks[nextJumpIndex];
						// System.out.println("Next jump to " + current);
					} else {
						current = random.nextInt(numberOfDocuments);
					}

					ncount++;
				}
				ncount = 0;
				histogram[current]++;
				mcount++;
			}
			mcount = 0;
		}

		for (int i = 0; i < histogram.length; i++) {
			histogram[i] = histogram[i] / numberOfDocuments;
		}
		System.out.println("Total number of iterations "
				+ (numberOfDocuments * n * m));
		return histogram;
	}

	private double[] MCcompletePath(int numberOfDocuments) {
		int m = 10;
		int n = 100;

		Random random = new Random(System.currentTimeMillis());

		double[] histogram = new double[numberOfDocuments];

		int mcount = 0;
		int ncount = 0;
		for (int i = 0; i < numberOfDocuments; i++) {
			while (mcount < m) {
				int current = i;

				while (ncount < n) {
					if (random.nextDouble() > BORED && out[current] != 0) {
						int nextJumpIndex = random.nextInt(out[current]);
						Object[] outLinks = link.get(current).keySet()
								.toArray();
						current = (Integer) outLinks[nextJumpIndex];
						histogram[current]++;
						// System.out.println("Next jump to " + current);
					} else {
						current = random.nextInt(numberOfDocuments);
						histogram[current]++;
					}

					ncount++;
				}
				ncount = 0;
				mcount++;
			}
			mcount = 0;
		}
		int normalizor = numberOfDocuments * m * n;

		for (int i = 0; i < histogram.length; i++) {
			histogram[i] = histogram[i] / normalizor;
		}
		System.out.println("Total number of iterations "
				+ (numberOfDocuments * n * m));
		return histogram;
	}

	/* --------------------------------------------- */

	public static void main(String[] args) {

		if (args.length != 1) {
			System.err.println("Please give the name of the link file");
		} else {

			new PageRank(args[0]);
		}
		// Hashtable<Integer, Hashtable<Integer, Boolean>> hasch = new
		// Hashtable<Integer, Hashtable<Integer, Boolean>>();
		// Hashtable<Integer, Boolean> l1 = new Hashtable<Integer, Boolean>();
		// // l1.put(2, true);
		// // l1.put(3, true);
		// // hasch.put(1, l1);
		//
		// Hashtable<Integer, Boolean> l2 = new Hashtable<Integer, Boolean>();
		// l2.put(1, true);
		// l2.put(3, true);
		// hasch.put(2, l2);
		//
		// Hashtable<Integer, Boolean> l3 = new Hashtable<Integer, Boolean>();
		// l3.put(1, true);
		// l3.put(2, true);
		// hasch.put(3, l3);
		//
		// double[][] wtf = pr.createProbabilityMatrix(hasch, 3);
		//
		// for (int i = 0; i < 3; i++) {
		// for (int j = 0; j < 3; j++) {
		// System.out.print(wtf[i][j] + " ");
		// }
		// System.out.println();
		// }

	}
}