package pojo;

import java.io.Serializable;

public class HuffmanNode implements Comparable<HuffmanNode>, Serializable {

	private static final long serialVersionUID = -299482035708790407L;

	private char c;
	private int freq;
	private final HuffmanNode left, right;

	public HuffmanNode(char c, int freq, HuffmanNode left, HuffmanNode right) {
		this.c = c;
		this.freq = freq;
		this.left = left;
		this.right = right;
	}

	public boolean isLeaf() {
		return left == null && right == null;
	}

	@Override
	public int compareTo(HuffmanNode that) {
		return this.freq - that.freq;
	}

	public int getFreq() {
		return freq;
	}

	public char getChar() {
		return c;
	}

	public HuffmanNode getLeft() {
		return left;
	}

	public HuffmanNode getRight() {
		return right;
	}

}
