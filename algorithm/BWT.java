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

		// 循环字符串矩阵最后一列从上到下是将 原字符串逆序后加上分隔符
		for (int i = 0; i < len - 1; i++)
			last[i] = charArray[len - i - 2];
		last[len - 1] = charArray[len - 1];

		for (int i = 0; i < len; i++)
			index.add(i);

		// 对循环字符串矩阵的每一行进行字典序排序（若某一列相等，则比较下一列），
		// 因为排序完成后只需要矩阵的最后一列，
		// 所以可以按照字符数组，对其序号排序，
		// 排完序后，index[i] 就是最后一列中第 i 个元素的位置
		Collections.sort(
				index,	
				(i1, i2) -> {
					// i1 和 i2 是 原字符串 的序号
					// 要经过转换变成循环字符串矩阵 第一列 的序号
					// 第一列 是 原字符串的逆序
					int index1 = len - i1 - 1;
					int index2 = len - i2 - 1;
					// 当前的两个字符相等，就比较下一个字符
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
		// bwt 在还原变换时要重建循环字符串矩阵
		// 首先将压缩后的字符串按照顺序从上到下放到最后一列
		// 然后插入到第一列，对矩阵进行每一行进行排序（不包括最后一列），
		// 然后再次将最后一列插入到第一列，重复以上步骤，直到矩阵填满

		int len = str.length();
		char[] lastCol = str.toCharArray();
		List<Integer> sortIndex = new ArrayList<>();

		for (int i = 0; i < len; i++)
			sortIndex.add(i);

		// 在最前面加上最后一列后，就需要进行行排序，若遇到某两行的同一列相等，
		// 就需要比较后一列，因为后面一列元素在上一轮操作中已经排好序，且使用的排序算法是稳定排序，
		// 所以不需要继续向后面比较，两者的顺序不需要变换就是正确的。所以在进行行排序时
		// 所以只需对最后一列排序一次，记录下交换的位置即可，之后每一次按照固定的顺序交换即可
		Collections.sort(sortIndex, (i1, i2) -> lastCol[i1] - lastCol[i2]);

		// 还原完矩阵后，若某一行最后一个的元素为分隔符时，此行就是解压后的字符串
		// 所以要找到对应的行数，之后只需要得到这一行的数据就可以了，不需要其他行的数据
		int index = 0;
		for (int j = 0; j < lastCol.length; j++) {
			if (lastCol[j] == '\0')
				index = j;
		}

		// 对最后一列按照排序后得到的正确位置进行 len - 1 第变换
		// 第 i 次变换可以得到第 i 列的原矩阵数据
		// 在两个数组中来回变换，每次得到的新的一列的第 index 行就是变换前的数据
		StringBuilder sb = new StringBuilder();
		char[] newCol1 = new char[len];
		char[] newCol2 = lastCol.clone();

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

		String test = "banana$";
		BWT bwt = new BWT();
		String r = bwt.enCode(test);
		System.out.println(r);
		System.out.println("----------------------------------");
		System.out.println(bwt.deCode(r));

		// BWT bwt = new BWT();
		// long t = System.currentTimeMillis();
		// bwt.bwt("test1_.fastq", "bwt");
		// bwt.bwt("test1_.bwt", "ibwt");
		// System.out.println("time: " + (System.currentTimeMillis() - t));

	}
}
