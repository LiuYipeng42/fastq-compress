package algorithm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pojo.SFNode;
import pojo.SFCode;
import pojo.SFCodes;


public class ShannonFano extends Algorithm {

	public SFCodes getSFCodes(Map<Byte, Integer> counts) {

		// 排序，排序可以加快树的构建
		List<Byte> list = sortByCounts(counts);

		SFNode trie = new SFNode();

		// 设置根节点含有的字符
		trie.setBytes(list);

		// 生成树
		genTrie(trie, counts);

		// 构建编码表
		SFCode[] table = buildSFCode(trie);

		SFCodes SFCodes = new SFCodes();
		SFCodes.SFCodeTable = table;
		SFCodes.SFCodeTrie = trie;

		return SFCodes;
	}

	private static List<Byte> sortByCounts(Map<Byte, Integer> counts) {

		Set<Byte> countsKeys = counts.keySet();

		List<Byte> list = new ArrayList<Byte>(countsKeys);

		// 按照字符出现次数，对字符进行从大到小的排序
		Collections.sort(list, (o1, o2) -> -counts.get(o1).compareTo(counts.get(o2)));

		return list;
	}

	private static void genTrie(SFNode SFNode, Map<Byte, Integer> counts) {
		
		List<Byte> list = SFNode.getBytes();

		// 结点中的字符只有一个，说明为叶结点
		if (list.size() <= 1)
			return;

		// 计算总频率
		int fullSum = 0;
		for (byte b : list)
			fullSum += counts.get(b);

		float bestdiff = 5;
		int i = 0;
		int sum = 0;
		// 左子结点中字符的总频率和尽可能接近左子结点中字符的总频率
		while (i < list.size()) {
			float prediff = bestdiff;
			// 计算 i 之前的所有数的和
			sum += counts.get(list.get(i)); 
			// 计算 和 与 0.5 的差的绝对值，因为和要接近总和的一半
			bestdiff = Math.abs((float) sum / fullSum - 0.5F); 
			// 越接近中间值，绝对值就会越小，当绝对值开始变大时，说明刚好经过了中间值
			// 所以绝对值比上一个绝对值大，就跳出循环
			if (prediff < bestdiff) 
				break;
			i++;
		}

		SFNode.setBytes(null);
		// 设置左、右子结点
		SFNode.setLeft(new SFNode(new ArrayList<>(list.subList(0, i))));
		SFNode.setRight(new SFNode(new ArrayList<>(list.subList(i, list.size()))));

		// 下一层递归
		genTrie(SFNode.getLeft(), counts);
		genTrie(SFNode.getRight(), counts);
	}

	private SFCode[] buildSFCode(SFNode trie) {
		SFCode[] table = new SFCode[128];  // 存储 128 个 asc 码的编码
		buildSFCode(table, trie, 0, 0);

		return table;
	}

	private void buildSFCode(SFCode[] table, SFNode SFNode, int SFCode, int len) {
		if (SFNode.isLeaf()) {
			table[SFNode.getBytes().get(0)] = new SFCode(SFCode, len);
			return;
		}

		// 左子结点 0，右子结点 1
		buildSFCode(table, SFNode.getLeft(), SFCode << 1, len + 1);
		buildSFCode(table, SFNode.getRight(), (SFCode << 1) | 1, len + 1);
	}

	private Map<Byte, Integer> buildCounts(String filepath) throws IOException {

		InputStream is = new FileInputStream(filepath);
		BufferedInputStream in = new BufferedInputStream(is);

		byte[] bytes = new byte[1024 * 1024];
		long byteNum = is.available();
		long index = 0;
		HashMap<Byte, Integer> counts = new HashMap<>();

		out: while (true) {
			in.read(bytes);

			for (int i = 0; i < bytes.length; i++) {
				int f = counts.getOrDefault(bytes[i], 0);
				counts.put(bytes[i], f + 1);
				index++;
				if (index == byteNum) {
					break out;
				}
			}
		}

		in.close();

		return counts;
	}

	public void compress(String filepath) throws IOException {

		Map<Byte, Integer> counts = buildCounts(filepath);
		SFCodes SFCodes = getSFCodes(counts);

		SFCode[] table = SFCodes.SFCodeTable;
		SFNode trie = SFCodes.SFCodeTrie;

		// 获取压缩后的文件名
		String compressFilename = "";
		String[] t = filepath.split("\\.");
		for (int i = 0; i < t.length - 1; i++) {
			compressFilename += t[i];
		}
		compressFilename += ".sf";

		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(compressFilename));

		// 将树序列化到 objBytes 中
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bo);
		oos.writeObject(trie);
		byte[] objBytes = bo.toByteArray();
		int objLen = objBytes.length;

		// 将序列化的树的数据长度和数据写入文件，write每次只写低 8 位
		for (int i = 3; i >= 0; i--) {
			out.write((int) (objLen >> (8 * i)));
		}
		out.write(objBytes);

		// 给压缩后的 bit 长度 占位
		for (int i = 0; i < 8; i++) {
			out.write(0);
		}

		// 压缩
		InputStream is = new FileInputStream(filepath);
		BufferedInputStream in = new BufferedInputStream(is);

		byte[] bytes = new byte[1024 * 1024];
		long byteNum = is.available();
		long index = 0;

		SFCode SFCode;
		int buffer = 0;
		int len = 0;
		long bitLen = 0;

		out: while (true) {
			in.read(bytes);
			for (int i = 0; i < bytes.length; i++) {
				SFCode = table[bytes[i]];
				for (int j = SFCode.len - 1; j >= 0; j--) {
					buffer <<= 1;
					if ((SFCode.SFCode & (1 << j)) != 0) {
						buffer |= 1;
					}
					len++;
					bitLen++;
					if (len == 8) {
						out.write(buffer);
						buffer = 0;
						len = 0;
					}
				}
				index++;
				if (index == byteNum) {
					if (len != 0)
						out.write(buffer << 8 - len);
					break out;
				}
			}
		}

		in.close();
		out.flush();
		out.close();

		RandomAccessFile rf = new RandomAccessFile(compressFilename, "rw");
		rf.seek(4 + objLen);
		rf.writeLong(bitLen);
		rf.close();

	}

	private String getExpendFilename(String filepath) {
		String expendFilename = "";
		String[] temp = filepath.split("\\.");
		for (int i = 0; i < temp.length - 1; i++) 
			expendFilename += temp[i];

		if (new File(expendFilename + ".fastq").exists()) {
			int c = 1;
			while (new File(expendFilename + c + ".fastq").exists()) 
				c += 1;
			expendFilename += c + ".fastq";
		}
		return expendFilename;
	}

	public void expend(String filepath) throws IOException {

		BufferedInputStream in = new BufferedInputStream(new FileInputStream(filepath));

		BufferedWriter out = new BufferedWriter(
			new OutputStreamWriter(new FileOutputStream(getExpendFilename(filepath))));

		// 读取树对象的长度
		ByteBuffer buffer;
		byte[] objLenNum = new byte[4];
		in.read(objLenNum);
		buffer = ByteBuffer.wrap(objLenNum, 0, 4);
		int objLen = buffer.getInt();

		// 读取树
		byte[] objBytes = new byte[objLen];
		in.read(objBytes);
		SFNode root = (SFNode) deserialize(objBytes);

		// 读取压缩数据的长度 bit 位数
		byte[] dataSize = new byte[8];
		in.read(dataSize);
		buffer = ByteBuffer.wrap(dataSize, 0, 8);
		long size = buffer.getLong();

		// 解压缩
		byte[] bytes = new byte[1024 * 1024];
		SFNode x = root;

		long n = 0;

		out: while (true) {
			in.read(bytes);
			for (int i = 0; i < bytes.length; i++) {
				for (int j = 7; j >= 0; j--) {
					if ((bytes[i] & (1 << j)) != 0) {
						x = x.getRight();
					} else {
						x = x.getLeft();
					}
					if (x.isLeaf()) {
						out.write(x.getBytes().get(0));
						x = root;
					}
					n++;
					if (n == size) {
						break out;
					}
				}
			}
		}

		in.close();

		out.flush();
		out.close();

	}

	public static Object deserialize(byte[] bytes) {
		Object object = null;
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			ObjectInputStream ois = new ObjectInputStream(bis);
			object = ois.readObject();
			ois.close();
			bis.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		}
		return object;
	}

}
