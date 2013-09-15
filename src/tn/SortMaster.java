package tn;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
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
import java.util.Random;
import java.util.UUID;
import java.util.Vector;

import tn.algo.Strategy;
import tn.algo.VariableDepthMSDRadixSort;
import tn.common.Configuration;
import tn.common.Plugin;
import tn.common.Plugin.PLUGIN_SUPPORT;
import tn.common.data.EnumerableTextSource;
import tn.common.data.Sortable;
import tn.common.engine.MultiWayTextFileMerger;
import tn.common.engine.MultiwayMerger;
import tn.common.fs.Chunk;
import tn.common.fs.TextFileChannelImpl;
import tn.common.fs.TextFileDataSource;

/**
 * Main: Sorting starts here
 * 
 * @author Senthu Sivasambu
 * 
 */

public class SortMaster {

	private static final String SORTSPACE = "sortspace";
	private static final String CHUNKSPACE = "chunkspace";


	public static void main(String[] args) {

		if(args == null || args.length < 1){
			throw new IllegalArgumentException("requires path to configuration directory");
		}
		
		String cString = args[0];
		File cFile = new File(cString);
		if(!cFile.canRead()){
			throw new IllegalStateException("Permission denied to access configuration file : "+cFile.getPath());
		}
		Configuration.initialise(cFile);
		Configuration config = Configuration.getInstance();
		
		String worspacePath = config.getProperty(Configuration.SYS_WORK_SPACE);
		String inputFileName = config.getProperty(Configuration.SYS_INPUT_FILE);
		String outputFileName = config.getProperty(Configuration.SYS_OUTPUT_FILE);
		File workspace = new File(worspacePath);
		if (!workspace.isDirectory()) {
			throw new IllegalArgumentException(workspace.getPath()
					+ " must be directory");
		}

		//expected file path for input and output files.
		File input = new File(worspacePath + File.separator + inputFileName);
		File output = new File(worspacePath + File.separator + outputFileName);
		
		checkRWPermission(workspace);
		checkRWPermission(input);

		// create required sub-directory structure
		File logspace = new File(worspacePath + File.separator + "logs");
		logspace.mkdir();
		
		File chunkspace = new File(workspace, CHUNKSPACE);
		chunkspace.mkdir();

		File sortspace = new File(workspace, SORTSPACE);
		sortspace.mkdir();

		chunkUpFile(input, chunkspace);

		sortChunks(chunkspace, sortspace,logspace);

		MultiwayMerger<TextFileDataSource> merger = MultiWayTextFileMerger.createInstance(sortspace, output, logspace);
		merger.merge(null/*TODO - an orchestraor will assign the chunks to be merged*/);
	}

	
	private static void sortChunks(File chunkSpace, File sortSpace,File logSpace) {
		
		FileFilter chunkFileFiler = new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return pathname.getName().startsWith("chunk");
			}
		};

		// go through the chunks - sort them using configured
		// algorithm
		for (File chunk : chunkSpace.listFiles(chunkFileFiler)) {
			sort(sortSpace, chunkSpace, chunk);
		}
	}


	private static void sort(File sortspace, File logSpace, File chunk/*to sort*/) {
		BufferedWriter bwriter = null;
		try {
			
			//this chunk is likely to have been distributed to a host by the federator - use
			//that info in deriving id
			InetAddress localHost = InetAddress.getLocalHost();
			String uuidString = localHost.getHostName()+"-"+localHost.getHostAddress()+"-"+chunk.getName();
			//UUID.fromString(uuidString).toString();
			Chunk toSort = Chunk.createReadWriteChunk(chunk, logSpace,uuidString);
			@SuppressWarnings("unchecked")
			Strategy<EnumerableTextSource> algo = (Strategy<EnumerableTextSource>) Plugin
					.getPlugin(PLUGIN_SUPPORT.ALGORITHM);
			algo.run(toSort);

			File sorted = new File(sortspace, chunk.getName() + ".sorted");
			bwriter = new BufferedWriter(new FileWriter(sorted));
			for (String svalue : toSort.getSorted()) {
				bwriter.write(svalue.toCharArray());
				bwriter.newLine();
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {

			try {
				if (bwriter != null)
					bwriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void chunkUpFile(File input, File workspace) {
		FileChannel fc = null;
		FileChannel cfc = null;

		Configuration config = Configuration.getInstance();
		long chunkSize = Integer.valueOf(config.getProperty(Configuration.SYS_CHUNK_SIZE_IN_BYTES));
		
		try {

			fc = new RandomAccessFile(input.getPath(), "r").getChannel();
			for (long fp = 0, cid = 0; fp < fc.size(); fp += (chunkSize), cid++) {
				long blockSize = chunkSize;
				if ((fp + chunkSize) >= fc.size()) {
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
				CharsetDecoder utf8deco = Charset.forName(config.getProperty(Configuration.IN_ENCODING)).newDecoder();
				CharsetEncoder utf8enco = Charset.forName(config.getProperty(Configuration.OUT_ENCODING)).newEncoder();
				CharBuffer decoded = utf8deco.decode(ByteBuffer.wrap(buffer));
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

	private static void checkRWPermission(File file) {
		if (!file.canRead() || !file.canWrite()) {
			throw new IllegalStateException(file.getPath()
					+ " must have r+w permission..");
		}
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
