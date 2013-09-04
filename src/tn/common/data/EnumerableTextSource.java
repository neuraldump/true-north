package tn.common.data;

public interface EnumerableTextSource extends DataSource {

	public String[] asArray();

	public String getCharSet();

	public String getEncoding();

	public String delimiter();
}
