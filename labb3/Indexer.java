/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */

package ir;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

/**
 * Processes a directory structure and indexes all PDF and text files.
 */
public class Indexer {

	/** The index to be built up by this indexer. */
	public Index index;

	/** The next docID to be generated. */
	private int lastDocID = 0;

	/* ----------------------------------------------- */

	/** Generates a new document identifier as an integer. */
	private int generateDocID() {
		return lastDocID++;
	}

	/** Generates a new document identifier based on the file name. */
	private int generateDocID(String s) {
		return s.hashCode();
	}

	/* ----------------------------------------------- */

	/**
	 * Initializes the index as a HashedIndex.
	 */
	public Indexer() {
		index = new HashedIndex();
	}

	/* ----------------------------------------------- */

	/**
	 * Tokenizes and indexes the file @code{f}. If @code{f} is a directory, all
	 * its files and subdirectories are recursively processed.
	 */
	public void processFiles(ArrayList<Node> sortedFiles) {

		int nrDocCounter = 0;

		for (int i = 0; i < sortedFiles.size(); i++) {
			Node n = sortedFiles.get(i);
			nrDocCounter++;
			File f = new File(n.name);
			// System.err.println( "Indexing " + f.getPath() );
			// First register the document and get a docID
			int docID = n.docID;
			index.docIDs.put("" + docID, f.getPath());

			try {

				// Read the first few bytes of the file to see if it is
				// likely to be a PDF
				Reader reader = new FileReader(f);
				char[] buf = new char[4];
				reader.read(buf, 0, 4);
				// added line below
				reader.close();
				// added line above
				if (buf[0] == '%' && buf[1] == 'P' && buf[2] == 'D'
						&& buf[3] == 'F') {
					// We assume this is a PDF file
					try {
						String contents = extractPDFContents(f);
						reader = new StringReader(contents);
					} catch (IOException e) {
						// Perhaps it wasn't a PDF file after all
						reader = new FileReader(f);
					}
				} else {
					// We hope this is ordinary text
					reader = new FileReader(f);
				}
				SimpleTokenizer tok = new SimpleTokenizer(reader);
				int offset = 0;
				String token;
				while (tok.hasMoreTokens()) {
					token = tok.nextToken();
					insertIntoIndex(docID, token, offset++);
				}
				index.docLengths.put("" + docID, offset);
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			// sortedFiles.remove(i);
		}

	}

	public void startProcessFiles(ArrayList<String> files) {
		ArrayList<Node> nodes = sortFiles(files);

		for (int i = 0; i < 10; i++) {
			ArrayList<Node> subList = new ArrayList<Node>();
			for (int j = 0; j < nodes.size() / 10; j++) {
				subList.add(nodes.get(i * nodes.size() / 10 + j));
			}
			processFiles(subList);
		}

	}

	public ArrayList<Node> sortFiles(ArrayList<String> files) {
		HashMap<Integer, Double> pageRanks = getPageRanksFromFile();

		ArrayList<Node> nodes = new ArrayList<Node>();

		System.out.println("Files.size()" + files.size());

		for (String s : files) {
			String[] ss = s.split("/");
			String[] sss = ss[8].split("\\.");

			int docID = Integer.parseInt(sss[0]);
			nodes.add(new Node(s, docID,
					(int) ((pageRanks.get(docID)) * Integer.MAX_VALUE)));
		}

		Collections.sort(nodes);

		System.out.println("Nodes.size()" + nodes.size());

		return nodes;

	}

	private class Node implements Comparable<Node> {
		public String name;
		public int docID;
		public int score;

		Node(String name, int docID, int score) {
			this.name = name;
			this.docID = docID;
			this.score = score;
		}

		@Override
		public int compareTo(Node other) {
			return Double.compare(other.score, score);
		}

	}

	private HashMap<Integer, Double> getPageRanksFromFile() {
		File file = new File(
				"/afs/nada.kth.se/home/3/u1qvl923/ir/labb2/pageRanks/pageRankAllLinks");
		try {
			FileInputStream f = new FileInputStream(file);
			ObjectInputStream s = new ObjectInputStream(f);
			HashMap<Integer, Double> pageRanks = (HashMap<Integer, Double>) s
					.readObject();
			s.close();
			return pageRanks;

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

		return null;

	}

	/* ----------------------------------------------- */

	/**
	 * Extracts the textual contents from a PDF file as one long string.
	 */
	public String extractPDFContents(File f) throws IOException {
		FileInputStream fi = new FileInputStream(f);
		PDFParser parser = new PDFParser(fi);
		parser.parse();
		fi.close();
		COSDocument cd = parser.getDocument();
		PDFTextStripper stripper = new PDFTextStripper();
		String result = stripper.getText(new PDDocument(cd));
		cd.close();
		return result;
	}

	/* ----------------------------------------------- */

	/**
	 * Indexes one token.
	 */
	public void insertIntoIndex(int docID, String token, int offset) {
		index.insert(token, docID, offset);
	}
}
