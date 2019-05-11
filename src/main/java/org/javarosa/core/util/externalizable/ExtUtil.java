package org.javarosa.core.util.externalizable;

import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.util.Interner;
import org.javarosa.core.util.OrderedHashtable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class ExtUtil {
    private static final boolean interning = true;
    private static Interner<String> stringCache;

    public static byte[] serialize(Object o) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            write(new DataOutputStream(baos), o);
        } catch (IOException ioe) {
            throw new RuntimeException("IOException writing to ByteArrayOutputStream; shouldn't happen!");
        }
        return baos.toByteArray();
    }

    public static int getSize(Object o) {
        return serialize(o).length;
    }

    public static PrototypeFactory defaultPrototypes() {
        return PrototypeManager.getDefault();
    }

    public static void write(DataOutputStream out, Object data) throws IOException {
        if (data instanceof Externalizable) {
            ((Externalizable)data).writeExternal(out);
        } else if (data instanceof Byte) {
            writeNumeric(out, (Byte)data);
        } else if (data instanceof Short) {
            writeNumeric(out, (Short)data);
        } else if (data instanceof Integer) {
            writeNumeric(out, (Integer)data);
        } else if (data instanceof Long) {
            writeNumeric(out, (Long)data);
        } else if (data instanceof Character) {
            writeChar(out, (Character)data);
        } else if (data instanceof Float) {
            writeDecimal(out, (Float)data);
        } else if (data instanceof Double) {
            writeDecimal(out, (Double)data);
        } else if (data instanceof Boolean) {
            writeBool(out, (Boolean)data);
        } else if (data instanceof String) {
            writeString(out, (String)data);
        } else if (data instanceof Date) {
            writeDate(out, (Date)data);
        } else if (data instanceof byte[]) {
            writeBytes(out, (byte[])data);
        } else {
            throw new ClassCastException("Not a serializable datatype: " + data.getClass().getName());
        }
    }

    public static void writeNumeric(DataOutputStream out, long val) throws IOException {
        writeNumeric(out, val, new ExtWrapIntEncodingUniform());
    }

    public static void writeNumeric(DataOutputStream out, long val, ExtWrapIntEncoding encoding) throws IOException {
        write(out, encoding.clone(val));
    }

    public static void writeChar(DataOutputStream out, char val) throws IOException {
        out.writeChar(val);
    }

    public static void writeDecimal(DataOutputStream out, double val) throws IOException {
        out.writeDouble(val);
    }

    public static void writeBool(DataOutputStream out, boolean val) throws IOException {
        out.writeBoolean(val);
    }

    public static void writeString(DataOutputStream out, String val) throws IOException {
        try {
            out.writeUTF(val);
        } catch (UTFDataFormatException e) {
            int percentOversized = ((val.getBytes("UTF-8").length / (((int)Short.MAX_VALUE) * 2)) - 1) * 100;
            throw new SerializationLimitationException(percentOversized);
        }
        //we could easily come up with more efficient default encoding for string
    }

    public static void writeDate(DataOutputStream out, Date val) throws IOException {
        writeNumeric(out, val.getTime());
        //time zone?
    }

    public static void writeBytes(DataOutputStream out, byte[] bytes) throws IOException {
        ExtUtil.writeNumeric(out, bytes.length);
        if (bytes.length > 0) //i think writing zero-length array might close the stream
            out.write(bytes);
    }

    public static Object read(DataInputStream in, Class type, PrototypeFactory pf) throws IOException, DeserializationException {
        if (Externalizable.class.isAssignableFrom(type)) {
            Externalizable ext = (Externalizable)PrototypeFactory.getInstance(type);
            ext.readExternal(in, pf == null ? defaultPrototypes() : pf);
            return ext;
        } else if (type == Byte.class) {
            return readByte(in);
        } else if (type == Short.class) {
            return readShort(in);
        } else if (type == Integer.class) {
            return readInt(in);
        } else if (type == Long.class) {
            return readNumeric(in);
        } else if (type == Character.class) {
            return readChar(in);
        } else if (type == Float.class) {
            return (float)readDecimal(in);
        } else if (type == Double.class) {
            return readDecimal(in);
        } else if (type == Boolean.class) {
            return readBool(in);
        } else if (type == String.class) {
            return readString(in);
        } else if (type == Date.class) {
            return readDate(in);
        } else if (type == byte[].class) {
            return readBytes(in);
        } else {
            throw new ClassCastException("Not a deserializable datatype: " + type.getName());
        }
    }

    public static Object read(DataInputStream in, ExternalizableWrapper ew, PrototypeFactory pf) throws IOException, DeserializationException {
        ew.readExternal(in, pf == null ? defaultPrototypes() : pf);
        return ew.val;
    }

    public static long readNumeric(DataInputStream in) throws IOException {
        return readNumeric(in, new ExtWrapIntEncodingUniform());
    }

    public static long readNumeric(DataInputStream in, ExtWrapIntEncoding encoding) throws IOException {
        try {
            return (Long)read(in, encoding, null);
        } catch (DeserializationException de) {
            throw new RuntimeException("Shouldn't happen: Base-type encoding wrappers should never touch prototypes");
        }
    }

    public static int readInt(DataInputStream in) throws IOException {
        return toInt(readNumeric(in));
    }

    public static long readLong(DataInputStream in) throws IOException {
        return readNumeric(in);
    }

    public static short readShort(DataInputStream in) throws IOException {
        return toShort(readNumeric(in));
    }

    public static byte readByte(DataInputStream in) throws IOException {
        return toByte(readNumeric(in));
    }

    public static char readChar(DataInputStream in) throws IOException {
        return in.readChar();
    }

    public static double readDecimal(DataInputStream in) throws IOException {
        return in.readDouble();
    }

    public static boolean readBool(DataInputStream in) throws IOException {
        return in.readBoolean();
    }

    public static String readString(DataInputStream in) throws IOException {
        String s = in.readUTF();
        return (interning && stringCache != null) ? stringCache.intern(s) : s;
    }

    public static Date readDate(DataInputStream in) throws IOException {
        return new Date(readNumeric(in));
        //time zone?
    }

    public static byte[] readBytes(DataInputStream in) throws IOException {
        int size = (int)ExtUtil.readNumeric(in);
        byte[] bytes = new byte[size];
        int read = 0;
        int toread = size;
        while (read != size) {
            read = in.read(bytes, 0, toread);
            toread -= read;
        }
        return bytes;
    }

    public static int toInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE)
            throw new ArithmeticException("Value (" + l + ") cannot fit into int");
        return (int)l;
    }

    public static short toShort(long l) {
        if (l < Short.MIN_VALUE || l > Short.MAX_VALUE)
            throw new ArithmeticException("Value (" + l + ") cannot fit into short");
        return (short)l;
    }

    public static byte toByte(long l) {
        if (l < Byte.MIN_VALUE || l > Byte.MAX_VALUE)
            throw new ArithmeticException("Value (" + l + ") cannot fit into byte");
        return (byte)l;
    }

    public static long toLong(Object o) {
        if (o instanceof Byte) {
            return (Byte)o;
        } else if (o instanceof Short) {
            return (Short)o;
        } else if (o instanceof Integer) {
            return (Integer)o;
        } else if (o instanceof Long) {
            return (Long)o;
        } else if (o instanceof Character) {
            return (Character)o;
        } else {
            throw new ClassCastException();
        }
    }

    public static byte[] nullIfEmpty(byte[] ba) {
        return (ba == null ? null : (ba.length == 0 ? null : ba));
    }

    public static String nullIfEmpty(String s) {
        return (s == null ? null : (s.length() == 0 ? null : s));
    }

    public static Vector nullIfEmpty(Vector v) {
        return (v == null ? null : (v.size() == 0 ? null : v));
    }

    public static Hashtable nullIfEmpty(Hashtable h) {
        return (h == null ? null : (h.size() == 0 ? null : h));
    }

    public static byte[] emptyIfNull(byte[] ba) {
        return ba == null ? new byte[0] : ba;
    }

    public static String emptyIfNull(String s) {
        return s == null ? "" : s;
    }

    public static Vector emptyIfNull(Vector v) {
        return v == null ? new Vector() : v;
    }

    public static Hashtable emptyIfNull(Hashtable h) {
        return h == null ? new Hashtable() : h;
    }

    public static Object unwrap(Object o) {
        return (o instanceof ExternalizableWrapper ? ((ExternalizableWrapper)o).baseValue() : o);
    }

    public static boolean equals(Object a, Object b, boolean unwrap) {
        if (unwrap) {
            a = unwrap(a);
            b = unwrap(b);
        }

        if (a == null) {
            return b == null;
        } else {
            if (unwrap) {
                if (a instanceof Vector) {
                    return (b instanceof Vector && vectorEquals((Vector)a, (Vector)b, unwrap));
                } else if (a instanceof Hashtable) {
                    return (b instanceof Hashtable && hashtableEquals((Hashtable)a, (Hashtable)b, unwrap));
                }
            }
            return a.equals(b);
        }
    }

    public static boolean vectorEquals(Vector a, Vector b, boolean unwrap) {
        if (a.size() != b.size()) {
            return false;
        } else {
            for (int i = 0; i < a.size(); i++) {
                if (!equals(a.elementAt(i), b.elementAt(i), unwrap)) {
                    return false;
                }
            }

            return true;
        }
    }

    public static boolean arrayEquals(Object[] a, Object[] b, boolean unwrap) {
        if (a.length != b.length) {
            return false;
        } else {
            for (int i = 0; i < a.length; i++) {
                if (!equals(a[i], b[i], unwrap)) {
                    return false;
                }
            }

            return true;
        }
    }

    public static boolean hashtableEquals(Hashtable a, Hashtable b, boolean unwrap) {
        if (a.size() != b.size()) {
            return false;
        } else if (a instanceof OrderedHashtable != b instanceof OrderedHashtable) {
            return false;
        } else {
            for (Enumeration ea = a.keys(); ea.hasMoreElements(); ) {
                Object keyA = ea.nextElement();

                if (!equals(a.get(keyA), b.get(keyA), unwrap)) {
                    return false;
                }
            }

            if (a instanceof OrderedHashtable && b instanceof OrderedHashtable) {
                Enumeration ea = a.keys();
                Enumeration eb = b.keys();

                while (ea.hasMoreElements()) {
                    Object keyA = ea.nextElement();
                    Object keyB = eb.nextElement();

                    if (!keyA.equals(keyB)) { //must use built-in equals for keys, as that's what hashtable uses
                        return false;
                    }
                }
            }

            return true;
        }
    }

    public static String printBytes(byte[] data) {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (int i = 0; i < data.length; i++) {
            String hex = Integer.toHexString(data[i]);
            if (hex.length() == 1)
                hex = "0" + hex;
            else
                hex = hex.substring(hex.length() - 2);
            sb.append(hex);
            if (i < data.length - 1) {
                if ((i + 1) % 30 == 0)
                    sb.append("\n ");
                else if ((i + 1) % 10 == 0)
                    sb.append("  ");
                else
                    sb.append(" ");
            }
        }
        sb.append("]");
        return sb.toString();
    }


    //**REMOVE THIS FUNCTION//
    //original deserialization API (whose limits made us make this whole new framework!); here for backwards compatibility
    public static Object deserialize(byte[] data, Class type, PrototypeFactory pf) throws IOException, DeserializationException {
        return read(new DataInputStream(new ByteArrayInputStream(data)), type, pf);
    }
    ////

    @SuppressWarnings("unused")
    public static void attachCacheTable(Interner<String> stringCache) {
        ExtUtil.stringCache = stringCache;
    }
}
