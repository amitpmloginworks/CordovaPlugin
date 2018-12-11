package com.gttronics.ble.blelibrary.dataexchanger.helpers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by EGUSI16 on 11/24/2016.
 */

public class DxAppHelper {

    public static <T> T[] concatenate (T[] a, T[] b) {
        int aLen = a.length;
        int bLen = b.length;

        @SuppressWarnings("unchecked")
        T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen+bLen);
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }

    public static byte[] toBytes(short s) {
        return new byte[]{(byte)(s & 0x00FF),(byte)((s & 0xFF00)>>8)};
    }

//    public static byte[] toBytes(short[] shorts) {
//        int index;
//        int iterations = shorts.length;
//
//        ByteBuffer bb = ByteBuffer.allocate(shorts.length * 2);
//
//        for(index = 0; index != iterations; ++index)
//        {
//            bb.putShort(shorts[index]);
//        }
//
//        return bb.array();
//    }

    public static byte[] toBytes(int integer) {
        byte[] bytes = ByteBuffer.allocate(4).putInt(integer).array();

//        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
//        byteBuffer.order(ByteOrder.BIG_ENDIAN);
//        byteBuffer.putInt(integer);
//        byte[] array = byteBuffer.array();

//        String hex = Integer.toHexString(integer);
//        byte[] array = hexStringToByteArray(hex);
        return bytes;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static byte[] toBytes(long l) {
        byte[] bytes = ByteBuffer.allocate(8).putLong(l).array();
        return bytes;
    }


    public static byte[] appendData(byte[] src, byte[] data) {
        byte[] tmp = new byte[src.length + data.length];
        System.arraycopy(src, 0, tmp, 0, src.length);
        System.arraycopy(data, 0, tmp, src.length, data.length);

        return tmp;
    }

    public static byte[] appendData(byte[] src, byte[] data, int length) {
        byte[] tmp = new byte[src.length + length];
        System.arraycopy(src, 0, tmp, 0, src.length);
        System.arraycopy(data, 0, tmp, src.length, length);

        return tmp;
    }

    public static String inputStreamToString(InputStream is) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        StringBuilder total = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            total.append(line).append('\n');
        }

        return total.toString();
    }

    public static byte[] readBytes(InputStream is) throws IOException {
        // this dynamically extends to take the bytes you read
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        // this is storage overwritten on each iteration with bytes
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        // we need to know how may bytes were read to write them to the byteBuffer
        int len = 0;
        while ((len = is.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        // and then we can return your byte array.
        return byteBuffer.toByteArray();
    }

    public static int sizeOf(Class className) {
        //TODO implement get object size
        try {
            Class<?> clazz = Class.forName(className.getName());
            Constructor<?> ctor = clazz.getConstructor();
            Object object = ctor.newInstance();
            byte[] bytes = serialize((Serializable) object);
            return bytes.length;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static int sizeOf(Serializable obj) {
        try {
            byte[] bytes = serialize(obj);
            return bytes.length;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static byte[] serialize(Serializable obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(bos);
        out.writeObject(obj);
        out.flush();
        return bos.toByteArray();
    }

    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes, 0 ,bytes.length);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return ois.readObject();
    }

    public static Object deserialize(byte[] bytes, int offset) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes, 0 , offset);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return ois.readObject();
    }


    public static int byteArrayToInt(byte[] bytes) {
        byte[] barr = {0, 0, 0, 0};
        int k = 3;
        for(int i = bytes.length - 1; i >=0; i--) {
            barr[k] = bytes[i];
            k--;
        }
        int tmp = ByteBuffer.wrap(barr).getInt();
        return tmp;
    }


    public static long byteArrayToUInt32(byte[] bytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream (bytes);
        DataInputStream dis = new DataInputStream (bais);
        int j = dis.readInt();
        return j & 0xFFFFFFFFL;
    }

    public static byte[] signedIntToUsignedByteArray(int i) {
        long uint = i & 0xffffffffL;
        return toBytes(uint);
    }

    public static <T> void removeFromSet(Set<T> set, int  index) {
        int i = 0;
        for (Iterator<T> it = set.iterator(); it.hasNext(); i++) {
            it.next();
            if (i == index) {
                it.remove();
                return;
            }
        }
    }

    public static <T> T getFromSet(Set<T> set, int  index) {
        int i = 0;
        for (Iterator<T> it = set.iterator(); it.hasNext(); i++) {
            T t = it.next();
            if (i == index) {
                it.remove();
                return t;
            }
        }

        return null;
    }

    public static int toUnsignedInt(byte x) {
        return x & 0xFF;
    }

    public static int toUnsignedInt(short s) {
        return s & 0xffff;
    }

    public static short[] byteArrayToShortArray(byte[] bytes) {
        short[] shorts = new short[bytes.length/2];
        // to turn bytes to shorts as either big endian or little endian.
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
        return shorts;
    }

    public static byte[] toBytes(short[] shorts) {
        ByteBuffer buffer = ByteBuffer.allocate(shorts.length * 2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.asShortBuffer().put(shorts);
        byte[] bytes = buffer.array();

        return bytes;
    }

    public static byte[] reverseArray(byte[] array) {

        int i = 0, j = array.length-1 ;
        while(i < j)
        {
            byte tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
            i++;
            j--;
        }
        return array;
    }

}
