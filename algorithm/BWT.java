package algorithm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// https://zh.wikipedia.org/wiki/Burrows-Wheeler%E5%8F%98%E6%8D%A2
public class BWT {

	public String enCode(String str) {

		int len = str.length();
		char[] charArray = str.toCharArray();

		char[] last = new char[len];
		List<Integer> index = new ArrayList<>();

		for (int i = 0; i < len - 1; i++)
			last[i] = charArray[len - i - 2];
		last[len - 1] = charArray[len - 1];

		for (int i = 0; i < len; i++)
			index.add(i);

		Collections.sort(
				index,
				(i1, i2) -> {
					int index1 = len - i1 - 1;
					int index2 = len - i2 - 1;
					while (charArray[index1] == charArray[index2]) {
						index1++;
						index2++;
						if (index1 == len)
							index1 = 0;
						if (index2 == len)
							index2 = 0;
					}
					return charArray[index1] - charArray[index2];
				});

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < len; i++)
			sb.append(last[index.get(i)]);

		return sb.toString();
	}

	public String deCode(String str) {
		int len = str.length();
		char[] lastCol = str.toCharArray();
		List<Integer> sortIndex = new ArrayList<>();

		for (int i = 0; i < len; i++)
			sortIndex.add(i);

		// 在最前面加上最后一列后，因为后一列的顺序已经是排好的，且此排序是稳定排序，
		// 所以只需对最后一列排序一次，记录下交换的位置即可
		Collections.sort(sortIndex, (i1, i2) -> lastCol[i1] - lastCol[i2]);

		int index = 0;
		for (int j = 0; j < lastCol.length; j++) {
			if (lastCol[j] == '\0')
				index = j;
		}

		char[] newCol1 = new char[len];
		char[] newCol2 = lastCol.clone();

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < len - 1; i++) {
			if (i % 2 == 0) {
				for (int j = 0; j < len; j++)
					newCol1[j] = newCol2[sortIndex.get(j)];
				sb.append(newCol1[index]);
			} else {
				for (int j = 0; j < len; j++)
					newCol2[j] = newCol1[sortIndex.get(j)];
				sb.append(newCol2[index]);
			}
		}

		return sb.toString();
	}

	private String bwtFilename(String filepath) {
		String bwtFilename = "";
		String[] t = filepath.split("\\.");
		for (int i = 0; i < t.length - 1; i++)
			bwtFilename += t[i];
		bwtFilename += ".bwt";
		return bwtFilename;
	}

	private String ibwtFilename(String filepath) {
		String filename = "";
		String[] temp = filepath.split("\\.");
		for (int i = 0; i < temp.length - 1; i++)
			filename += temp[i];

		if (new File(filename + ".fastq").exists()) {
			int c = 1;
			while (new File(filename + c + ".fastq").exists())
				c += 1;
			filename += c + ".fastq";
		}
		return filename;
	}

	public void bwt(String filepath, String type) throws IOException {

		String filename;
		int len = 1024 * 100;
		if (type.equals("bwt")) {
			filename = bwtFilename(filepath);
		} else {
			filename = ibwtFilename(filepath);
			len++;
		}

		InputStream is = new FileInputStream(filepath);
		BufferedInputStream in = new BufferedInputStream(is);

		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filename));

		byte[] bytes = new byte[len];
		long byteNum = is.available();

		while (byteNum > 0) {

			if (byteNum >= len) {
				in.read(bytes);
			} else {
				bytes = new byte[(int) byteNum];
				in.read(bytes);
			}

			if (type.equals("bwt"))
				out.write(enCode(new String(bytes) + "\0").getBytes());
			else
				out.write(deCode(new String(bytes)).getBytes());
			System.out.println(byteNum);
			byteNum -= len;

		}

		in.close();
		out.close();
	}

	public static void main(String[] args) throws IOException {

		// String test = "ban\ncnd\0";
		// BWT bwt = new BWT();
		// String r = bwt.enCode(test);
		// System.out.println(r);
		// System.out.println("----------------------------------");
		// System.out.println(bwt.deCode(r));

		BWT bwt = new BWT();
		long t = System.currentTimeMillis();
		bwt.bwt("test1_.fastq", "bwt");
		bwt.bwt("test1_.bwt", "ibwt");
		System.out.println("time: " + (System.currentTimeMillis() - t));

	}
}