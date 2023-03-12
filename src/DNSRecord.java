import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.time.Duration;
import java.util.*;

public class DNSRecord {
    //Everything after the header and question parts of the DNS message are stored as records. This should have all the fields listed in the spec as well as a Date object storing when this record was created by your program. It should also have the following public methods:
    String[] rname;
    byte[] rnameInBytes;
    byte[] rtype;
    byte[] rclass;
    int rTTL;
    short rRDLength;
    byte[] rRData;


    public short offset;

    static Date date;
    static Calendar calendar = new GregorianCalendar();
    static long startTime;
    static long endTime;

    long differenceTTL ;


    private DNSRecord() {

    }

    static DNSRecord decodeRecord(InputStream inputStream, DNSMessage message) throws IOException {

        DNSRecord record = new DNSRecord();
        date = calendar.getTime();
        startTime = date.getTime();

        // Mark the inputStream so that we can return to this location
        inputStream.mark(2);

        // Need to check the first byte to see if the first 2 bits are set, which indicates compression
        byte[] checkCompression = inputStream.readNBytes(1);

        // If the first two bits are 1's, it means that the domain name is compressed.
        byte firstTwoBits = (byte) (checkCompression[0] >> 6);

        if ((firstTwoBits & (0x3)) == 0x3) {
            byte[] secondCompressionByte = inputStream.readNBytes(1) ;

            // If the first two bits are 1's, then the next 14 bits represent the location of the domain name in the message (the offset).
            byte firstSixBits = (byte) (checkCompression[0] << 2);
            firstSixBits = (byte) (firstSixBits >> 2);

            // Mask the first six bits together with the next eight bits
            record.offset = (short) ((short) firstSixBits << 8 | (secondCompressionByte[0] & (0xFF)));

            record.rname = message.readDomainName(record.offset);
        } else {
            // Return to the initial inputStream location
            inputStream.reset();

            record.rname = message.readDomainName(inputStream);
        }
        record.rtype = inputStream.readNBytes(2);
        record.rclass = inputStream.readNBytes(2);

        byte[] timeToLive = inputStream.readNBytes(4);
        record.rTTL = ((timeToLive[0] << 24) | ((timeToLive[1] & (0xFF0000)) | (timeToLive[2] & (0xFF00) | (timeToLive[3] & (0xFF)))));
        ;

        byte[] tempRDL = inputStream.readNBytes(2);
        record.rRDLength = (short) ((tempRDL[0] << 8) | (tempRDL[1] & (0xFF)));

        if (record.rRDLength > 0) {
            record.rRData = inputStream.readNBytes(record.rRDLength);
        }

        return record;

    }

    void writeBytes(ByteArrayOutputStream outputStream, HashMap<String, Integer> domainNameLocations) throws IOException {
        DNSMessage.writeDomainName(outputStream, domainNameLocations, rname);

        outputStream.write(rtype);

        outputStream.write(rclass);

        int ttl = (int) getCurrentTTL();
        outputStream.write(ttl >>> 24);
        outputStream.write(ttl >>> 16);
        outputStream.write(ttl >>> 8);
        outputStream.write(ttl);


        outputStream.write(shortToBytes(rRDLength));

        if (rRDLength > 0) {
            outputStream.write(rRData);
        }

    }


    long getCurrentTTL() {
        endTime = date.getTime();

        differenceTTL = endTime - startTime ;
        return differenceTTL ;
    }

    byte[] shortToBytes(short s)
    {

        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.putShort(s);
        return bb.array();
    }

    boolean timestampValid(){

        Date date1 = calendar.getTime();
        long currentTime1 = date1.getTime();

        long ExpirationTime = startTime + differenceTTL ;

        return ( currentTime1 < ExpirationTime) ;
    }

    @Override
    public String toString() {
        return "DNSRecord{" +
                "rname=" + Arrays.toString(rname) +
                ", rnameInBytes=" + Arrays.toString(rnameInBytes) +
                ", rtype=" + Arrays.toString(rtype) +
                ", rclass=" + Arrays.toString(rclass) +
                ", rTTL=" + rTTL +
                ", rRDLength=" + rRDLength +
                ", rRData=" + Arrays.toString(rRData) +
                ", offset=" + offset +
                ", differenceTTL=" + differenceTTL +
                '}';
    }
}

