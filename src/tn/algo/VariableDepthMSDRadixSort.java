package tn.algo;

import tn.common.data.EnumerableTextSource;

/**
 * Inspiration to this implementation is from the proposed solution on a paper
 * by Jon. L. Bentley and Robert Sedgewick, 1997, Fast Algorithms for Sorting
 * and Searching Strings. <br>
 * 
 * However it is observed that implementation provided therein may not satisfy
 * correctness ' requirements. For example, the provided implementation in that
 * paper checks for nullability at a given depth @ an index where equal elements
 * begins. This is, while correct, not a sufficient condition to be used to
 * invoke a recursive calls to process equal elements.<br>
 * <br>
 * 
 * for example, given implementation is not robust in C - i2c() implementation
 * assumes that out of bound calls to given macro will always return 0 (null) -
 * this is not entirely true. Further implementation will fail to correctly make
 * recursive calls for a equal input class in following order
 * {AA,AA,AAAB,AA,AAAC} - because r can return 0 where we may still have {AAAB,
 * AAAC} not processed <br>
 * <br>
 * 
 * This implementation keeps tracks of maximum length string in equal category
 * (at a given depth) and use that to invoke such processing. <br>
 * <br>
 * 
 * it is shown, otherwise
 * 
 * @author Senthu Sivasambu, 3 Sept 2013
 * 
 */

public class VariableDepthMSDRadixSort implements
		Strategy<EnumerableTextSource> {

	public VariableDepthMSDRadixSort()
	/* no public instantiation */
	{
	}

	public static VariableDepthMSDRadixSort createInstance() {
		return new VariableDepthMSDRadixSort();
	}

	@Override
	public void run(EnumerableTextSource input) {
		String[] asArray = input.asArray();
		BentleyAndSedgwickSort(asArray, 0, (asArray.length - 1), 0);
		input.setSorted(asArray);
	}

	// one of the improvements to Bentley and Sedgewick proposed algorithm is
	// that class level
	// max depth is maintained (per class) and dynamically calculated. In
	// programming languages
	// like c, a special programming paradigms may need to be maintained such as
	// length could
	// be encoded in first 2 bytes of input set of strings this algorithm
	// receives - possible
	// at the time input set is generated.
	void BentleyAndSedgwickSort(String input[], int startIdx, int endIdx,
			int depth) {

		int length = (endIdx - startIdx) + 1;

		// base condition (1)
		if (length <= 1)
			return;

		// base condition (2) for ternary partitioning - it is cheaper this way
		// to maintain invariants for markers
		if (length == 2) {
			if (i2c(input, startIdx, depth) > i2c(input, endIdx, depth)) {
				swap(input, startIdx, endIdx);
			}
			return;
		}

		// to dynamically track maximum depth
		int dMax = depth;

		// declare and initialize markers
		int a, b, c, d, tmp;
		a = tmp = startIdx;
		b = startIdx + 1; // because startIdx will house pivote value
		c = endIdx;
		d = endIdx + 1; // one element beyond array boundary to indicate an
						// invalid position

		// compute a random pivot
		double index = length * /* 0.5 */Math.random();
		index = Math.floor(index);
		int pivot = (int) Math.round(index);
		int pivotIdx = startIdx + pivot; // can never overrun array boundaries
											// as Math.random is always < 1
		int pVal = i2c(input, pivotIdx, depth);

		// move the pivot to index 0 of sub-array
		swap(input, startIdx, pivotIdx);

		// Wegner's ternary partition scheme with improved maintenance of
		// markers
		boolean loop = b <= c;
		while (loop) {

			// move marker b from left side until it encounters c or an element
			// larger than pVal - whichever the first
			while (b <= c && (tmp = i2c(input, b, depth) - pVal) <= 0) {
				if (tmp == 0) {
					swap(input, ++a, b);
					// because depth again an array index in second dimension,
					// when depth==0 and no more we do not want extra recursive
					// call
					int l = input[a].length() - 1;
					dMax = l > dMax ? l : dMax;
				}
				if (b == c)
					break;
				if (b < c)
					b++;
			}

			if (b == c)/* no more unexplored region in this (sub) array */{
				break;
			}

			// move marker c from left size until it encounters b or an element
			// smaller than pVal - whichever the first
			while (b <= c && (tmp = i2c(input, c, depth) - pVal) >= 0) {
				if (tmp == 0) {
					swap(input, c, --d);
					int l = input[d].length() - 1;
					dMax = l > dMax ? l : dMax;
				}
				if (b == c)
					break;
				if (b < c)
					c--;
			}

			// at this stage, either b==c or each of them have identified a less
			// than and larger than elements (than pVal)
			if (b < c) {
				swap(input, b, c);
			}

			loop = b <= c;
		}

		/*
		 * at this stage, we have partitioned the input (sub) array, we need to
		 * arrange it so that the pivot+other equal elements are moved to the
		 * middle of the array, < elements are on the left of equal region and >
		 * elements are to the left of equal region
		 * 
		 * equal region will have minimum of 1 element (the pivot) in the equal
		 * region. It may or may not have < or > regions based on input (sub)
		 * array
		 */

		// at this point marker a is pointing to the end of equal elements in
		// the left partition.
		// a can be = b; but b also can be some @ greater index

		// at this stage b==c is expected. it is still not known if this last
		// element @ b < or > than pivot
		int thisVal = i2c(input, b, depth);
		if (thisVal <= pVal) {
			if (thisVal == pVal) {
				if (a < endIdx)
					a++;
			}
			// move the b and c markers to the right
			b++;
			c++;
		}

		// we need to deduce what partition sizes we have got in hand for this
		// (sub) array
		int sizeLT, sizeGT, sizeEQ;

		sizeEQ = ((a - startIdx) + 1)/* from left */
				+ (d > endIdx ? 0 : ((endIdx - d) + 1))/* from right */;
		sizeLT = (b - 1) - a; // b+1 > a is always true
		sizeGT = d - b;

		// move (if any) equal elements to the middle from left
		int left = startIdx;
		int right = b - 1; // if we swap with b, then that would place a greater
							// value to the left
		while (left <= a && right > a && right <= endIdx) {
			swap(input, left, right);
			left++;
			right--;
		}

		// move (if any) equal elements to the middle from right
		left = b; // greater > pivot starts at b or c for that matter
		right = endIdx;
		if (d != length) {
			while (right >= d && left < d) {
				swap(input, left, right);
				left++;
				right--;
			}
		}

		// at this stage it is expected that elements are ordered like a Dutch
		// flag
		left = right = startIdx;
		right = sizeLT > 0 ? left + (sizeLT - 1) : left/* can never be < 0 */;
		BentleyAndSedgwickSort(input, left, right, depth);

		left = sizeLT == 0 ? startIdx : right + 1;
		right = sizeEQ > 0 ? left + (sizeEQ - 1) : left;
		if (dMax > depth)
			BentleyAndSedgwickSort(input, left, right, (depth + 1));

		left = right + 1;
		right = endIdx;
		BentleyAndSedgwickSort(input, left, right, depth);

	}

	private void swap(String[] input, int i, int j) {
		String tmp = input[i];
		input[i] = input[j];
		input[j] = tmp;
	}

	private int i2c(String[] input, int i, int depth) {
		String t = input[i];
		if (depth < t.length()) {
			return t.charAt(depth);// not Unicode friendly
		}
		return 0; // null
	}

}
