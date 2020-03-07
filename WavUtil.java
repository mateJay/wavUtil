package cn.bluepulse.caption.db.util;

import java.io.*;

public class WavUtil {
    /**
     *
     * @param srcPath 源文件路径
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param destPath 处理后的文件路径
     * @return
     * @throws IOException
     */
    public static boolean genNewWav(String srcPath, Integer startTime, Integer endTime, String destPath) throws IOException {

        byte[] songBytes = getSongBytes(srcPath, startTime, endTime);
        if (songBytes == null) {
            return false;
        }

        FileOutputStream outputFile = new FileOutputStream(destPath);

        // data数据
        outputFile.write(songBytes);

        return true;
    }

    /**
     * 获取截取歌曲信息后的byte信息
     * @param srcPath
     * @param startTime
     * @param endTime
     * @return
     */
    public static byte[] getSongBytes(String srcPath, Integer startTime, Integer endTime) throws IOException {
        // 不会变动的头部分，subChunk2Size前的长度
        int headLength;

        RandomAccessFile inputWav = new RandomAccessFile(srcPath, "r");

        // 每秒数据字节数
        byte[] byteRateArr = new byte[4];
        long byteRateIndex = 28;
        inputWav.seek(byteRateIndex);
        inputWav.read(byteRateArr);
        int byteRate = byteArr2Int(byteRateArr);
        //System.out.println("每秒数据字节数：" + byteRate);

        // 类型
        byte[] type = new byte[4];
        long typeRateIndex = 36;
        inputWav.seek(typeRateIndex);
        inputWav.read(type);

        int dataLength;
        if (isData(type)) {
            // data数据大小
            byte[] dataLengthArr = new byte[4];
            inputWav.seek(40);
            inputWav.read(dataLengthArr);
            dataLength = byteArr2Int(dataLengthArr);

            headLength = 40;
        } else if (isLIST(type)) {
            // 后面紧跟着有段数据（记录格式转换的一些信息）的长度
            byte[] convertByteLengthArr = new byte[4];
            inputWav.seek(40);
            inputWav.read(convertByteLengthArr);
            int convertByteLength = byteArr2Int(convertByteLengthArr);

            // data数据大小
            byte[] dataLengthArr = new byte[4];
            inputWav.seek(40 + 4 + convertByteLength + 4);
            inputWav.read(dataLengthArr);
            dataLength = byteArr2Int(dataLengthArr);

            headLength = 40 + 4 + convertByteLength + 4;
        } else {
            return null;
        }

        // 音频多少秒
        int seconds = Math.round((float) dataLength / byteRate);
        //System.out.println("音频多少秒：" + seconds);
        if (endTime > seconds) {
            return null;
        }

        // 头信息
        byte[] headByteArr = new byte[headLength];
        inputWav.seek(0);
        inputWav.read(headByteArr);

        // 新的data数据大小
        int newDataLength = (endTime - startTime) * byteRate;
        byte[] newDataLengthArr = int2ByteArr(newDataLength);

        // 实际歌曲数据长度
        int realDataLength = (endTime - startTime) * byteRate;
        // data数据开始读的位置
        int dataReadIndex = startTime * byteRate + headLength + 4;
        // 截取数据部分
        byte[] dataContent = new byte[realDataLength];
        inputWav.seek(dataReadIndex);
        inputWav.read(dataContent);

        byte[] songBytes = new byte[headByteArr.length + newDataLengthArr.length + dataContent.length];
        System.arraycopy(headByteArr, 0, songBytes, 0, headByteArr.length);
        System.arraycopy(newDataLengthArr, 0, songBytes, headByteArr.length, newDataLengthArr.length);
        System.arraycopy(dataContent, 0, songBytes, headByteArr.length + newDataLengthArr.length, dataContent.length);

        return songBytes;
    }

    private static int byteArr2Int(byte[] byteArr) {

        int byte1 = byteArr[0] & 0xff;
        int byte2 = (byteArr[1] & 0xff) << 8;
        int byte3 = (byteArr[2] & 0xff) << 16;
        int byte4 = (byteArr[3] & 0xff) << 24;

        return byte1|byte2|byte3|byte4;
    }

    private static byte[] int2ByteArr(int integer)
    {
        byte[] bytes=new byte[4];
        bytes[3]=(byte) (integer >> 24);
        bytes[2]=(byte) (integer >> 16);
        bytes[1]=(byte) (integer >> 8);
        bytes[0]=(byte) integer;

        return bytes;
    }

    /**
     * 判断是否是原wav（没有经过转换的）
     * @param byteArr
     * @return
     */
    private static boolean isData(byte[] byteArr) {
        if (byteArr.length != 4) {
            return false;
        }
        if (byteArr[0] == 100 && byteArr[1] == 97 && byteArr[2] == 116 && byteArr[3] == 97) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 判断是否经过转换
     * @param byteArr
     * @return
     */
    private static boolean isLIST(byte[] byteArr) {
        if (byteArr.length != 4) {
            return false;
        }
        if (byteArr[0] == 76 && byteArr[1] == 73 && byteArr[2] == 83 && byteArr[3] == 84) {
            return true;
        } else {
            return false;
        }
    }

    // 400M,随机截取三分钟，46毫秒，十分钟，98毫秒  120个线程，几十M内存，200毫秒到1600毫秒，20个线程内存忽略不计，200毫秒左右。线程越多，浪费在线程调度上的时间越多
    public static void main(String[] args) throws IOException {
//        for (int i = 0; i < 120; i++) {
//            new Thread(() -> {
//                try {
//                    long startTime = System.currentTimeMillis();
//                    genNewWav("/Users/apple/workspace/测试文件/3h30min.wav", 3600, 3780, "/Users/apple/workspace/测试文件/3h30min3600-3780-my-" + UUID.randomUUID() + ".wav");
//                    long endTime = System.currentTimeMillis();
//                    System.out.println("花费时长：" + (endTime - startTime));
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }).start();
//        }

        long startTime = System.currentTimeMillis();
        genNewWav("/Users/apple/workspace/测试文件/翻唱—夏风.wav", 80, 100, "/Users/apple/workspace/测试文件/翻唱—夏风80-100.wav");
        long endTime = System.currentTimeMillis();
        System.out.println("花费时长：" + (endTime - startTime));
    }
}