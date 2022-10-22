package pojo;

import java.util.ArrayList;

public class CompressionData {
    private ArrayList<Byte> bytes;
	private int bitLen;

	public CompressionData (ArrayList<Byte> bytes, int bitLen){
		this.bytes = bytes;
		this.bitLen = bitLen;
	}

	public void setBitLen(int bitLen) {
		this.bitLen = bitLen;
	}

	public void setBytes(ArrayList<Byte> bytes) {
		this.bytes = bytes;
	}

	public int getBitLen() {
		return bitLen;
	}

	public ArrayList<Byte> getBytes() {
		return bytes;
	}
}
