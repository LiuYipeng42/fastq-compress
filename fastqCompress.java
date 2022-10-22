import java.io.IOException;
import java.util.ArrayList;

import algorithm.Huffman;
import algorithm.LZW;
import pojo.CompressionData;


public class fastqCompress {
    public static void main(String[] args) throws IOException {
        

        String text = "CGAGGTTGCGGTGCCACTTCACGCTCCCGTATGAAAAGCTGACCCGCTTCGCTCGAGGGCTCTTCCGCTCCGAGGAG";
        byte[] bytes;

        // Huffman huffman = new Huffman();
        // huffman.countText(text);
        // huffman.countText(text);
        // huffman.buildTrie();
        // huffman.buildHuffmanCode();
        // CompressionData data = huffman.compressText(text);
        // bytes = new byte[data.getBitLen() / 8 + 1];

        // for (int i = 0; i < bytes.length; i++) 
        //     bytes[i] = data.getBytes().get(i);

        // String t = huffman.expendBytes(huffman.getTrie(), bytes, data.getBitLen());
        // System.out.println(t);

        
        // LZW lzw = new LZW();
        // ArrayList<Byte> data = lzw.compressText(text);
        // bytes = new byte[data.size()];

        // for (int i = 0; i < bytes.length; i++) 
        //     bytes[i] = data.get(i);

        // String t = lzw.expendBytes(bytes);
        // System.out.println(t);
    }
}
