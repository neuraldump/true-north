package tn.algo;

import java.text.CollationKey;

import tn.common.data.DataSource;

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
public interface Strategy<T> {
	/**
	 * @param input
	 *            - a non null reference of {@link DataSource}
	 */
	public void run(T input);
}
