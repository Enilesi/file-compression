import java.io.*;
import java.util.*;


public class FileCompresser{

    protected static int R = 256;


    protected static class HufmanNode implements Comparable<HufmanNode> {
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

    protected static HufmanNode buildHufmanTree(HashMap<Byte, Integer> freq) {
  
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
        if (args.length < 3) {
            System.out.println("Incorrect nr of args, expecting: <mode(-c or -d)> <input-file> <output-file>");
            return;
        }
    
        String mode = args[0];
        String inputFile = args[1];
        String outputFile = args[2];
    
        long startTime = System.nanoTime();
    
        if (mode.equals("-c")) {
            Encode.encode(inputFile, outputFile);
        } else if (mode.equals("-d")) {
            Decode.decode(inputFile, outputFile);
        } else {
            throw new RuntimeException("Incorrect mode, expecting: <mode(-c or -d)>");
        }
    
        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;
        System.out.println("Operation took: " + durationMs + " ms");
    
        if (mode.equals("-c")) {
            File in = new File(inputFile);
            File out = new File(outputFile);
            double compressionRate = (100.0 * out.length()) / in.length();
            System.out.println("Original size: " + in.length() + " bytes");
            System.out.println("Compressed size: " + out.length() + " bytes");
            System.out.printf("Compression rate: %.2f%%\n", compressionRate);
        }

        else if (mode.equals("-d")) {
            File in = new File(inputFile);
            File out = new File(outputFile);
            System.out.println("Compressed file size: " + in.length() + " bytes");
            System.out.println("Decompressed file size: " + out.length() + " bytes");
        } else {
        throw new RuntimeException("Incorrect mode, expecting: <mode(-c or -d)>");
    }
        
    }

    


    

    
}

class Encode extends FileCompresser{
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

            long fileSize = 0;
            long fileSizePos = fout.size(); 
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
                    fileSize++;

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
        raf.seek(fileSizePos);
        raf.writeLong(fileSize);
        raf.close();
            System.out.println("File encoded! (in binary)");
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
}

class Decode extends FileCompresser {

    public static void decode(String inputFile, String outputFile) {
        HashMap<Byte, Integer> freq = new HashMap<>();

        try (FileInputStream fin = new FileInputStream(inputFile);
             DataInputStream din = new DataInputStream(new FileInputStream(inputFile));
             FileOutputStream fout = new FileOutputStream(outputFile)) {

            int freqSize = din.readInt();

            for (int i = 0; i < freqSize; i++) {
                byte byteVal = (byte) din.readChar();
                int count = din.readInt();
                freq.put(byteVal, count);
            }


            long fileSize = din.readLong();

            HufmanNode root = buildHufmanTree(freq);

            HufmanNode current = root;


            int bitBuff = 0;
            int bitNr = 0;


            for (long fr = 0; fr < fileSize; fr++) {
                if (bitNr == 0) {
                    int nextByte = din.read();
                    if (nextByte == -1) break;
                    bitBuff = nextByte;
                    bitNr = 8;
                }
            
                int bit = (bitBuff >> (bitNr - 1)) & 1;
                bitNr--;
            
                current = (bit == 0) ? current.left : current.right;
            
                if (current.isLeaf()) {
                    fout.write((byte) current.ch);
                    current = root;
                }
            }
            

            System.out.println("File decoded!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

