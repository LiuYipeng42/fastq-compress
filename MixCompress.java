import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import algorithm.Algorithm;
import algorithm.Huffman;
import algorithm.LZW;
import pojo.CompressResult;
import pojo.HuffmanNode;

public class MixCompress extends Algorithm {

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
		compressFilename += ".mix";

        Huffman huffman = new Huffman();
        LZW lzw = new LZW();

        File file = new File(filepath);
        BufferedReader reader = new BufferedReader(new FileReader(file));

        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(compressFilename));

        String line;
        int lineCnt = 0;

        ArrayList<Byte> bytes = new ArrayList<>();

        reader.mark((int) file.length() + 1);
        while ((line = reader.readLine()) != null) {
            if (lineCnt % 4 == 3)
                huffman.countText(line);
            lineCnt++;
        }

        CompressResult data;
        int buffer = 0;
        int len = 0;
        reader.reset();
        huffman.buildTrie();
        huffman.buildHuffmanCode();

        // 将树序列化到 objBytes 中
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bo);
        oos.writeObject(huffman.getTrie());
        byte[] objBytes = bo.toByteArray();
        int objByteLen = objBytes.length;

        // 写入行数，24 位表示
        out.write(lineCnt >> 16);
        out.write(lineCnt >> 8);
        out.write(lineCnt);

        // 用 32 位来表示哈夫曼树的长度
        for (int i = 3; i >= 0; i--) 
            out.write((int) (objByteLen >> (8 * i)));

        // 将序列化的树写入文件
        out.write(objBytes);

        lineCnt = 0;
        while ((line = reader.readLine()) != null) {

            // lzw
            if (lineCnt % 4 == 0) {
                bytes = lzw.compressText(line);
                // 字节数，用 16 位 bit 表示长度
                out.write(bytes.size() >> 8);
                out.write(bytes.size() & 0xff);
                for (byte b : bytes)
                    out.write(b);
            }
            // bit
            if (lineCnt % 4 == 1) {
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
                        buffer = 0;
                        len = 0;
                    }
                }
                if (len != 0){
                    out.write(buffer << (8 - len));
                    buffer = 0;
                    len = 0;
                }
            }

            // huffman
            if (lineCnt % 4 == 3) {
                data = huffman.compressText(line);
                // bit 数
                out.write(data.getBitLen() >> 8);
                out.write(data.getBitLen() & 0xff);
                for (byte b : data.getBytes()){
                    out.write(b);
                }
            }
            lineCnt++;
        }

        reader.close();
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
			while (new File(expendFilename + c + ".fastq").exists()) 
				c += 1;
			expendFilename += c + ".fastq";
		}
		return expendFilename;
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

    public void expend(String filepath) throws IOException{

        FileInputStream is = new FileInputStream(filepath);
		BufferedInputStream in = new BufferedInputStream(is);
		BufferedWriter out = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(getExpendFilename(filepath))));

        // 读取行数
		ByteBuffer byteBuffer;
		byte[] lineNumBytes = new byte[3];
		in.read(lineNumBytes);
		int lineNum = 0;

        for (int i = 0; i < 6; i++) {
            lineNum <<= 4;
            lineNum |= (lineNumBytes[i / 2] >> 4 * (i % 2 == 0 ? 1 : 0)) & 0xf;
        }
        
        // 读取树对象的长度
		byte[] objLenNum = new byte[4];
		in.read(objLenNum);
		byteBuffer = ByteBuffer.wrap(objLenNum, 0, 4);
		int objLen = byteBuffer.getInt();

		// 读取树
		byte[] objBytes = new byte[objLen];
		in.read(objBytes);
		HuffmanNode root = (HuffmanNode) deserialize(objBytes);

        byte[] bytes = new byte[1024 * 1024];

        int dataLen = 0;
        int dataLenByteNum = 0;
        byte[] lineBuffer = new byte[65536];
        int index = 0;

        int lineCnt = 0;

        Huffman huffman = new Huffman();
        LZW lzw = new LZW();

        StringBuilder dnaStr = new StringBuilder();
        int huffmanByteNum = 0;

        out: while (true) {
			in.read(bytes);
			for (int i = 0; i < bytes.length; i++) {
                if (dataLenByteNum < 2){
                    for (int j = 1; j >= 0; j--) {
                        dataLen <<= 4;
                        dataLen |= (bytes[i] >> 4 * j) & 0xf;
                    }
                    dataLenByteNum ++;
                } else {
                    if (lineCnt % 4 == 0){
                        // lzw
                        lineBuffer[index] = bytes[i];
                        index ++;
                        if (index == dataLen){
                            out.write(lzw.expendBytes(lineBuffer, dataLen) + "\n");
                            lineCnt ++;
                            dataLen = 0;
                            dataLenByteNum = 0;
                            index = 0;
                            continue;
                        }
                    }
                    if (lineCnt % 4 == 1){
                        // bit
                        for (int j = 3; j >= 0; j--) {
                            if ((bytes[i] >> (2 * j) & 3) == 0)
                                dnaStr.append("A");
                            if ((bytes[i] >> (2 * j) & 3) == 1)
                                dnaStr.append("T");
                            if ((bytes[i] >> (2 * j) & 3) == 2)
                                dnaStr.append("G");
                            if ((bytes[i] >> (2 * j) & 3) == 3)
                                dnaStr.append("C");
                            if (dnaStr.length() == dataLen)
                                break;
                        }
                        if (dnaStr.length() == dataLen){

                            out.write(dnaStr.toString() + "\n");
                            lineCnt ++;
                            // +
                            out.write("+\n");
                            lineCnt ++;

                            dnaStr = new StringBuilder();
                            dataLen = 0;
                            dataLenByteNum = 0;
                            continue;
                        }
                    }
                    if (lineCnt % 4 == 3){
                        // huffman
                        lineBuffer[index] = bytes[i];
                        index ++;
                        if (index == 1){
                            huffmanByteNum = dataLen / 8;
                            if (huffmanByteNum < dataLen * 1.0 / 8)
                            huffmanByteNum ++;
                        }

                        if (index == huffmanByteNum){
                            out.write(huffman.expendBytes(root, lineBuffer, dataLen));
                            if (lineCnt != lineNum - 1)
                                out.write("\n");
                            lineCnt ++;
                            dataLen = 0;
                            dataLenByteNum = 0;
                            index = 0;
                        }
                    }
                    
                    if (lineCnt == lineNum)
                        break out;
                }

			}
		}

        in.close();
        out.close();

    }

}
