import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;

class Prefix {
	String str;
	int code;

	public Prefix(String str, int code) {
		this.str = str;
		this.code = code;
	}
}

// 三向单词查找树
// 每个结点都含有一个字符、三条链接和一个值，
// 三条链接分别对应着当前字母小于、等于和大于结点字母的值
class TST {

	private class Node {
		char c; // 字符
		Node left, mid, right;
		Integer val; // 编码值
	}

	private Node root;

	// 在树中查找字符串 s 的 beginIndex 之后部分的最长前缀
	public Prefix longestPrefixOf(String s, int beginIndex) {

		int[] endIndexAndVal = search(root, s, beginIndex, 0, 0);

		if (endIndexAndVal[0] >= beginIndex)
			return new Prefix(s.substring(beginIndex, endIndexAndVal[0] + 1), endIndexAndVal[1]);
		else
			return null;
	}

	// 最长前缀的最后一个字符在字符串 s 中的位置
	// 字符串 s 的 index 之后的部分和树一起遍历
	private int[] search(Node node, String s, int index, int endIndex, int val) {

		// 上一个访问的结点是叶子结点，表明已经找到最长前缀
		if (node == null)
			return new int[] { endIndex, val };
		// index 达到字符串的最后一位，返回找到的最长前缀
		if (index == s.length())
			return new int[] { endIndex, val };

		// c 为字符串 index 位置的字符
		char c = s.charAt(index);

		// 若当前的字符与当前结点的代表的字符相同
		// 则字符串索引 index 加 1，当前访问结点变为中间子结点
		Node next = null;
		if (c == node.c) {
			if (node.val != null) {
				endIndex = index;
				val = node.val;
			}
			index++;
			next = node.mid;
		}

		// 若小于，则当前访问结点变为左子结点
		if (c < node.c)
			next = node.left;

		// 若大于，则当前访问结点变为右子结点
		if (c > node.c)
			next = node.right;

		return search(next, s, index, endIndex, val);

	}

	// 获取一个字符串的编码值
	public Integer get(String key) {

		Node x = get(root, key, 0);

		if (x == null)
			return null;
		return x.val;
	}

	private Node get(Node x, String key, int index) {

		if (x == null)
			return null;

		char c = key.charAt(index);

		if (c < x.c)
			return get(x.left, key, index);
		else if (c > x.c)
			return get(x.right, key, index);
		else if (index < key.length() - 1)
			return get(x.mid, key, index + 1);
		else
			return x;
	}

	// 将字符串 key 的编码之存储到 key 最后一个字符对应结点上
	// 要先找到 key 最后一个字符对应的结点，然后存储
	public void put(String key, int val) {
		root = put(root, key, val, 0);
	}

	private Node put(Node x, String key, int val, int d) {

		char c = key.charAt(d);

		if (x == null) {
			x = new Node();
			x.c = c;
		}

		if (c < x.c)
			x.left = put(x.left, key, val, d);
		else if (c > x.c)
			x.right = put(x.right, key, val, d);
		else if (d < key.length() - 1)
			x.mid = put(x.mid, key, val, d + 1);
		else
			x.val = val;
		return x;
	}

}

public class LZW {

	private String text;

	private void readFile(String filepath) throws FileNotFoundException, IOException {
		InputStream is = new FileInputStream(filepath);
		byte[] bytes = new byte[is.available()];
		is.read(bytes);
		this.text = new String(bytes);
		is.close();
	}

	public void compress(String filepath) throws IOException {
		int R = 256; // 输入字符数，asc有 177 个 ，需要 8 位
		int L = 4096; // 编码总数 2^12

		readFile(filepath);
		TST st = new TST();

		// 先将每一种字符的编码存储到树中
		for (int i = 0; i < R; i++)
			st.put("" + (char) i, i);

		// 获取加密后的文件名称
		String compressFilename = "";
		String[] t = filepath.split("\\.");
		for (int i = 0; i < t.length - 1; i++) {
			compressFilename += t[i];
		}
		compressFilename += ".lzw";

		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(compressFilename));

		int code = R + 1;
		int beginIndex = 0;
		int buffer = 0;
		int len = 0;

		while (beginIndex < text.length()) {

			// 找到最长前缀及其编码
			Prefix prefix = st.longestPrefixOf(text, beginIndex);

			// 存储最长前缀的编码，编码是 12 位
			for (int i = 11; i >= 0; i--) {
				buffer <<= 1;
				buffer |= ((prefix.code & (1 << i)) != 0) ? 1 : 0;
				len++;
				if (len == 8) {
					out.write(buffer);
					buffer = 0;
					len = 0;
				}
			}

			// 最长前缀 加上 之后一个字符 ，将形成的字符串存储到树中，编码为当前编码加 1
			int plen = prefix.str.length();
			if (plen < text.length() - beginIndex && code < L)
				st.put(text.substring(beginIndex, beginIndex + plen + 1), code++);

			beginIndex += plen;
		}

		if (buffer != 0) {
			buffer <<= 4;
			out.write(buffer);
		}

		out.close();

		System.out.println("compress success");

	}

	private String getExpendFilename(String filepath) {
		String expendFilename = "";
		String[] temp = filepath.split("\\.");
		for (int i = 0; i < temp.length - 1; i++) {
			expendFilename += temp[i];
		}

		if (new File(expendFilename + ".fastq").exists()) {
			int c = 1;
			while (new File(expendFilename + c + ".fastq").exists()) {
				c += 1;
			}
			expendFilename += c + ".fastq";
		}
		return expendFilename;
	}

	public void expend(String filepath) throws IOException {
		
		// 编码表：数组下标为编码
		// 建表方法：编码表中新的编码（编码序号加 1）对应的字符串 为 
		//         当前输入编码对应的字符串 加上 下一个输入编码对应的字符串的首字母

		String[] st = new String[4096];

		// 初始化编码到字符串的编码表
		int stIndex = 0;
		for (stIndex = 0; stIndex < 256; stIndex++)
			st[stIndex] = "" + (char) stIndex;
		st[stIndex++] = " ";

		FileInputStream is = new FileInputStream(filepath);
		BufferedInputStream in = new BufferedInputStream(is);
		BufferedWriter out = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(getExpendFilename(filepath))));
		
		long byteNum = is.available();
		long index = 0;
		int nextCode = 0;  // 前瞻字符（对于当前编码的下一个编码对应字符串的首字符）所在字符串的编码
		String val; // 当前读取的编码的对应字符串
		int len = 0;

		// 首先要读取一个编码，因为 编码为 12 位，所以要读取两个字节
		byte[] t = new byte[2];
		in.read(t);
		index = 2;

		// 取高 12位 数据，获得编码值
		nextCode |= t[0];
		nextCode <<= 4;
		nextCode |= (t[1] >> 4) & 0xf;
		val = st[nextCode];

		nextCode = t[1] & 0xf;
		len = 4;

		out: while (true) {
			byte[] bytes = new byte[1024 * 1024];
			in.read(bytes);
			for (int i = 0; i < bytes.length; i++) {
				for (int j = 1; j >= 0; j--) {
					nextCode <<= 4;
					nextCode |= (bytes[i] >> 4 * j) & 0xf;
					len += 4;
					// 编码长度为 12，所以 len 为 12 时，就读取到了一个编码 
					if (len == 12) {

						// 编码对应的字符串输出到文件中
						for (int k = 0; k < val.length(); k++) 
							out.write(val.charAt(k));
						
						// 找到前瞻字符所在的字符串
						String s = st[nextCode];
						// 当前需要存储到编码表的编码 stIndex 和 刚读取到的下一个编码 nextCode 相同
						// 此时因为 stIndex 在符号表中还没有值，所以 s 为空
						// s 就变为当前编码的字符串加上此字符串的首字符
						if (stIndex == nextCode)
							s = val + val.charAt(0);
						// 下一个编码对应的字符串是 当前字符串 加上 前瞻字符
						if (stIndex < 4096)
							st[stIndex++] = val + s.charAt(0);

						// 当前编码的字符串 更新为 下一个编码的字符串
						val = s; 
						nextCode = 0;
						len = 0;
					}
				}

				index++;
				if (index == byteNum + 2) {
					break out;
				}
			}
		}
		in.close();

		out.flush();
		out.close();

		System.out.println("expend success");

	}

	public static void main(String[] args) throws IOException {

		LZW lzw = new LZW();

		long t = System.currentTimeMillis();
		lzw.compress("dataset.fastq");
		System.out.println("------------------------");
		lzw.expend("dataset.lzw");
		System.out.println("time: " + (System.currentTimeMillis() - t));

	}

}
