import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;

public class DNSQuestion
{
    //This class represents a client request. It should have the following public methods:
    String[] qName ;
    byte[] qType ;
    byte[] qClass ;


    static DNSQuestion decodeQuestion(InputStream inputStream,DNSMessage message) throws IOException // read a question from the input stream. Due to compression, you may have to ask the DNSMessage containing this question to read some of the fields.
    {
        DNSQuestion question = new DNSQuestion() ;

        question.qName  = message.readDomainName(inputStream) ;
        question.qType  = inputStream.readNBytes(2);
        question.qClass = inputStream.readNBytes(2);

        return question ;
    }

    void writeBytes(ByteArrayOutputStream byteArrayOutputStream, HashMap<String,Integer> domainNameLocations) throws IOException//Write the question bytes which will be sent to the client. The hash map is used for us to compress the message, see the DNSMessage class below.
    {
        DNSMessage.writeDomainName(byteArrayOutputStream, domainNameLocations, qName);

        byteArrayOutputStream.write(qType);

        byteArrayOutputStream.write(qClass);

    }

    @Override
    public String toString() {
        return "DNSQuestion{" +
                "qName=" + Arrays.toString(qName) +
                ", qType=" + Arrays.toString(qType) +
                ", qClass=" + Arrays.toString(qClass) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DNSQuestion question = (DNSQuestion) o;
        return Arrays.equals(qName, question.qName) && Arrays.equals(qType, question.qType) && Arrays.equals(qClass, question.qClass);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(qName);
        result = 31 * result + Arrays.hashCode(qType);
        result = 31 * result + Arrays.hashCode(qClass);
        return result;
    }
}

