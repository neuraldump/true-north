package tn.common.engine;

import java.util.Comparator;
import java.util.Set;

import tn.common.data.DataSource;

/**
 * Encapsulates logic to merge two or more {@link DataSource} using multi-way
 * merge technique.
 * 
 * @author Senthu Sivasambu, September 04, 2013
 * 
 */
public interface MultiwayMerger<T> {

	public void merge(Set<T> mergeSet);

}
