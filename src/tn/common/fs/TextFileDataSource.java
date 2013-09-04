package tn.common.fs;

import java.io.File;
import java.io.IOException;

import tn.common.data.EnumerableTextSource;
import tn.common.engine.Distributable;

/**
 * represents a file base data source. However a file can be text file or other
 * binary data. Therefore that type information is deferred until the time of
 * creation of such data source
 * 
 * @author Senthu Sivasambu
 * 
 */
public class TextFileDataSource implements TextFileChannel,
		EnumerableTextSource, Distributable<File, File> {

	private final TextFileChannelImpl fc = null;

	public TextFileDataSource(File source, String id, String name,
			String charSet, String encoding) {

	}

	@Override
	public String getID() {
		return null;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public String getCharSet() {
		return null;
	}

	@Override
	public String getEncoding() {
		return null;
	}

	@Override
	public String[] asArray() {
		return null;
	}

	@Override
	public tn.common.engine.Distributable.DistrState distrStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void distribute(File from, File to) {

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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String peek() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] keysLessThanOrEqual(String targetKey) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] keysGreaterThan(String targetKey) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] nextKeys(int bufferSize) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void rewind() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isEOF() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isMore() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public long size() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

}
