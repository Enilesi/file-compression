import java.io.File;
import java.io.FileNotFoundException;
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

    public static void encode(String inputFile){
        HashMap<Character,Integer> freq=new HashMap<>();
        
        try (Scanner sc = new Scanner(new File(inputFile))) {
        sc.useDelimiter("");

        while(sc.hasNext()){
            Character ch = sc.next().charAt(0);
            freq.put(ch, (freq.getOrDefault(ch,0)+1) );
        }
    } catch (FileNotFoundException e) {
        e.printStackTrace();
    }
    for (Map.Entry<Character, Integer> entry : freq.entrySet()) {
        System.out.println("Character: " + entry.getKey() + ", Frequency: " + entry.getValue());
    }
}

public static void main(String[] args) {
    if (args.length < 1) {
        System.out.println("Usage: java FileCompresser <input-file>");
        return;
    }

    encode(args[0]);
}

}