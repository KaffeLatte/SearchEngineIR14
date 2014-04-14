package ir;

import java.util.Comparator;

public class PostingsEntryComparator implements Comparator<PostingsEntry> {

	@Override
	public int compare(PostingsEntry pE1, PostingsEntry pE2) {
		if (pE1.getScore() < pE2.getScore()) {
			return -1;
		}
		if (pE1.getScore() > pE2.getScore()) {
			return 1;
		}
		return 0;
	}
}