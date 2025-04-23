import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
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

    public static void encode(String inputFile, String outputFile) {
        HashMap<Byte, Integer> freq = new HashMap<>();
    
        try (FileInputStream fin = new FileInputStream(inputFile)) {
            int b;
            while ((b = fin.read()) != -1) {

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

        String[] st = new String[R];
        getHuffmanCodeTable(st, root, "");

        printEncoded(inputFile,st,freq,outputFile);

        
    }


    private static void printEncoded(String inputFile, String [] st,HashMap<Byte, Integer> freq, String outputFile){
        try (DataOutputStream fout = new DataOutputStream(new FileOutputStream(outputFile))) {
            int freqSize = freq.size(); 
            fout.writeInt(freqSize); 
            
            for (Map.Entry<Byte, Integer> f: freq.entrySet()){
                fout.writeChar(f.getKey());
                fout.writeInt(f.getValue());
                
            }

            long bitsSize = 0;
            long bitsSizePos = fout.size(); 
            fout.writeLong(0);

            FileInputStream fin = new FileInputStream(inputFile);
            int b;
            int bitNr = 0;
            byte bitBuff = 0;

            while((b=fin.read())!=-1){
                byte byteVal =(byte) b;

                String huffCode = st[byteVal & 0xFF];
                
                for(char c: huffCode.toCharArray()){
                    bitBuff=(byte)((bitBuff<<1) | (c - '0'));
                    bitNr++;
                    bitsSize++;

                    if(bitNr==8){
                        fout.writeByte(bitBuff);
                        bitNr= 0;
                        bitBuff = 0;
                    }
                }
            }


        if (bitNr > 0) {
            bitBuff = (byte)(bitBuff << (8 - bitNr));
            fout.writeByte(bitBuff);
        }


        RandomAccessFile raf = new RandomAccessFile(outputFile, "rw");
        raf.seek(bitsSizePos);
        raf.writeLong(bitsSize);
        raf.close();
            System.out.println("Binary data written to output");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void getHuffmanCodeTable(String[] st, HufmanNode x, String s) {
        if (!x.isLeaf()) {
            getHuffmanCodeTable(st, x.left, s + '0');
            getHuffmanCodeTable(st, x.right, s + '1');
        } else {
            st[x.ch] = s;

            System.out.println("Code of " + x.ch + " is " + s);
        }
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
    if (args.length < 2) {
        System.out.println("Inncorect nr of args, expecting: <input-file> <output-file>");
        return;
    }

    encode(args[0],args[1]);
}

}