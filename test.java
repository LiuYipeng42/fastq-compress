import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import algorithm.Algorithm;
import algorithm.BWT;
import algorithm.Huffman;
import algorithm.LZW;
import algorithm.RLE;
import algorithm.ShannonFano;
import pojo.CompressResult;


public class test {

	public static String binToString(int b, int len) {
		String result = "";
		int a = b;

		for (int j = 0; j < len; j++) {
			int c = a;
			a = a >> 1;
			a = a << 1;
			if (a == c) {
				result = "0" + result;
			} else {
				result = "1" + result;
			}
			a = a >> 1;
		}

		return result;
	}

	public static void compressTest(String algo, String file) throws IOException{

		Algorithm algorithm = null;

		switch(algo){
			case "huffman":
				algorithm = new Huffman();
				break;
			case "sf":
				algorithm = new ShannonFano();
				break;
			case "rle":
				algorithm = new RLE();
				break;
			case "lzw":
				algorithm = new LZW();
				break;
			case "fmix":
				algorithm = new FastqCompress();
		}

		System.out.println(algo);

		long t;
	
		t = System.currentTimeMillis();
		algorithm.compress(file + ".fastq");
		System.out.println("compress time: " + (System.currentTimeMillis() - t));

		t = System.currentTimeMillis();
		algorithm.expend(file + "." + algo);
		System.out.println("expend time: " + (System.currentTimeMillis() - t));

		File originalFile = new File(file + ".fastq");
		File compressFile = new File(file + "." + algo);
		System.out.println("before compress size: " + originalFile.length());
		System.out.println("after compress size: " + compressFile.length());

		System.out.println("compress rate: " + compressFile.length() * 1.0 / originalFile.length());

	}

	public static void BWTtest(String filepath) throws IOException {

		BWT bwt = new BWT();
		long t;

		t = System.currentTimeMillis();
		bwt.bwt(filepath, "bwt", 1024 * 10);
		System.out.println("encode time: " + (System.currentTimeMillis() - t));

		t = System.currentTimeMillis();
		bwt.bwt(filepath, "ibwt", 1024 * 10);
		System.out.println("decode time: " + (System.currentTimeMillis() - t));

		InputStream is = new FileInputStream(filepath);
		BufferedInputStream in = new BufferedInputStream(is);

		int equalLen = 0;
		int notEqualLen = 0;
		int equalMax = 0;
		int notEqualMax = 0;
		int all = 0;

		byte[] bytes = new byte[1024 * 1024];
		long byteNum = is.available();

		long index = 0;
		int cnt = 0;
		boolean equal = false;

		// 给 pre 变量提前读取一个字节
		byte[] temp = new byte[1];
		in.read(temp);
		index++;
		byte pre = temp[0];
		byte now;

		out: while (true) {
			in.read(bytes);

			for (int i = 0; i < bytes.length; i++) {
				all ++;

				now = bytes[i];

				cnt++;
				if (pre != now) {
					if (equal && cnt > 0) {
						if (cnt > 1)
							equalLen += cnt;
						if (cnt > equalMax)
							equalMax = cnt;
						cnt = 0;
					}

					equal = false;
				} else {
					// 如果从不连续相等变为连续相等，则保存前面不连续相等字符串的数据
					if (!equal && cnt - 1 > 0) {
						if (cnt > 1)
							notEqualLen += cnt - 1;
						if (cnt > notEqualMax)
							notEqualMax = cnt;
						cnt = 1;
					}
					equal = true;
				}

				pre = now;
				index++;
				if (index == byteNum) {
					// 此时 now（或pre）变量的数据并没有经过程序的判断
					if (cnt > 0)
						cnt++;
					if (equal)
						if (cnt > 1)
							equalLen += cnt;
						if (cnt > equalMax)
							equalMax = cnt;
					else {
						if (cnt > 1)
							notEqualLen += cnt - 1;
						if (cnt > notEqualMax)
							notEqualMax = cnt;
					}
					break out;
				}
			}
		}
		in.close();	

		System.out.println("连续相等的字符最大长度：" + equalMax);
		System.out.println("连续相等的字符总长度：" + equalLen);
		System.out.println("总长度占文件长度的比例：" + equalLen * 1.0 / all);
		System.out.println("---------------------------------");
		System.out.println("连续不相等的字符最大长度：" + notEqualMax);
		System.out.println("连续不相等的字符总长度：" + notEqualLen);
		System.out.println("总长度占文件长度的比例：" + notEqualLen * 1.0 / all);

	}

	// rm *.lzw *.huffman *.sf *.rle *.bwt *.fmix test[0-9][0-9].fastq dataset[1-9].fastq
	public static void main(String[] args) throws IOException {
		// BWTtest("test_data/.fastq");

		String[] algos = new String[] {"huffman", "sf", "lzw", "rle", "fmix"};
		String file = "test_data/test2";

		for (String algo : algos) {
			System.out.println("----------------------");
			compressTest(algo, file);
		}

	}

}