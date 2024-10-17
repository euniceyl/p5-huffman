# Project 5: Huffman Coding/Compression

## Project Introduction

There are many techniques used to compress digital data (that is, to represent it using less memory). This project implements a compelte program to compress and uncompress data using Huffman coding. We input and output or I/O classes that read and write 1 to many bits at a time, i.e., a single zero or one to several zeros and ones.

## Part 0: Starter Code

### `BitInputStream` and `BitOutputStream` Classes

These two classes read/write bits in (un)compressed files, functioning like Scanner, except instead of reading in / writing out arbitrarily delimited “tokens”, they read/write a specified number of bits. Two consecutive calls to the `readBits` method will likely return different results since InputStream classes maintain an internal "cursor" or "pointer" to a spot in the stream from which to read -- and once read the bits won't be read again (unless the stream is reset).

1. *`int BitInputStream.readBits(int numBits)`*: This method reads from the source the specified number of bits and returns an integer. Since integers are 32-bit in Java, the parameter `numBits` must be between 1 and 32, inclusive. **It will return -1 if there are no more bits to read.**
2. `void BitInputStream.reset()`: This method repositions the “cursor” to the beginning of the input file.
3. `void BitOutputStream.writeBits(int numBits, int value)`: This method writes the least-significant `numBits` bits of the value to the output file.

## Part 1: Implementing `HuffProcessor.decompress`

To implement `decompress`, we follow 4 conceptual steps in decompressing a file that has been compressed using Huffman coding:
1. Read the 32-bit "magic" number as a check on whether the file is Huffman-coded
2. Read the tree used to decompress, this is the same tree that was used to compress, i.e., was written during compression
3. Read the bits from the compressed file and use them to traverse root-to-leaf paths, writing leaf values to the output file. Stop when finding `PSEUDO_EOF`
4. Close the output file.

<details> 
<summary> Expand for example decompress outline </summary>

<div align="center">
  <img width="837" height="254" src="p5-figures/decompress.png">
</div>

</details>

A `HuffException` is thrown if the file of compressed bits does not start with the 32-bit value `HUFF_TREE`. Our code throws a `HuffException` if reading bits ever fails, i.e., the `readBits` method returns -1. This could happen in the helper methods when reading the tree and when reading the compressed bits.

### Reading the Tree (private HuffNode readTree)

Reading the tree using a helper method is required since reading the tree, stored using a pre-order traversal, is much simpler with recursion. In our protocol, interior tree nodes are indicated by the single bit 0 and leaf nodes are indicated by the single bit 1. No values are written for internal nodes and a 9-bit value is written for a leaf node. 

This leads to the following pseudocode to read the tree.

<details>
<summary> Expand for outline of readTree </summary>

``` java
private HuffNode readTree(BitInputStream in) {
bit = in.readBits(1);
if (bit == -1) throw exception
if (bit == 0) {
    left = readTree(...)
    right = readTree(...)
    return new HuffNode(0,0,left,right);
}
else {
    value = read BITS_PER_WORD+1 bits from input
    return new HuffNode(value,0,null,null);
    }
}
``` 

</details>

For example, the tree below corresponds to the bit sequence `0001X1Y01Z1W01A1B`, with each letter representing a 9-bit sequence to be stored in a leaf as shown in the tree to the right. We read these 9-bit chunks with an appropriate call of `readBits`. Rather than using 9, we use `BITS_PER_WORD + 1`, the +1 is needed since one leaf stores `PSEUDO_EOF` so all leaf nodes store a 9-bit value.

<details> 
<summary> Expand for example tree </summary>

<div align="center">
  <img width="291" height="213" src="p5-figures/huffheadtreeNODES.png">
</div>

</details>


### Reading Compressed Bits (while (true))

Once we read the bit sequence representing the tree, we'll read the remaining bits from the `BitInputStream` representing the compressed file one bit at a time, traversing the tree from the root and going left or right depending on whether you read a zero or a one. This is represented in the pseudocode for `decompress` by the hidden while loop.

<details>
<summary> Expand for pseudocode example </summary>

``` java
  HuffNode current = root; 
  while (true) {
      int bits = input.readBits(1);
      if (bits == -1) {
          throw new HuffException("bad input, no PSEUDO_EOF");
      }
      else { 
          if (bits == 0) current = current.left;
          else current = current.right;

          if (current is a leaf node) {
              if (current.value == PSEUDO_EOF) 
                  break;   // out of loop
              else {
                  write BITS_PER_WORD bits to output for current.value;
                  current = root; // start back after leaf
              }
          }
      }
  }
  close output file
```

</details>


## Part 2: Implementing `HuffProcessor.compress`

We follow five conceptual steps to compress a file using Huffman coding:

1. Determine the frequency of every eight-bit character/chunk in the file being compressed (see line 78 below).
2. From the frequencies, create the Huffman trie/tree used to create encodings (see line 79 below).
3. From the trie/tree, create the encodings for each eight-bit character chunk (see lines 83-84 below).
4. Write the magic number and the tree to the beginning/header of the compressed file (see lines 81-82 below).
5. Read the file again and write the encoding for each eight-bit chunk, followed by the encoding for PSEUDO_EOF, then close the file being written (not shown).

<div align="center">
  <img width="600" height="180" src="p5-figures/newcompress.png">
</div>

### Determining Frequencies (private int[] getCounts)

We create an integer array that can store 256 values (use `ALPH_SIZE`). We read 8-bit characters/chunks, (using `BITS_PER_WORD` rather than 8), and use the read/8-bit value as an index into the array, incrementing the frequency. Conceptually this is a map from 8-bit chunk to frequency for that chunk, using an array and mapping the index to the number of times the index occurs, e.g., `counts['a']` is the number of times 'a' occurs in the input file being compressed.

### Making Huffman Trie/Tree (private HuffNode makeTree)

We used a greedy algorithm and a `PriorityQueue` of `HuffNode` objects to create the trie. Since `HuffNode` implements `Comparable` (using weight), our code removes the minimal-weight nodes when `pq.remove()` is called as shown in the pseudocode included in the expandable section below.

<details>
<summary> Expand for makeTree pseudocode </summary>

``` java
PriorityQueue<HuffNode> pq = new PriorityQueue<>();
for(every index such that freq[index] > 0) {
    pq.add(new HuffNode(index,freq[index],null,null));
}
pq.add(new HuffNode(PSEUDO_EOF,1,null,null)); // account for PSEUDO_EOF having a single occurrence

while (pq.size() > 1) {
   HuffNode left = pq.remove();
   HuffNode right = pq.remove();
   // create new HuffNode t with weight from
   // left.weight+right.weight and left, right subtrees
   pq.add(t);
}
HuffNode root = pq.remove();
return root;
```

</details>

To ensure that `PSEUDO_EOF` is represented in the tree, you only added nodes to the priority queue for indexes/8-bit values that occur, i.e., that have non-zero weights.


### Make Codings from Trie/Tree (private makeEncodings)

This recursive helper method populates an array of Strings such that `encodings[val]` is the encoding of the 8-bit chunk val. This has the array of encodings as one parameter, a node that's the root of a subtree as another parameter, and a string that's the path to that node as a string of zeros and ones. 
Example: helper method `makeEncodings`:

``` java
    String[] encodings = new String[ALPH_SIZE + 1];
    makeEncodings(root,"",encodings);
```

In this method, if the `HuffNode` parameter is a leaf (recall that a leaf node has no left or right child), an encoding for the value stored in the leaf is added to the array, e.g.,
``` java
   if (root is leaf) {
        encodings[root.value] = path;
        return;
   }
```
If the root is not a leaf, we make recursive calls adding "0" to the path when making a recursive call on the left subtree and adding "1" to the path when making a recursive call on the right subtree. Every node in a Huffman tree has two children. We only add a single "0" for left-call and a single "1" for right-call. Each recursive call has a String path that's one more character than the parameter passed, e.g., path + "0" and path + "1".

### Writing the Tree (private void writeTree)

If a node is an internal node, i.e., not a leaf, write a single bit of zero. Else, if the node is a leaf, write a single bit of one, followed by _nine bits_ of the value stored in the leaf. This is a pre-order traversal: we write one bit for the node, then make two recursive calls if the node is an internal node. We wrote 9 bits, or `BITS_PER_WORD + 1`, because there were 257 possible values including `PSEUDO_EOF`.

### Writing Compressed Bits

After writing the tree, we read the file being compressed one more time. The ***`BitInputStream` is reset, then read again to compress it***. The first reading was to determine frequencies of every 8-bit chunk. The encoding for each 8-bit chunk read is stored in the array created when encodings were made from the tree. These encodings are stored as strings of zeros and ones, e..g., "010101110101". To convert such a string to a bit-sequence we use `Integer.parseInt` specifying a radix, or base of two. For example, to write the encoding for the 8-bit chunk representing 'A', which has an ASCII value of 65, we used:
``` java
    String code = encoding['A']
    out.writeBits(code.length(), Integer.parseInt(code,2));
```
You'll use code like this for every 8-bit chunk read from the file being compressed. i.e.,
``` java
    String code = encoding[PSEUDO_EOF]
    out.writeBits(code.length(), Integer.parseInt(code,2));
```
We wrote these bits _after_ writing the bits for every 8-bit chunk. The encoding for `PSEUDO_EOF` is used when decompressing, ***so we wrote the encoding bits before the output file was closed.***

Coursework from Duke CS 201: Data Structures and Algorithms.
