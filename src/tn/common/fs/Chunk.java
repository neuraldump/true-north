package tn.common.fs;

import java.io.File;
import java.util.Comparator;

import tn.common.Configuration;
import tn.common.Plugin;
import tn.common.Plugin.PLUGIN_SUPPORT;
import tn.common.data.Processable;
import tn.common.data.Sortable;

/**
 * Chunk is a file chunk (like shards in databases). A chunk of file is taken to
 * be of text type (though we may need to modify to support other formats).
 * 
 * @author Senthu Sivasambu, September 4, 2013
 * 
 */

public class Chunk extends TextFileDataSource implements Processable {

	private Chunk(File source, String id, String name, String charSet,
			String encoding,String locale,File logSpace) {
		super(source, id, name, charSet, encoding, locale, logSpace);
	}

	
	//at this stage we assume all chunks are r+w
	/**
	 * 
	 * @param source
	 * @param logSpace - assigned by orchestrator -- it can be distributed FS / locations
	 * @param id - assigned by orchestrator
	 * @return
	 */
	public static Chunk createReadWriteChunk(File source,File logSpace,String id){
		Configuration config = Configuration.getInstance();
		String charSet = config.getProperty(Configuration.IN_CHAR_SET);
		String encoding = config.getProperty(Configuration.IN_ENCODING);
		String locale = config.getProperty(Configuration.IN_LOCALE);
		return new Chunk(source, id, source.getName(), charSet, encoding, locale, logSpace);
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

	@SuppressWarnings("unchecked")
	@Override
	public Comparator<String> getComparator() {
		return (Comparator<String>) Plugin.getPlugin(PLUGIN_SUPPORT.ALGO_COMPARATOR);
	}

}
