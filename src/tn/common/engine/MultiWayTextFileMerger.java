package tn.common.engine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import tn.common.Configuration;
import tn.common.Plugin;
import tn.common.Plugin.PLUGIN_SUPPORT;
import tn.common.data.MergeComparator;
import tn.common.fs.TextFileChannelImpl;
import tn.common.fs.TextFileDataSource;

/**
 * A multi-way merge implementation.
 * 
 * @author Senthu Sivasambu, Sept 4, 2013
 * 
 */
public class MultiWayTextFileMerger implements
		MultiwayMerger<TextFileDataSource> {

	private final File sortspace;

	private final File out;

	private final File logspace;

	private MultiWayTextFileMerger(File sortspace, File out, File logspace) {
		/* no public instantiation */
		this.sortspace = sortspace;
		this.out = out;
		this.logspace = logspace;
	}

	public MultiwayMerger<TextFileDataSource> createInstance(File sortspace,
			File out, File logspace) {
		return new MultiWayTextFileMerger(sortspace, out, logspace);
	}

	@Override
	public void merge(Set<TextFileDataSource> mergeSet) {

		// TODO -- listing wrong number of chunks - such as having a hidden file
		// under sort space will
		// break this logic. improve it.
		FileFilter sortedChunkFilter = new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return pathname.getName().matches("chunk[0-9]*.sorted")
						&& !pathname.isHidden();
			}
		};

		@SuppressWarnings("unchecked")
		final Comparator<String> utfComp = (Comparator<String>) Plugin
				.getPlugin(PLUGIN_SUPPORT.MERGE_COMPARATOR);

		final Logger MERGE_LOGGER = Logger.getLogger("MERGE_LOGGER");
		MERGE_LOGGER.setLevel(Level.ALL);

		try {

			FileHandler fileHandler = new FileHandler(new File(logspace,
					"merge_log.txt").getPath());

			SimpleFormatter formatter = new SimpleFormatter();
			fileHandler.setFormatter(formatter);
			MERGE_LOGGER.addHandler(fileHandler);

		} catch (SecurityException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// as to reduce the number of IO to disks; the strategy adopted here is
		// to
		// allocate equal chunks of available memory to that many number of file
		// chunks
		// this computing unit has been assigned to handle + 2.
		// 1 is for out put buffer from where the final values can be written to
		// final file.
		// 1 is for intermediate sort buffer. that output space is to be shared
		// by values from
		// all the file channels.
		int mWay = sortspace.list().length; // M - way merge -- can give the
											// control to calling routine
		int chanelBufferSize = Math.round((getAvailableMemory() / (mWay + 2/*
																	 * 2 equal
																	 * output
																	 * buffer
																	 */))); // TODO
																			// can
																			// encapsulate
																			// this
																			// in
																			// a
																			// strategy
		int interSortSpace = Math.round(chanelBufferSize / mWay);// tuning these
																	// sizes,
																	// for
																	// example a
																	// higher
																	// value for
																	// intermediate
																	// sort
																	// space
		// could achieve higher throughput

		// the idea behind extending a two way merge into a polyphase merge
		// (Knuth) is to
		// maintain a data structure that enforces relative ordering among the
		// items retrieved from individually sorted file.
		Comparator<TextFileChannelImpl> utfsfcComp = new Comparator<TextFileChannelImpl>() {

			@Override
			public int compare(TextFileChannelImpl sfc1,
					TextFileChannelImpl sfc2) {
				try {
					String peek1 = sfc1.peek();
					String peek2 = sfc2.peek();
					return utfComp.compare(peek1, peek2);
				} catch (IOException e) {
					MERGE_LOGGER.log(Level.SEVERE, e.getMessage());
					e.printStackTrace();
				}
				return 0;
			}
		};
		PriorityQueue<TextFileChannelImpl> sortBuffer = new PriorityQueue<TextFileChannelImpl>(
				mWay, utfsfcComp);

		// maintain a bit vector to see if the values are changing
		BitSet noChangeVector = new BitSet(mWay); // initially false

		Configuration config = Configuration.getInstance();
		Locale locale = new Locale((String) config.get(Configuration.IN_LOCALE));
		String charSet = (String) config.get(Configuration.IN_CHAR_SET);
		MergeComparator mcomp = MergeComparator.getInstance();
		
		
		for (File schunk : sortspace.listFiles(sortedChunkFilter)) {
			TextFileChannelImpl sfc = TextFileChannelImpl.createInstance(
					schunk, mcomp, chanelBufferSize, locale,
					charSet, logspace);
			MERGE_LOGGER.log(Level.INFO, "Loading into sort buffer");
			try {
				sfc.load();
				sortBuffer.add(sfc);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				// do required logs - implement fail safe - loop back mechanism
			}
		}

		// prepare final output file channel
		BufferedWriter bwriter = null;
		TextFileChannelImpl fc = null;
		try {

			// output buffer space
			bwriter = new BufferedWriter(new FileWriter(out), chanelBufferSize);

			while (!sortBuffer.isEmpty()) {

				TextFileChannelImpl pivot = sortBuffer.remove();
				String peek = pivot.peek();

				if (peek != null) {
					MERGE_LOGGER.log(Level.INFO,
							"Processing " + pivot.toString());
					// handle duplicate values in this channel
					String[] keysLessThanOrEqual = pivot.nextKeys(peek,
							chanelBufferSize);
					if (keysLessThanOrEqual != null)
						writeToOutbuffer(bwriter, keysLessThanOrEqual);
					while (pivot.isMore()) {
						MERGE_LOGGER.log(Level.INFO,
								"Handling duplicate values for this file channel : "
										+ pivot.toString());
						keysLessThanOrEqual = pivot.nextKeys(peek,
								chanelBufferSize);
						if (keysLessThanOrEqual == null && pivot.isEOF())
							break;
						writeToOutbuffer(bwriter, keysLessThanOrEqual);
					}

				}

				if (!pivot.isEOF()) {
					// re-order
					sortBuffer.add(pivot);
				} else {
					// this file channel is already no longer in the sort space
					MERGE_LOGGER.log(Level.INFO, "EOF reached for chunk : "
							+ pivot.toString());
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
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

	private void writeToOutbuffer(BufferedWriter bwriter, String[] toWrite)
			throws IOException {
		if (toWrite != null)
			for (int i = 0; i < toWrite.length; i++) {
				String valToWrite = toWrite[i];
				bwriter.write(valToWrite);
				// TODO tightly coupling formatting rules in the code - enhance
				// it
				bwriter.newLine();
			}
	}
	
	private long getAvailableMemory(){
		return Runtime.getRuntime().freeMemory();
	}

}
