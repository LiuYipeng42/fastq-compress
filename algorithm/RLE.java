package algorithm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class RLE {

	private int eBitLen = 3; // 表示连续相等字符串的长度的 bit 数
	private int eLenMax = (int) Math.pow(2, eBitLen - 1) - 1;

	private int neBitLen = 5; // 表示连续不相等字符串的长度的 bit 数
	private int neLenMax = (int) Math.pow(2, neBitLen - 1) - 1;

	private int buffer;
	private int len = 0;

	private void writeCnt(boolean equal, int cnt, BufferedOutputStream out) throws IOException {

		int bitLen = equal ? eBitLen : neBitLen;
		int lenMax = equal ? eLenMax : neLenMax;

		for (int i = 0; i < bitLen; i++) {
			buffer <<= 1;
			if (i == 0)
				buffer |= equal ? 1 : 0;
			else {
				if (cnt > lenMax)
					buffer |= 1;
				else
					buffer |= (cnt & (1 << (bitLen - 1 - i))) != 0 ? 1 : 0;
			}
			len++;
			if (len == 8) {
				out.write(buffer);
				buffer = 0;
				len = 0;
			}
		}
	}

	private void writeEqual(byte b, int cnt, BufferedOutputStream out) throws IOException {

		while (cnt > 0) {

			writeCnt(true, cnt, out);

			for (int i = 7; i >= 0; i--) {
				buffer <<= 1;
				buffer |= (b & (1 << i)) != 0 ? 1 : 0;
				len++;
				if (len == 8) {
					out.write(buffer);
					buffer = 0;
					len = 0;
				}
			}

			cnt -= eLenMax;
		}

	}

	private void writeNotEqual(byte[] bytes, int cnt, BufferedOutputStream out) throws IOException {

		int index = 0;
		int maxSize = 0;

		while (cnt > 0) {

			writeCnt(false, cnt, out);

			if (cnt > neLenMax)
				maxSize = neLenMax;
			else
				maxSize = cnt;

			for (int i = 0; i < maxSize; i++) {
				for (int j = 7; j >= 0; j--) {
					buffer <<= 1;
					len++;
					buffer |= (bytes[index] & (1 << j)) != 0 ? 1 : 0;
					if (len == 8) {
						out.write(buffer);
						buffer = 0;
						len = 0;
					}
				}
				index++;
			}

			cnt -= neLenMax;
		}
	}

	public void compress(String filepath) throws IOException {

		// 获取压缩后的文件名
		String compressFilename = "";
		String[] t = filepath.split("\\.");
		for (int i = 0; i < t.length - 1; i++)
			compressFilename += t[i];
		compressFilename += ".rle";

		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(compressFilename));

		InputStream is = new FileInputStream(filepath);
		BufferedInputStream in = new BufferedInputStream(is);

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

		byte[] charBuffer = new byte[1024];
		out: while (true) {
			in.read(bytes);

			for (int i = 0; i < bytes.length; i++) {
				now = bytes[i];

				cnt++;
				if (pre != now) {
					// 如果从连续相等中断，则保存前面连续相等字符串的数据
					if (equal && cnt > 0) {
						writeEqual(pre, cnt, out);
						cnt = 0;
					}

					// 将不连续相等的字符保存
					// 因为连续相等中断时，now 是连续相等字符串的最后一个字符，
					// 所以此时不保存
					if (!equal)
						charBuffer[cnt - 1] = pre;

					equal = false;
				} else {
					// 如果从不连续相等变为连续相等，则保存前面不连续相等字符串的数据
					if (!equal && cnt - 1 > 0) {
						writeNotEqual(charBuffer, cnt - 1, out);
						charBuffer = new byte[1024];
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
						writeEqual(pre, cnt, out);
					else {
						charBuffer[cnt - 1] = pre;
						writeNotEqual(charBuffer, cnt, out);
					}
					break out;
				}
			}
		}

		if (len != 0)
			out.write(buffer << 8 - len);

		in.close();
		out.close();

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

		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(getExpendFilename(filepath)));

		FileInputStream is = new FileInputStream(filepath);
		int byteNum = is.available();
		BufferedInputStream in = new BufferedInputStream(is);

		byte[] bytes = new byte[1024 * 1024];
		int index = 0;
		boolean bit = false;

		// 当前读取的状态
		// 0 表示读取标志位，1 表示读取字符数，2 表示读取字符
		int status = 0;  
		boolean equal = false;
		int cnt = 0;
		int bitLen = 0;  // 所要读取的数据 bit 长度
		int buffer = 0;

		out: while (true) {
			in.read(bytes);
			for (int i = 0; i < bytes.length; i++) {
				for (int j = 7; j >= 0; j--) {
					bit = (bytes[i] & (1 << j)) != 0 ? true : false;

					// 读取标志位
					if (status == 0){
						equal = bit;
						bitLen = 0;
						status = 1;
						continue;
					}

					// 读取字符数
					if (status == 1){
						cnt <<= 1;
						if (bit)
							cnt |= 1;
						bitLen ++;
						if ((equal && bitLen == eBitLen - 1) || (!equal && bitLen == neBitLen - 1)){
							bitLen = 0;
							status = 2;
							continue;
						}	
					}

					// 读取字符
					if (status == 2){
						buffer <<= 1;
						if (bit)
							buffer |= 1;
						
						if (equal){
							// 写入连续重复的字符
							bitLen ++;
							if (bitLen == 8){
								for (int k = 0; k < cnt; k++) 
									out.write(buffer);
								bitLen *= cnt;
							}
						} else {
							// 写入不连续重复的字符
							bitLen ++;
							if (bitLen > 0 && bitLen % 8 == 0)
								out.write(buffer);
						}
						if (bitLen == 8 * cnt){
							status = 0;
							cnt = 0;
							bitLen = 0;
							continue;
						}
					}
				}

				index++;
				if (index == byteNum) {
					break out;
				}
			}
		}

		in.close();
		out.close();
	}

	public static void main(String[] args) throws IOException {
		RLE rle = new RLE();
		long t = System.currentTimeMillis();
		String file = "dataset";
		rle.compress(file + ".fastq");
		System.out.println("---------------------");
		rle.expend(file + ".rle");
		System.out.println("time: " + (System.currentTimeMillis() - t));

	}
}
