package tn.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class Configuration extends Properties{

	//perhaps this value must be supplied at the time of execution
	private static final String CONFIG_RELATIVE_PATH = "../config/input.properties";
	
	private static Configuration config = null;
	
	public static String IN_CHAR_SET="input.charset";
	public static String IN_ENCODING = "input.encoding";
	public static String IN_LOCALE = "input.locale";
	
	private Configuration() {
		/*strictly no public instantiation*/
	}
	
	public static Configuration getInstance(){
		if(config == null){
			config = new Configuration();
			try {
				config.load(new FileInputStream(new File(CONFIG_RELATIVE_PATH)));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return config;
	}
	
}