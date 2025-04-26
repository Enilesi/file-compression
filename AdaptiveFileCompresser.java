import java.io.*;
import java.util.*;

public class AdaptiveFileCompresser {
    public static void main(String[] args) {

        if (args.length < 3) {
            System.out.println("usage: <mode(-c or -d)> <input-file> <output-file>");
            return;
        }

        long start = System.currentTimeMillis();

        if (args[0].equals("-c")) {
            Encode.encode(args[1], args[2]);
            long dur = System.currentTimeMillis() - start;
            report(args[1], args[2], dur, "Compression");

        } else if (args[0].equals("-d")) {
            Decode.decode(args[1], args[2]);
            long dur = System.currentTimeMillis() - start;
            report(args[1], args[2], dur, "Decompression");

        } else {

            throw new RuntimeException("Mode must be -c or -d");

        }
    }

    private static void report(String in, String out, long ms, String label) {

        File fIn  = new File(in);
        File fOut = new File(out);
        
        System.out.printf("%s Time: %d ms%n", label, ms);
        System.out.printf("Original Size: %d bytes%n", fIn.length());
        System.out.printf("Output   Size: %d bytes%n", fOut.length());
        System.out.printf("%s rate: %.2f%%%n", label, 100.0 * fOut.length() / fIn.length());
    }
}



class AdaptiveHuffmanTree {
    protected static final int R = 256;
    protected HufmanNode root, nyt;
    protected Map<Character,HufmanNode> charToNode = new HashMap<>();
    protected static final Map<Character,String> encodingMap = createEncodingMap();
    protected Map<Integer, List<HufmanNode>> weightToNodes = new HashMap<>();

    AdaptiveHuffmanTree() {
        int nextNum = 2*R + 1;
        nyt = new HufmanNode('\0', nextNum--, true);
        root = nyt;
        addNodeToWeightMap(nyt);
    }

    protected static class HufmanNode {
        char ch;
        int weight = 0, number;
        boolean isNyt;
        HufmanNode parent, left, right;
        HufmanNode(char c, int num, boolean nyt) {
            this.ch = c; this.number = num; this.isNyt = nyt;
        }
        boolean isLeaf() { return left == null && right == null; }
    }

    protected void addNewSymbol(char c) {
        HufmanNode oldNyt = nyt;
        oldNyt.isNyt = false;

        HufmanNode newNyt  = new HufmanNode('\0', oldNyt.number - 2, true);
        HufmanNode newNode = new HufmanNode(c,     oldNyt.number - 1, false);

        oldNyt.left  = newNyt;
        oldNyt.right = newNode;

        newNyt.parent  = oldNyt;
        newNode.parent = oldNyt;

        nyt = newNyt;

        charToNode.put(c, newNode);

        addNodeToWeightMap(newNyt);
        addNodeToWeightMap(newNode);

        updateTree(newNode);
    }

    protected void updateTree(HufmanNode node) {
        while (node != null) {
            HufmanNode leader = findBlockLeader(node);

            if (leader != null) {
                swapNodes(node, leader);
                node = leader;
            }

            removeNodeFromWeightMap(node);

            node.weight++;

            addNodeToWeightMap(node);

            node = node.parent;
        }
    }

    private HufmanNode findBlockLeader(HufmanNode node) {
        List<HufmanNode> block = weightToNodes.get(node.weight);

        if (block == null) return null;
        HufmanNode best = null;
        int maxNum = -1;

        for (HufmanNode n : block) {
            if (n == node || isAncestor(n, node) || isAncestor(node, n))
                continue;
            if (n.number > maxNum) {
                maxNum = n.number;
                best = n;
            }
        }
        return best;
    }

    private boolean swapNodes(HufmanNode a, HufmanNode b) {
        HufmanNode ap = a.parent, bp = b.parent;

        boolean aLeft = ap.left == a, bLeft = bp.left == b;

        if (aLeft) ap.left = b; else ap.right = b;
        if (bLeft) bp.left = a; else bp.right = a;

        a.parent = bp;
        b.parent = ap;

        int tmp = a.number;
        a.number = b.number;
        b.number = tmp;

        return true;
    }

    protected String getCode(HufmanNode node) {

        StringBuilder sb = new StringBuilder();

        Deque<Character> stack = new ArrayDeque<>();

        for (HufmanNode cur = node; cur.parent != null; cur = cur.parent) {
            stack.push(cur.parent.left == cur ? '0' : '1');
        }

        while (!stack.isEmpty())
            sb.append(stack.pop());

        return sb.toString();
    }

    private boolean isAncestor(HufmanNode anc, HufmanNode node) {

        for (HufmanNode cur = node; cur != null; cur = cur.parent)

            if (cur == anc) return true;

        return false;
    }

    private void addNodeToWeightMap(HufmanNode n) {
        List<HufmanNode> list = weightToNodes.get(n.weight);

        if (list == null) {
            list = new ArrayList<>();
            weightToNodes.put(n.weight, list);
        }

        list.add(n);
    }

    private void removeNodeFromWeightMap(HufmanNode n) {
        List<HufmanNode> lst = weightToNodes.get(n.weight);

        if (lst != null) {
            lst.remove(n);
            if (lst.isEmpty()) weightToNodes.remove(n.weight);
        }
    }

    private static Map<Character,String> createEncodingMap() {
        Map<Character,String> map = new HashMap<>();

        for (int i = 0; i < R; i++) {
            String bin = String.format("%8s", Integer.toBinaryString(i)).replace(' ', '0');
            map.put((char)i, bin);
        }


        return map;
    }
}



class Encode extends AdaptiveHuffmanTree {
    public static void encode(String inFile, String outFile) {


        Encode tree = new Encode();

        try (FileInputStream fin = new FileInputStream(inFile);
             FileOutputStream fos = new FileOutputStream(outFile);
             DataOutputStream dout = new DataOutputStream(fos)) {

            dout.writeLong(0L);

            BitOutputStream bitOut = new BitOutputStream(dout);

             int b;
            long totalBits = 0;

            while ((b = fin.read()) != -1) {

                char c = (char)b;

                if (tree.charToNode.containsKey(c)) {

                    String code = tree.getCode(tree.charToNode.get(c));

                    for (char bit : code.toCharArray()) {
                        bitOut.writeBit(bit == '1' ? 1 : 0);
                        totalBits++;
                    }

                    tree.updateTree(tree.charToNode.get(c));


                } else {
                    String nytCode = tree.getCode(tree.nyt);
                    String raw = encodingMap.get(c);


                    for (char bit : nytCode.toCharArray()) {
                        bitOut.writeBit(bit == '1' ? 1 : 0);
                        totalBits++;
                    }


                    for (char bit : raw.toCharArray()) {
                        bitOut.writeBit(bit == '1' ? 1 : 0);
                        totalBits++;
                    }

                    tree.addNewSymbol(c);
                }


            }

            bitOut.flush();

            try (RandomAccessFile raf = new RandomAccessFile(outFile, "rw")) {
                raf.seek(0);
                raf.writeLong(totalBits);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Decode extends AdaptiveHuffmanTree {

    public static void decode(String inFile, String outFile) {

        Decode tree = new Decode();

        try (DataInputStream din = new DataInputStream(new FileInputStream(inFile));

             FileOutputStream fout = new FileOutputStream(outFile)) {

            long totalBits = din.readLong();
            BitInputStream bitIn = new BitInputStream(din, totalBits);

            if (tree.root.isNyt) {

                int val = 0;

                for (int i = 0; i < 8; i++) {
                    int bit = bitIn.readBit();
                    if (bit < 0) break;
                    val = (val << 1) | bit;
                }

                fout.write(val);
                tree.addNewSymbol((char)val);
            }


            AdaptiveHuffmanTree.HufmanNode cur = tree.root;

            int bit;

            while ((bit = bitIn.readBit()) != -1) {

                cur = (bit == 0 ? cur.left : cur.right);

                if (cur.isLeaf()) {

                    if (cur.isNyt) {

                        int val = 0;

                        for (int i = 0; i < 8; i++) {
                            int b2 = bitIn.readBit();
                            if (b2 < 0) break;
                            val = (val << 1) | b2;
                        }


                        fout.write(val);
                        tree.addNewSymbol((char)val);

                    } else {

                        fout.write(cur.ch);
                        tree.updateTree(cur);

                    }

                    cur = tree.root;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class BitInputStream {
    private final InputStream in;
    private int currByte, bitsLeft;
    private long total;


    public BitInputStream(InputStream in, long totalBits) {
        this.in = in;
        this.total = totalBits;
        this.bitsLeft = 0;
    }


    public int readBit() throws IOException {
        if (total == 0) return -1;

        if (bitsLeft == 0) {

            currByte = in.read();

            if (currByte < 0) return -1;
            bitsLeft = 8;
        }

        bitsLeft--;
        total--;

        return (currByte >>> bitsLeft) & 1;
    }
}

class BitOutputStream {
    private final OutputStream out;
    private int currByte = 0, bitsFilled = 0;


    public BitOutputStream(OutputStream out) {
        this.out = out;
    }

    public void writeBit(int b) throws IOException {
        currByte = (currByte << 1) | b;

        bitsFilled++;

        if (bitsFilled == 8) {

            out.write(currByte);
            bitsFilled = 0;
            currByte = 0;
        }
    }


    public void flush() throws IOException {

        if (bitsFilled > 0) {

            currByte <<= (8 - bitsFilled);
            out.write(currByte);

            bitsFilled = 0;
            currByte = 0;
        }


        out.flush();
    }
}
