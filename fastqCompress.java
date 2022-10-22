import java.io.IOException;
import java.util.logging.Handler;

import algorithm.Huffman;
import algorithm.CompressionData;


public class fastqCompress {
    public static void main(String[] args) throws IOException {
        Huffman huffman = new Huffman();

        huffman.countText("as");
    }
}
