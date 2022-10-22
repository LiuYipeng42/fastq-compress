package pojo;

import java.io.Serializable;
import java.util.List;

public class SFNode implements Serializable {

	private static final long serialVersionUID = -299482035708790407L;

	private List<Byte> bytes;
	private SFNode left, right;

	public SFNode() {
	}

	public SFNode(List<Byte> bytes) {
		this.bytes = bytes;
	}

	public boolean isLeaf() {
		return left == null && right == null;
	}

	public List<Byte> getBytes() {
		return bytes;
	}

	public void setBytes(List<Byte> bytes) {
		this.bytes = bytes;
	}

	public void setLeft(SFNode left) {
		this.left = left;
	}

	public void setRight(SFNode right) {
		this.right = right;
	}

	public SFNode getLeft() {
		return left;
	}

	public SFNode getRight() {
		return right;
	}

}
