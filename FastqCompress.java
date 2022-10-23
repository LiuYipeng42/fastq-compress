import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import algorithm.Algorithm;
import algorithm.Huffman;
import algorithm.LZW;
import pojo.CompressResult;

public class FastqCompress extends Algorithm {

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

    public void compress(String filepath) throws IOException {

		// 获取压缩后的文件名
		String compressFilename = "";
		String[] t = filepath.split("\\.");
		for (int i = 0; i < t.length - 1; i++) 
			compressFilename += t[i];
		compressFilename += ".fmix";

        Huffman huffman = new Huffman();
        LZW lzw = new LZW();

        File file = new File(filepath);
        BufferedReader reader = new BufferedReader(new FileReader(file));

        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(compressFilename));

        String line;
        int cnt = 0;

        ArrayList<Byte> bytes = new ArrayList<>();

        reader.mark((int) file.length() + 1);
        while ((line = reader.readLine()) != null) {
            if (cnt % 4 == 3)
                huffman.countText(line);
            cnt++;
        }

        CompressResult data;
        int buffer = 0;
        int len = 0;
        cnt = 0;
        reader.reset();
        huffman.buildTrie();
        huffman.buildHuffmanCode();
        while ((line = reader.readLine()) != null) {

            // lzw
            if (cnt % 4 == 0) {
                bytes = lzw.compressText(line);
                // 字节数
                out.write(bytes.size() >> 8);
                out.write(bytes.size() & 0xff);
                for (byte b : bytes)
                    out.write(b);
            }
            // bit
            if (cnt % 4 == 1) {
                // 字符数
                out.write(line.length() >> 8);
                out.write(line.length() & 0xff);
                for (int j = 0; j < line.length(); j++) {
                    buffer <<= 2;
                    if (line.charAt(j) == 'T')
                        buffer |= 1;
                    if (line.charAt(j) == 'G')
                        buffer |= 2;
                    if (line.charAt(j) == 'C')
                        buffer |= 3;
                    len += 2;
                    if (len == 8) {
                        out.write(buffer);
                        len = 0;
                    }
                }
                if (buffer != 0)
                    out.write(buffer << (8 - len));
            }

            // huffman
            if (cnt % 4 == 3) {
                data = huffman.compressText(line);
                // bit 数
                out.write(data.getBitLen() >> 8);
                out.write(data.getBitLen() & 0xff);
                for (byte b : data.getBytes())
                    out.write(b);
            }
            cnt++;
        }

        reader.close();
        out.close();
    }

}
