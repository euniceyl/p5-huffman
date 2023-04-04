import java.util.PriorityQueue;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Ow	en Astrachan
 *
 * Revise
 */

public class HuffProcessor {

	private class HuffNode implements Comparable<HuffNode> {
		HuffNode left;
		HuffNode right;
		int value;
		int weight;

		public HuffNode(int val, int count) {
			value = val;
			weight = count;
		}
		public HuffNode(int val, int count, HuffNode ltree, HuffNode rtree) {
			value = val;
			weight = count;
			left = ltree;
			right = rtree;
		}

		public int compareTo(HuffNode o) {
			return weight - o.weight;
		}
	}

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private boolean myDebugging = false;
	
	public HuffProcessor() {
		this(false);
	}
	
	public HuffProcessor(boolean debug) {
		myDebugging = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){


		// remove all this code when implementing compress
		int[] counts = getCounts(in);
		HuffNode root = makeTree(counts);
		in.reset();
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeTree(root, out);
		String[] encodings = new String[ALPH_SIZE + 1];
		makeEncodings(root, "", encodings);

		in.reset();
		while (true) {
			int bits = in.readBits(BITS_PER_WORD);
			if (bits == -1) {
				String code = encodings[PSEUDO_EOF];
				out.writeBits(code.length(), Integer.parseInt(code, 2));
				break;
			}
			String code = encodings[bits];
			out.writeBits(code.length(), Integer.parseInt(code, 2));
		}
		out.close();

		// while (true){
		// 	int val = in.readBits(BITS_PER_WORD);
		// 	if (val == -1) break;
		// 	out.writeBits(BITS_PER_WORD, val);
		// }
		// out.close();
	}

	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out){

		// remove all code when implementing decompress
		int bits = in.readBits(BITS_PER_INT);
		if (bits == -1) throw new HuffException("illegal header starts with " + bits);
		if (bits != HUFF_TREE) throw new HuffException("illegal header starts with " + bits);
		HuffNode root = readTree(in);
		HuffNode curr = root;

		while (true) {
			int oneBit = in.readBits(1);
			if (oneBit == -1) {
				throw new HuffException("failed to read bit");
			}
			else {
				if (oneBit == 0) {
					curr = curr.left;
				}
				else {
					curr = curr.right;
				}
				if (curr.right == null && curr.left == null) {
					if (curr.value == PSEUDO_EOF) {
						break;
					}
					else {
						out.writeBits(BITS_PER_WORD, curr.value);
						curr = root;
					}
				}
			}
		}
		out.close();

		// while (true){
		// 	int val = in.readBits(BITS_PER_WORD);
		// 	if (val == -1) break;
		// 	out.writeBits(BITS_PER_WORD, val);
		// }
		// out.close();
	}

	private int[] getCounts(BitInputStream in){
		int[] numArray = new int[ALPH_SIZE];
		int bits = in.readBits(BITS_PER_WORD);
		while(true){
			if(bits == -1) break;
			numArray[bits] +=1;
			bits = in.readBits(BITS_PER_WORD);
		} 
		return numArray; 
	}

	private HuffNode makeTree(int[] freqCounts){
		PriorityQueue<HuffNode> pq = new PriorityQueue<>();
		for(int i = 0; i <freqCounts.length;i++){
			if(freqCounts[i]>0){
				pq.add(new HuffNode(i, freqCounts[i], null, null));
			}
		}
		pq.add(new HuffNode(PSEUDO_EOF, 1, null, null));
		while(pq.size() > 1){
			HuffNode left = pq.remove();
			HuffNode right = pq.remove();
			HuffNode tHuffNode = new HuffNode(0, left.weight+right.weight, left, right);
			pq.add(tHuffNode);
		}
		HuffNode root = pq.remove();
		return root;
	}

	private HuffNode readTree(BitInputStream in) {
		int bit = in.readBits(1);
		if (bit == -1) {
			throw new HuffException("invalid magic number");
		}
		if (bit == 0) {
			HuffNode left = readTree(in);
			HuffNode right = readTree(in);
			return new HuffNode(0, 0, left, right);
		}
		else {
			int value = in.readBits(1 + BITS_PER_WORD);
			return new HuffNode(value, 0, null, null);
		}
	}

	private void writeTree(HuffNode root, BitOutputStream out) {
		if (root.left == null && root.right == null) {
			out.writeBits(1, 1);
			out.writeBits(BITS_PER_WORD + 1, root.value);
			return;
		}
		out.writeBits(1, 0);
		writeTree(root.left, out);
		writeTree(root.right, out);
	}

	private void makeEncodings(HuffNode tree, String path, String[] encodings) {
		if (tree == null) {
			return;
		}
		if (tree.left == null && tree.right == null) {
			encodings[tree.value] = path;
			return;
		}
		makeEncodings(tree.left, path + "0", encodings);
		makeEncodings(tree.right, path + "1", encodings);
	}
}