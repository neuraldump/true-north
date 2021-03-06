package tn.common.fs;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * An interface to define the methods available to treat a text {@link File} as
 * file channel. The methods provided here form a facade over legacy methods.
 * 
 * @author Senthu Sivasambu, September 4, 2013
 * 
 */

public interface TextFileChannel {

	/**
	 * Open for reading only. Invoking any of the write methods of the resulting object will 
	 * cause an IOException to be thrown. 
	 * */
	public static final String READ = "r";
	/**
	 * Open for reading and writing. If the file does not already exist then an attempt will 
	 * be made to create it.
	 */
	public static final String READ_WRITE = "rw";
	/**
	 * Open for reading and writing, as with "rw", and also require that every update to the 
	 * file's content or metadata be written synchronously to the underlying storage device.
	 */
	public static final String READ_WRITE_S = "rws";
	/**
	 * Open for reading and writing, as with "rw", and also require that every update to the 
	 * file's content be written synchronously to the underlying storage device. 
	 */
	public static final String READ_WRITE_D = "rwd";
	
	/**
	 * retrieves the next word (key) in this file channel. <br>
	 * It will alter the buffer position
	 * 
	 * @return - can possibly return empty buffer if end of file has been
	 *         reached
	 * @throws IOException
	 */
	public String nextKey() throws IOException;

	/**
	 * peek and retrieves the next word (key) in this file channel. <br>
	 * unlike {@link #nextKey()} calling this method will <b>not</b> alter the
	 * buffer position
	 * 
	 * @return - can possibly return empty/null buffer if end of file has been
	 *         reached
	 * @throws IOException
	 */
	public String peek() throws IOException;

	/**
	 * retrieves all the words(keys) in this file channel whose values are less
	 * or equal to @param targetKey<br>
	 * 
	 * it will alter the buffer position.
	 * 
	 * @param targetKey
	 * @return
	 * @throws IOException
	 */
	public String[] keysLessThanOrEqual(String targetKey) throws IOException;

	public String[] keysGreaterThan(String targetKey) throws IOException;

	/**
	 * This retrieves keys from the chunks until their accumulated size is less
	 * than or equal to @param bufferSize
	 * 
	 * @param bufferSize
	 *            -- the limit of sizes of keys (in bytes)
	 * @return array of valid keys whose aggregated size are less than @param
	 *         bufferSize
	 * @throws IOException
	 */
	public String[] nextKeys(int bufferSize) throws IOException;

	public void rewind();

	public boolean isEOF();

	public boolean isMore();

	public void close() throws IOException;

	public long size() throws IOException;
	
	public FileChannel getFileChannel();
}
