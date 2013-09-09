package tn.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class Plugin {

	public static enum PLUGIN_SUPPORT {

		ALGORITHM("sort.algorithm"), ALGO_COMPARATOR("sort.algorithm.comparator"),
		MERGE_COMPARATOR("merge.algorithm.comparator");

		private final String key;

		PLUGIN_SUPPORT(String key) {
			this.key = key;
		}

		public String getKey() {
			return key;
		}
	};

	private static Properties config = null;

	public static void initialize(File cfile) throws FileNotFoundException,
			IOException {

		if (config != null)
			return;

		if (cfile == null || "".equals(cfile)) {
			throw new IllegalArgumentException("@param cfile cannot be null");
		}

		config = new Properties();
		config.load(new FileInputStream(cfile));
	}

	public static Object getPlugin(PLUGIN_SUPPORT ps){
		String className = config.getProperty(ps.getKey());
		try {
			ClassLoader.getSystemClassLoader().loadClass(className);
			Class c = Class.forName(className);
			return c.newInstance();
		} catch (Throwable e){
			e.printStackTrace();
			throw new RuntimeException("could not initialise plugin : "+ps.key);
		}
	}

}
