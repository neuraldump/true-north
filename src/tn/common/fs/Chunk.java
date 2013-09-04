package tn.common.fs;

import java.io.File;

import tn.common.data.Processable;

/**
 * Chunk is a file chunk (like shards in databases). A chunk of file is taken to
 * be of text type (though we may need to modify to support other formats).
 * 
 * @author Senthu Sivasambu, September 4, 2013
 * 
 */

public class Chunk extends TextFileDataSource implements Processable {

	public Chunk(File source, String id, String name, String charSet,
			String encoding) {
		super(source, id, name, charSet, encoding);
		// TODO Auto-generated constructor stub
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

}
