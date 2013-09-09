package tn.common.data;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

import tn.common.Configuration;

/**
 * 
 * This class is an implementation of {@link Comparator} interface; to be used in merge routine. 
 * Why a merge routine may use a different implementation of {@link Comparator} than the one used 
 * for sort routine is not clearly established at this point.
 * 
 * @author Senthu Sivsambu, 05 September, 2013
 *
 */
public class MergeComparator implements Comparator<String>{

	private  Collator col = null;
	
	private Locale locale = null;
	
	private MergeComparator(String locString) {
		// no public instantiation
		this.locale = new Locale(locString);
		col = Collator.getInstance(locale);
	}
	
	public static MergeComparator getInstance(){
		String locString = (String) Configuration.getInstance().get(Configuration.IN_CHAR_SET);
		return new MergeComparator(locString);
	}
	
	
	@Override
	public int compare(String s1, String s2) {
		int ls1 = s1.length();
		int ls2 = s2.length();
		int max = ls1 <= ls2 ? ls1 : ls2;
		for (int i = 0; i < max; i++) {
			int r = col.compare(String.valueOf(s1.charAt(i)),
					String.valueOf(s2.charAt(i)));
			if (r != 0)
				return r;
		}
		if (ls1 == ls2)
			return 0;
		if (ls1 > ls2)
			return 1;
		else
			return -1;
	}

}
