package tn.common.data;

/**
 * String represents an abstract notion of data sink. It can be a file or
 * database or anything else for that matter. <br>
 * <br>
 * 
 * Data sinks are mutable subject to external permission restrictions.
 * 
 * @author Senthu Sivasambu, September 4, 2013
 * 
 */
public interface DataSink {

	public String getID();

	public String getName();

	public String getCharSet();

	public String getEncoding();

	public void log();
}
