package tn.common.fs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.text.BreakIterator;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * 
 * This encapsulates a {@link FileChannel} and provides methods to deal with
 * strings.
 * 
 * @author Senthu Sivasambu 10 Aug 2013
 * 
 */

public class TextFileChannelImpl implements TextFileChannel{

	private final FileChannel fc;

	// character-buffer-array maintained in memory
	private char[] cbfa = null;

	private final String channelId;

	// file offset
	private long fp = 0L;

	// buffer offset
	private int bp = 0; // often the values 'bp' can take is less than that of
						// 'fp'

	// pointer to traverse character buffer -- TODO when getting rid of char
	// buff remove this as well.
	private int cbp = 0;

	private long blockSize = 0L;

	private int retrieveSize = 0;

	// private MappedByteBuffer mbf = null;

	private BreakIterator bit = null;

	private BreakIterator cit = null;

	private Collator col = null;

	private boolean lastBlock = false;

	private boolean eof = false;

	private boolean isMore = false;

	private String charSet = null; // default

	private Comparator<String> comp = null;

	private Logger LOGGER = null;

	// required by the sorted to park the last yanked one
	private String lastKey = null;
	private String lastKeyCache = null;

	private TextFileChannelImpl(FileChannel fc, Comparator<String> comp,
			String channelID, long blockSize, Locale locale, String charSet,
			Logger logger) {
		this.fc = fc;
		this.channelId = channelID;
		this.blockSize = blockSize;
		this.retrieveSize = Math.round((blockSize / 4)); // default
		if (locale != null) {
			bit = BreakIterator.getWordInstance();
			cit = BreakIterator.getCharacterInstance();
		} else {
			bit = BreakIterator.getWordInstance(locale);
			cit = BreakIterator.getCharacterInstance(locale);
		}
		col = Collator.getInstance(locale);
		this.charSet = charSet;
		this.comp = comp;
		LOGGER = logger;
	}

	public static TextFileChannelImpl createDefaultInstance(File file,
			Comparator<String> comp, long blockSize, Locale locale,
			File logSpace) {
		if (file == null) {
			throw new IllegalArgumentException("@param file cannot be null");
		}

		FileChannel fc = null;
		try {
			fc = new RandomAccessFile(file, "r").getChannel();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		final Logger logger = setupLogger(logSpace, file.getName());

		return new TextFileChannelImpl(fc, comp, file.getName(), blockSize, locale,
				"UTF-16BE", logger);
	}

	private static Logger setupLogger(File logSpace, String fcName) {
		final Logger logger = Logger.getLogger("SORT_CHANNEL_LOGGER_" + fcName);
		logger.setLevel(Level.ALL);

		try {

			FileHandler fileHandler = new FileHandler(new File(logSpace,
					(fcName + "_log.txt")).getPath());

			SimpleFormatter formatter = new SimpleFormatter();
			fileHandler.setFormatter(formatter);
			logger.addHandler(fileHandler);

		} catch (SecurityException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return logger;
	}

	public static TextFileChannelImpl createInstance(File file,
			Comparator<String> comp, long blockSize, Locale locale,
			String charSet, File logSpace) {
		if (file == null) {
			throw new IllegalArgumentException("@param file cannot be null");
		}

		FileChannel fc = null;
		try {
			fc = new RandomAccessFile(file, "r").getChannel();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		final Logger logger = setupLogger(logSpace, file.getName());

		return new TextFileChannelImpl(fc, comp, file.getName(), blockSize, locale,
				charSet, logger);
	}

	/**
	 * This method must be invoked before any methods are invoked. it would make
	 * any effect only for the first time invoked.<br>
	 * This loads file content into allocated memory buffer. Having a separate
	 * method like this also allows the user to choose the moment to invoke
	 * loading contents into memory (first time only - subsequently the file
	 * contents are automatically loaded)
	 * 
	 * @throws IOException
	 *             - if any problem is encountered during load operation
	 */
	public void load() throws IOException {
		if (cbfa == null) {
			loadNext();
		}
	}

	private boolean loadNext() throws IOException {

		if (fp >= fc.size()) {
			throw new IllegalAccessError("EOF reched..");
		}

		long fetchSize = blockSize;
		// handle last block - can be less than the standard block size
		if ((fp + fetchSize) >= fc.size()) {
			fetchSize = fc.size() - fp;
			lastBlock = true;
		}
		MappedByteBuffer mbf = fc.map(MapMode.READ_ONLY, fp, fetchSize);
		cbfa = Charset.forName("UTF-8").decode(mbf).array(); // TODO - -get
																// UTF-8/16 this
																// from a user
																// configuration
																// file
		bp = 0; // reset this to beginning of byte buffer
		cbp = 0; // reset this to beginning of character buffer
		fp += fetchSize;

		double cper = ((double) fp / (double) fc.size()) * 100;
		LOGGER.log(Level.INFO,
				"Loading new block into memory [Size : " + fc.size() + "][FP :"
						+ fp + "][so far completed :" + Math.round(cper) + "%]"
						+ toString());

		if (fetchSize > 0)
			return true; // return success.
		else {
			lastBlock = true; // must have arelady been set at this point anyway
			return false;
		}
	}

	/**
	 * retrieves the next word (key) in this file channel. <br>
	 * It will alter the buffer position
	 * 
	 * @return - can possibly return empty buffer if end of file has been
	 *         reached
	 * @throws IOException
	 */

	public String nextKey() throws IOException {
		return nextKey(false);
	}

	/**
	 * peek and retrieves the next word (key) in this file channel. <br>
	 * unlike {@link #nextKey()} calling this method will <b>not</b> alter the
	 * buffer position
	 * 
	 * @return - can possibly return empty/null buffer if end of file has been
	 *         reached
	 * @throws IOException
	 */
	public String peek() throws IOException {
		return nextKey(true);
	}

	private String nextKey(boolean isPeek) throws IOException {
		if (cbfa == null) {
			throw new IllegalStateException(
					"This channel has not been initialised yet. Call load() first");
		}

		CharBuffer cbf = CharBuffer.allocate(100); // TODO (100 byte) large word
													// -- one of the places user
													// provided tuning could be
													// helpful

		// meaning we still can read words from this buffer
		/* bp */int sBoundry = skipMarkers(cbp/* bp */, isPeek);

		if (((sBoundry == -1) || (sBoundry >= cbfa.length))/* bp >= blockSize */
				&& !lastBlock) {
			if (loadNext())
				sBoundry = 0;
			else
				sBoundry = -1;
		}

		if (((sBoundry == -1) || (sBoundry >= cbfa.length))/* bp >= blockSize */
				&& lastBlock)// just trying to be explicit
		{
			eof = true;
			return null;// CharBuffer.allocate(0).toString();
		}

		boolean isDel = false; // Delimiter
		int cbpStart = sBoundry;
		int ccount = 0;
		do {
			if (cbpStart >= cbfa.length/* bp >= blockSize */) {
				if (!lastBlock) {
					loadNext();
					cbpStart = 0;
				} else {
					break;
				}
			}

			int codePointAt = Character.codePointAt(cbfa, cbpStart/* bp */);

			isDel = Character.isWhitespace(codePointAt); // TODO can be made
															// user defined

			int c = 0;
			if (!isDel && Character.isValidCodePoint(codePointAt)) {
				for (char ch : Character.toChars(codePointAt)) {
					cbf.append(ch);
					ccount++;
					c++;
				}
			}

			cbpStart += c;
			/* bp++; */
		} while (!isDel);

		if (!isPeek)
			cbp = cbpStart;

		// cbf.get(dst,offset,length) is not working :(
		char[] tmp = new char[ccount];
		for (int i = 0; i < ccount; i++) {
			tmp[i] = cbf.get(i);
		}
		lastKeyCache = String.valueOf(tmp);
		return lastKeyCache;
	}

	/**
	 * This method skips whitespace and newlines from this point where buffer
	 * offset is current is.
	 * 
	 * @return buffer offset
	 */
	private int skipMarkers(int currentBufferOffset, boolean isPeek) {
		int codePointAt = -1;
		boolean isMarker = false;
		int offset = cbp;
		do {
			if (offset >= cbfa.length)
				return -1;
			codePointAt = Character.codePointAt(cbfa, offset/* bp */);
			if (!Character.isWhitespace(codePointAt)/*
													 * TODO markers are user
													 * provided - or discovered
													 * by closely examining
													 * target data domain -
													 * provide ability to list
													 * custom markers,preferably
													 * from a property file
													 */) {
				break;
			}
			offset++;// whitespace is single char long

			/* bp++; */
			if (offset >= cbfa.length) {
				break;
			}

		} while (!isMarker);

		if (!isPeek)
			cbp = offset;

		return /* bp */offset;
	}

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
	public String[] keysLessThanOrEqual(String targetKey) throws IOException {
		return getKeys(targetKey, true, retrieveSize);
	}

	public String[] keysGreaterThan(String targetKey) throws IOException {
		return getKeys(targetKey, false, retrieveSize);
	}

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
	public String[] nextKeys(int bufferSize) throws IOException {
		return getKeys(null, true, bufferSize);
	}

	public String[] nextKeys(String targetKey, int bufferSize)
			throws IOException {
		return getKeys(targetKey, true, bufferSize);
	}

	private String[] getKeys(String targetKey, boolean isLT/*
															 * true if to return
															 * keys less or
															 * equal to than
															 * target or false
															 * otherwise
															 */, int bufferSize)
			throws IOException {
		if (cbfa == null) {
			throw new IllegalStateException(
					"This channel has not been initialised yet. Call load() first");
		}

		// order preserving data structure
		ArrayList<String> ar = new ArrayList<String>();
		int size = 0;

		// investigate use of collation keys - performance improvement vs. extra
		// space
		String infc = peek();

		while (size < bufferSize && infc != null && !"".equals(infc)) {
			size += infc.getBytes(charSet).length;
			if (isSelect(infc, targetKey, isLT)) {
				if ((size < bufferSize)) {
					infc = nextKey();
					if (infc != null) {
						ar.add(infc);
						isMore = true;
					} else {// trying to be explicit on conditions
						isMore = false;
						break;
					}
				} else {
					isMore = false;
					break;
				}
			} else {
				isMore = false;
				break;
			}
			infc = peek();
		}

		if (ar.size() == 0)
			return null;

		return ar.toArray(new String[ar.size()]);
	}

	// returns true fall null target values
	private boolean isSelect(String src, String target, boolean isLT) {
		if (target == null) {
			return true;
		}
		if (isLT)
			return comp.compare(src, target) <= 0;
		else
			return comp.compare(src, target) >= 0;
	}

	public void rewind() {

		if (lastKey != null) {
			throw new IllegalStateException(
					"this has already been rewinded..yank first");
		}

		if (lastKeyCache == null) {
			throw new IllegalStateException(
					"nothing in cache .. call nextKey() first..");
		}

		lastKey = lastKeyCache;
	}

	public boolean isEOF() {
		return eof;
	}

	public boolean isMore() {
		return isMore;
	}

	public void setRetrieveSize(int retrieveSize) {
		this.retrieveSize = retrieveSize;
	}

	public void close() throws IOException {
		fc.close();
	}

	public long size() throws IOException {
		return fc.size();
	}

	@Override
	public String toString() {
		return "[id:" + channelId + "][EOF:" + eof + "]" + "[is last block:"
				+ lastBlock + "]" + "[buffer pointer:" + cbp + "]";
	}

	@Override
	public FileChannel getFileChannel() {
		return fc;
	}

}
