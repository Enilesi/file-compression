Commands: 
javac FileCompresser.java 
java FileCompresser -c fin.txt fout5.txt
java FileCompresser -d fout5.txt fout6.txt

javac AdaptiveFileCompresser.java 
java AdaptiveFileCompresser -c fin.txt fout5.txt 
java AdaptiveFileCompresser -d fout5.txt fout6.txt

Results:

For static Huffman:

File encoded! (in binary) Operation took: 252.479966 ms Original size: 39740 bytes Compressed size: 22200 bytes Compression rate: 55.86%

File decoded! Operation took: 221.330306 ms Compressed file size: 22200 bytes Decompressed file size: 39740 bytes

For adaptive Huffman:

Compression Time: 299 ms Original Size: 39740 bytes Output Size: 24506 bytes Compression rate: 61.67%

Decompression Time: 328 ms Original Size: 24506 bytes Output Size: 39740 bytes Decompression rate: 162.16%

