import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;


class FileCompresser{

    private static int R = 256;


    private static class HufmanNode implements Comparable<HufmanNode> {
        char ch;
        int freq;
        HufmanNode left, right;

        HufmanNode(char ch, int freq, HufmanNode left, HufmanNode right) {
            this.ch = ch;
            this.freq = freq;
            this.left = left;
            this.right = right;
        }

        boolean isLeaf() {
            return (left == null) && (right == null);
        }

        public int compareTo(HufmanNode that) {
            return this.freq - that.freq;
        }
    }

    private static class CompressionResult {
        HufmanNode huffmanCodes;
        String Bits;

        public CompressionResult(String bits, HufmanNode tree) {
            this.huffmanCodes = tree;
            this.Bits = bits;
        }
    }

    public static void encode(String inputFile) {
        HashMap<Byte, Integer> freq = new HashMap<>();
    
        try (FileInputStream fis = new FileInputStream(inputFile)) {
            int b;
            while ((b = fis.read()) != -1) {
                byte byteVal = (byte) b;
                freq.put(byteVal, freq.getOrDefault(byteVal, 0) + 1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    
        for (Map.Entry<Byte, Integer> entry : freq.entrySet()) {
            int unsignedVal = entry.getKey() & 0xFF;
            System.out.println("Byte: " + unsignedVal + ", Frequency: " + entry.getValue());
        }

        HufmanNode root = buildHufmanTree(freq);

  
    }

    private static HufmanNode buildHufmanTree(HashMap<Byte, Integer> freq) {
  
        PriorityQueue<HufmanNode> pq = new PriorityQueue<>();
        for (Map.Entry<Byte, Integer> f: freq.entrySet()){
            if (f.getValue()>0)
                pq.add(new HufmanNode((char) (f.getKey() & 0xFF),f.getValue(), null, null));
        }
        while (pq.size() > 1) {
            HufmanNode left = pq.remove();
            HufmanNode right = pq.remove();
            HufmanNode parent = new HufmanNode('\0', left.freq + right.freq, left, right);
            pq.add(parent);
        }
        return pq.remove();
    }

    
    

public static void main(String[] args) {
    if (args.length < 1) {
        System.out.println("Usage: java FileCompresser <input-file>");
        return;
    }

    encode(args[0]);
}

}