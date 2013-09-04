package tn;

import tn.algo.VariableDepthMSDRadixSort;

public class TernaryPartitioner {

	public static void main(String[] args) {

		args = new String[] { "2" };

		args = new String[] { "1", "2", "3" };
		args = new String[] { "2", "1", "3", "4" };
		args = new String[] { "2", "1", "3", "5", "4" };
		args = new String[] { "2", "4", "7", "1", "4", "4", "7", "3", "7", "4" };
		args = new String[] { "3", "2", "5" };
		args = new String[] { "2", "1", "3", "5", "4", "1", "7" };

		args = new String[] { "2", "4", "7", "1", "4", "4", "7", "3", "7", "4" };

		args = new String[] { "2", "41", "3333333", "3", "5", "4", "1", "7",
				"99", "8765" };

		args = new String[] { "2", "1" };

		args = new String[] { "2", "1", "3", "4" };

		args = new String[] { "1", "2", "3" };
		args = new String[] { "3", "2", "1" };
		args = new String[] { "2", "3", "1" };

		args = new String[] { "1", "2", "3", "4", "5", "6" };
		args = new String[] { "6", "5", "4", "3", "2", "1" };

		args = new String[] { "2", "1" };
		args = new String[] { "2", "1", "3" };
		args = new String[] { "1", "2", "3" };
		args = new String[] { "3", "2", "1" };

		args = new String[] { "5", "4", "3", "2", "1" };
		args = new String[] { "1", "2", "3", "4", "5" };

		args = new String[] { "7", "6", "27", "1", "4", "4", "7", "3", "777",
				"776", "2" };

		args = new String[] { "2", "2", "2" };

		args = new String[] { "2", "441", "44442", "443", "2", "5442441",
				"5554432", "443", "2222222222222", "441", "442", "443", "222",
				"44312", "442", "443", "2", "441", "442", "443" };

		args = new String[] { "441", "442", "443", "444", "444", "4445", "441" };

		args = new String[] { "441", "442", "443", "4442", "4444444444",
				"4445", "44441", "7", "111111111111111111111" };

		args = new String[] { "AA", "AA", "AAA" };

		args = new String[] { "A", "A", "A" };
		VariableDepthMSDRadixSort.createInstance().sort(args, null);
		for (String s : args)
			System.out.print(s + ",");
	}

}
