package tn.common.data;

public interface EnumerableTextSource extends DataSource {

	public String[] asArray();

	public String getCharSet();

	public String getEncoding();

	public String delimiter();
	
	public boolean isSorted();
	
	public String[] getSorted();
	
	public void setSorted(String[] sorted);
}
