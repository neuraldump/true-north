package tn.common.data;

import java.text.CollationKey;
import java.util.Comparator;

/**
 * An interface every sort algorithm to be used in this framework must
 * implement.
 * 
 * @author Senthu Sivasambu, Sept 3, 2013
 * 
 * @param <T>
 *            - can be anything typically objects like {@link String},
 *            {@link CollationKey}
 */
public interface Sortable<T> extends Processable {

	public Comparator<T> getComparator();

}
