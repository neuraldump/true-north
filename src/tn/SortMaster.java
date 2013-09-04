package tn;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.text.BreakIterator;
import java.text.Collator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Vector;

import tn.common.Plugin;
import tn.common.Plugin.PLUGIN_SUPPORT;
import tn.common.data.Sortable;
import tn.common.fs.TextFileChannelImpl;

/**
 * 
 * @author Senthu Sivasambu
 * 
 */

public class SortMaster {

	private static final String UTF_8 = "UTF-8";
	private static final String INPUT_FILE = "input.txt";
	private static final String OUTPUT_FILE = "output.txt";
	private static final String WORK_SPACE = "D://development//workspace//workbench//src//tmp";
	static int CHUNK_SIZE_IN_BYTES = 64000;

	public static void main(String[] args) {

		File workspace = new File(WORK_SPACE);
		File input = new File(WORK_SPACE + File.separator + INPUT_FILE);
		File output = new File(WORK_SPACE + File.separator + OUTPUT_FILE);

		// create log directory
		File logspace = new File(WORK_SPACE + File.separator + "logs");
		logspace.mkdir();

		if (!workspace.isDirectory()) {
			throw new IllegalArgumentException(workspace.getPath()
					+ " must be directory");
		}

		checkPermission(workspace);
		checkPermission(input);

		// TODO get the property file location as single argument and the rest
		// from prop
		// do the permission checks and other as same above
		try {
			Plugin.initialize(new File(
					"D://development//workspace//workbench//src//sorter//src//com//config//sorter.properties"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// create required directory structure
		File chunkspace = new File(workspace, "chunkspace");
		chunkspace.mkdir();

		File sortspace = new File(workspace, "sortspace");
		sortspace.mkdir();

		// chunk up files
		chunkUpFile(input, chunkspace);

		sortChunks(chunkspace, sortspace);

		long availMem = Runtime.getRuntime().freeMemory();
		Locale locale = new Locale("en", "US");
		mergeChunks(sortspace, output, logspace, 128000/*
														 * a percentage of
														 * availMem - prop file
														 * can have a tuning
														 * value
														 */, locale, UTF_8);
	}

	public static Comparator<String> getComparator(final Locale locale) {
		Comparator<String> utfComp = new Comparator<String>() {

			private final Collator col = Collator.getInstance(locale);

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
		};
		return utfComp;
	}

	private static void sortChunks(File chunkspace, File sortspace) {
		// in a distributed system the federator will distribute a chunk
		// and we will distribute jobs to separate individual machines.
		FileFilter chunkFileFiler = new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return pathname.getName().startsWith("chunk");
			}
		};

		// go through the chunks - sort them using configured
		// algorithm
		for (File chunk : chunkspace.listFiles(chunkFileFiler)) {
			FileChannel fc = null;
			BufferedWriter bwriter = null;
			try {

				fc = new RandomAccessFile(chunk.getPath(), "rw").getChannel();
				MappedByteBuffer mbf = fc.map(MapMode.READ_WRITE, 0, fc.size());
				mbf.load();

				Locale locale = new Locale("en", "US"); // TODO from user --
														// depending on data set
				BreakIterator wordIterator = BreakIterator
						.getWordInstance(locale);

				// CharsetEncoder utf16LE =
				// Charset.forName("UTF-16LE").newEncoder();
				CharsetDecoder utf8 = Charset.forName(UTF_8).newDecoder();
				CharBuffer decoded = utf8.decode(mbf);

				// StringBuffer sb = new StringBuffer(decoded);
				// opting for single thread faster buffer
				StringBuilder sb = new StringBuilder(decoded);

				// because StringBuilder copies the supplies sequence - purge it
				// decoded.clear(); //this does not actually delete the data it
				// seems :(
				decoded = null;
				mbf = null;

				String[] cin = extractWords(sb.toString(), wordIterator); // consider
																			// a
																			// scenario
																			// where
																			// this
																			// cannot
																			// be
																			// held
																			// in
																			// memory
				sb.delete(0, (sb.length() - 1));
				sb = null;

				// BentleySedgewickSort algo =
				// BentleySedgewickSort.createInstance();
				Sortable<String> algo = (Sortable<String>) Plugin
						.getPlugin(PLUGIN_SUPPORT.ALGORITHM);
				algo.sort(cin, getComparator(locale));

				File sorted = new File(sortspace, chunk.getName() + ".sorted");
				bwriter = new BufferedWriter(new FileWriter(sorted));
				for (String svalue : cin) {
					bwriter.write(svalue.toCharArray());
					bwriter.newLine();
				}

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {

				try {
					if (fc != null)
						fc.close();
					if (bwriter != null)
						bwriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void chunkUpFile(File input, File workspace) {
		FileChannel fc = null;
		FileChannel cfc = null;

		try {

			fc = new RandomAccessFile(input.getPath(), "r").getChannel();
			for (long fp = 0, cid = 0; fp < fc.size(); fp += (CHUNK_SIZE_IN_BYTES), cid++) {
				long blockSize = CHUNK_SIZE_IN_BYTES;
				if ((fp + CHUNK_SIZE_IN_BYTES) >= fc.size()) {
					blockSize = fc.size() - fp;
				}

				MappedByteBuffer mbf = fc.map(MapMode.READ_ONLY, fp, blockSize);
				mbf = mbf.load();

				/*
				 * do a check to make sure the words are not broken in the
				 * middle - valid code points are those represents empty space
				 * and new line character - if in the middle down size to
				 * previous code point that makes up that
				 */
				/* 100 letter word (very large) * 32 (0r 16) = 3200 */
				byte[] buffer = new byte[3200];
				for (int i = 0, j = buffer.length - 1; i < buffer.length
						&& j > 0; j--, i++) {
					buffer[i] = mbf.get((mbf.capacity() - j));
				}
				CharsetDecoder utf8 = Charset.forName(UTF_8).newDecoder();
				CharsetEncoder utf8enco = Charset.forName(UTF_8).newEncoder();
				CharBuffer decoded = utf8.decode(ByteBuffer.wrap(buffer));
				char[] charArray = decoded.array();
				// is last character a space character? then break right there..
				int codePointAt = Character.codePointAt(charArray,
						(charArray.length - 1));
				if (!Character.isSpaceChar(codePointAt)) {
					// if not pedal back and find the space
					int j = charArray.length - 1;
					for (; j >= 0; j--) {
						codePointAt = Character.codePointAt(charArray, j);
						if (Character.isSpaceChar(codePointAt)) {
							j++; // move back passing the last checked space
									// char
							String substring = String.valueOf(charArray)
									.substring(j);
							ByteBuffer encoded = utf8enco.encode(CharBuffer
									.wrap(substring.toCharArray()));
							int yank = encoded.capacity();
							if (fp < yank) {
								mbf = fc.map(MapMode.READ_ONLY, fp,
										(blockSize - yank));
							} else {
								mbf = fc.map(MapMode.READ_ONLY, fp, blockSize);
							}
							fp -= yank;
							mbf = mbf.load();
							break;
						}
					}
				}

				File chunk = new File(workspace, "chunk" + cid);
				cfc = new RandomAccessFile(chunk, "rws").getChannel();
				cfc.write(mbf);
				cfc.close();
			}
			fc.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (cfc != null)
					cfc.close();
				if (fc != null)
					fc.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void checkPermission(File file) {
		if (!file.canRead() || !file.canWrite()) {
			throw new IllegalStateException(file.getPath()
					+ " must have r+w permission..");
		}
	}

	public static String[] extractWords(String target,
			BreakIterator wordIterator) {

		int initSize = (CHUNK_SIZE_IN_BYTES > Integer.MAX_VALUE) ? Integer.MAX_VALUE
				: CHUNK_SIZE_IN_BYTES;
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

	// (trial - a simple but expensive) sort keys along with associated file
	// channels in intermediate (in memory) sort space
	public static void insertionSort(Vector<String> keys,
			Vector<TextFileChannelImpl> sfc, Comparator<String> comp) {
		for (int i = 0; i < keys.size(); i++) {
			String keyAtI = keys.get(i);
			TextFileChannelImpl fcAtI = sfc.get(i);
			int j = i - 1;
			for (; j >= 0 && comp.compare(keyAtI, keys.get(j)) < 0; j--) {
				keys.set((j + 1), keys.get(j));
				sfc.set((j + 1), sfc.get(j));
			}
			keys.set((j + 1), keyAtI);
			sfc.set((j + 1), fcAtI);
		}
	}

}
