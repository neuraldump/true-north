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
import java.nio.charset.CharsetDecoder;
import java.text.BreakIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

import tn.SortMaster;
import tn.common.Configuration;
import tn.common.data.EnumerableTextSource;
import tn.common.data.Sortable;
import tn.common.engine.Distributable;

/**
 * represents a file base data source. However a file can be text file or other
 * binary data. Therefore that type information is deferred until the time of
 * creation of such data source
 * 
 * @author Senthu Sivasambu
 * 
 */
public abstract class TextFileDataSource implements TextFileChannel,
		EnumerableTextSource, Distributable<File, File>, Sortable<String> {

	private TextFileChannel fc = null;
	
	private File file= null;
	
	private String id;
	
	private String name;
	
	private String charSet;
	
	private String encoding;
	
	private String language;
	
	private boolean isSorted;
	
	private String[] sorted = null;
	
	
	//TODO support for multi-locale data source is under investigation.
	private Locale locale;
	
	private File logSpace;
	

	public TextFileDataSource(File file, String id, String name,
			String charSet, String encoding,String language,String locale,File logSpace) {
		this.file = file;
		this.id=id;
		this.name=name;
		this.charSet=charSet;
		this.language = language;
		this.encoding=encoding;
		this.locale=new Locale(language,locale);
		this.logSpace=logSpace;
	}

	@Override
	public String getID() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getCharSet() {
		return charSet;
	}

	@Override
	public String getEncoding() {
		return encoding;
	}

	@Override
	public String[] asArray() {
		try{
			
			Configuration config = Configuration.getInstance();
			long blockSize = Long.valueOf(config.getProperty(Configuration.SYS_FS_BLOCK_SIZE));
			
			fc = TextFileChannelImpl.createInstance(file,TextFileChannel.READ_WRITE,getComparator() , blockSize, locale, charSet, logSpace);
			MappedByteBuffer mbf = fc.getFileChannel().map(MapMode.READ_WRITE, 0, fc.size());
			mbf.load();
	
			BreakIterator wordIterator = BreakIterator
					.getWordInstance(locale);
	
			CharsetDecoder decoder = Charset.forName(getCharSet()).newDecoder();
			CharBuffer decoded = decoder.decode(mbf);
	
			StringBuilder sb = new StringBuilder(decoded);
	
			decoded = null;
			mbf = null;
			
			String[] cin = extractWords(sb.toString(), wordIterator); 
			//sb.delete(0, (sb.length() - 1));
			sb = null;
		
			return cin;
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {

			try {
				if (fc != null)
					fc.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		throw new IllegalStateException("oops..something went wrong!");
	}

	@Override
	public tn.common.engine.Distributable.DistrState distrStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void distribute(File from, File to) {
		// TODO Auto-generated method stub
	}

	@Override
	public void log() {
		// TODO Auto-generated method stub

	}

	@Override
	public String delimiter() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String nextKey() throws IOException {
		return fc.nextKey();
	}

	@Override
	public String peek() throws IOException {
		return fc.peek();
	}

	@Override
	public String[] keysLessThanOrEqual(String targetKey) throws IOException {
		return fc.keysLessThanOrEqual(targetKey);
	}

	@Override
	public String[] keysGreaterThan(String targetKey) throws IOException {
		return fc.keysGreaterThan(targetKey);
	}

	@Override
	public String[] nextKeys(int bufferSize) throws IOException {
		return fc.nextKeys(bufferSize);
	}

	@Override
	public void rewind() {
		fc.rewind();
	}

	@Override
	public boolean isEOF() {
		return fc.isEOF();
	}

	@Override
	public boolean isMore() {
		return fc.isMore();
	}

	@Override
	public void close() throws IOException {
		fc.close();
	}

	@Override
	public long size() throws IOException {
		return fc.size();
	}
	
	private String[] extractWords(String target,
			BreakIterator wordIterator) {

		Configuration config = Configuration.getInstance();
		int chunkSize = Integer.valueOf(config.getProperty(Configuration.SYS_CHUNK_SIZE_IN_BYTES));
		
		int initSize = (chunkSize > Integer.MAX_VALUE) ? Integer.MAX_VALUE
				: chunkSize;
		ArrayList<String> tmp = new ArrayList<String>(initSize);
		wordIterator.setText(new StringCharacterIterator(target));
		int start = wordIterator.first();
		int end = wordIterator.next();

		while (end != BreakIterator.DONE) {
			String word = target.substring(start, end);
			if (Character.isLetterOrDigit(word.charAt(0))) {
				tmp.add(word);
			}
			start = end;
			end = wordIterator.next();
		}
		return tmp.toArray(new String[tmp.size()]);
	}

	@Override
	public double total() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double remaining() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double sofar() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int progress() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ProcessState processStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FileChannel getFileChannel() {
		return fc.getFileChannel();
	}
	
	@Override
	public boolean isSorted() {
		return isSorted;
	}

	@Override
	public String[] getSorted() {
		return sorted;
	}

	@Override
	public void setSorted(String[] sorted){
		this.sorted = sorted;
		isSorted = true;
	}
	
	@Override
	public abstract Comparator<String> getComparator();

}
