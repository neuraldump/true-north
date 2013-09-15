package tn.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class Configuration extends Properties{

	private static final long serialVersionUID = 1L;
	
	private static File cSpace = null;
	private static Configuration config = null;
	
	public static final String IN_CHAR_SET="input.charset";
	public static final String IN_ENCODING = "input.encoding";
	public static final String IN_LANGUAGE = "input.language";
	public static final String IN_LOCALE = "input.locale";
	
	public static final String OUT_CHAR_SET="output.charset";
	public static final String OUT_ENCODING = "output.encoding";
	public static final String OUT_LOCALE = "output.locale";
	
	public static final String SYS_FS_BLOCK_SIZE="system.fs.blocksize";
	public static final String SYS_CHUNK_SIZE_IN_BYTES = "sort.chunk.size";
	public static final String SYS_INPUT_FILE = "system.input.file";
	public static final String SYS_OUTPUT_FILE = "system.output.file";
	public static final String SYS_WORK_SPACE = "system.workspace";
	
	
	private Configuration() {
		/*strictly no public instantiation*/
	}
	
	public static void initialise(File cParent){
		
		if(cParent == null){
			throw new IllegalArgumentException("@param cSpace cannot be null");
		}
		
		if(!cParent.isDirectory()){
			throw new IllegalArgumentException("@param cFileSpace should be a directory");
		}
		
		cSpace = cParent; 
	}
	
	public static Configuration getInstance(){
		
		if(cSpace == null){
			throw new IllegalStateException("Call initialise() first");
		}
		
		if(config == null){
			config = new Configuration();
			try {
				for (File cFile : cSpace.listFiles()) {
					config.load(new FileInputStream(cFile));
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return config;
	}
	
}
