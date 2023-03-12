import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class DNSHeader {
    short ID;
    byte QR;
    byte Opcode;
    byte AA;
    byte TC;
    byte RD;
    byte RA;
    byte Z;
    byte RCODE;

    byte[] QDCOUNT;
    byte[] ANCOUNT;
    byte[] NSCOUNT;
    byte[] ARCOUNT;

    short ANCountShort;
    short NSCountShort;
    short ARCountShort;

    byte[] thirdAndFourthHeaderBytes;

    byte[] HeaderData ;

    public DNSHeader()
    {

    }

    static DNSHeader decodeHeader(ByteArrayInputStream inputStream) throws IOException//read the header from an input stream (we'll use a ByteArrayInputStream but we will only use the basic read methods of input stream to read 1 byte, or to fill in a byte array, so we'll be generic).
    {
        DNSHeader header = new DNSHeader();

        byte[] tempID = inputStream.readNBytes(2);
        header.ID = (short) ((tempID[0] << 8) | (tempID[1] & (0xFF)));

        header.thirdAndFourthHeaderBytes = inputStream.readNBytes(2);

        byte thirdHeaderByte = header.thirdAndFourthHeaderBytes[0];

        header.QR = (byte) (thirdHeaderByte >> 7 & 0x1);


        header.Opcode = (byte) (thirdHeaderByte << 1);
        header.Opcode = (byte) (header.Opcode >> 4);

        header.AA = (byte) (thirdHeaderByte << 5);
        header.AA = (byte) (header.AA >> 7);

        header.TC = (byte) (thirdHeaderByte << 6);
        header.TC = (byte) (header.TC >> 7);

        header.RD = (byte) (thirdHeaderByte << 7);
        header.RD = (byte) (header.RD >> 7);


        byte fourthHeaderByte = header.thirdAndFourthHeaderBytes[1];

        header.RA = (byte) (fourthHeaderByte >> 7);

        header.Z = (byte) (fourthHeaderByte << 1);
        header.Z = (byte) (header.Z >> 5);

        header.RCODE = (byte) (fourthHeaderByte << 4);
        header.RCODE = (byte) (header.RCODE >> 4);

        header.QDCOUNT = inputStream.readNBytes(2);


        header.ANCOUNT = inputStream.readNBytes(2);
        header.ANCountShort = (short) ((header.ANCOUNT[0] << 8) | (header.ANCOUNT[1] & (0xFF)));

        header.NSCOUNT = inputStream.readNBytes(2);
        header.NSCountShort = (short) ((header.NSCOUNT[0] << 8) | (header.NSCOUNT[1] & (0xFF)));

        header.ARCOUNT = inputStream.readNBytes(2);
        header.ARCountShort = (short) ((header.ARCOUNT[0] << 8) | (header.ARCOUNT[1] & (0xFF)));

        return header;

    }
       static DNSHeader buildHeaderForResponse(DNSMessage request, DNSMessage response) // This will create the header for the response. It will copy some fields from the request
       {
           DNSHeader responseHeader = new DNSHeader() ;

           responseHeader.ID = request.header.ID;

           // Remaining header components should match Google's response header
           responseHeader.thirdAndFourthHeaderBytes = response.header.thirdAndFourthHeaderBytes;

           responseHeader.QDCOUNT = response.header.QDCOUNT;

           responseHeader.RA = response.header.RA;

           responseHeader.Z = response.header.Z ;
           responseHeader.ANCountShort= response.header.ANCountShort;
           responseHeader.NSCountShort = response.header.NSCountShort;
           responseHeader.ARCountShort = response.header.ARCountShort;

           return responseHeader ;

       }

    void writeBytes(OutputStream outputStream) throws IOException //encode the header to bytes to be sent back to the client. The OutputStream interface has methods to write a single byte or an array of bytes.
    {
        outputStream.write(shortToBytes(ID));
        outputStream.write(thirdAndFourthHeaderBytes);
        outputStream.write(QDCOUNT);

        // ANCOUNT will never be greater than 1 for our purposes, since we only want to send back 1 answer
        if (ANCountShort > (short)1){
            ANCountShort = 1;
        }

        outputStream.write(shortToBytes(ANCountShort));

        outputStream.write(shortToBytes(NSCountShort));

        outputStream.write(shortToBytes(ARCountShort));
    }

    byte[] shortToBytes(short s){

        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.putShort(s);
        return bb.array();
    }

    @Override
    public String toString() {
        return "DNSHeader{" +
                "ID=" + ID +
                ", QR=" + QR +
                ", Opcode=" + Opcode +
                ", AA=" + AA +
                ", TC=" + TC +
                ", RD=" + RD +
                ", RA=" + RA +
                ", Z=" + Z +
                ", RCODE=" + RCODE +
                ", QDCOUNT=" + Arrays.toString(QDCOUNT) +
                ", ANCOUNT=" + Arrays.toString(ANCOUNT) +
                ", NSCOUNT=" + Arrays.toString(NSCOUNT) +
                ", ARCOUNT=" + Arrays.toString(ARCOUNT) +
                ", ANCountShort=" + ANCountShort +
                ", NSCountShort=" + NSCountShort +
                ", ARCountShort=" + ARCountShort +
                ", thirdAndFourthHeaderBytes=" + Arrays.toString(thirdAndFourthHeaderBytes) +
                ", HeaderData=" + Arrays.toString(HeaderData) +
                '}';
    }
}
