package tn.common.data;

/**
 * String represents an abstract notion of data source. It can be a file ,
 * database or anything else for that matter. <br>
 * <br>
 * 
 * Data sources are meant to be immutable - however an implementation may chose
 * to make it mutable.
 * 
 * @author Senthu Sivasambu
 * 
 */
public interface DataSource {

	public String getID();

	public String getName();

	public void log();

}
