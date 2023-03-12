import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class DNSMessage
{
    ByteArrayInputStream inputStream ;
    byte[] messageInBytes ;
    DNSHeader header = new DNSHeader();
    DNSQuestion question ;
    ArrayList <DNSRecord> answers  = new ArrayList<>();

    ArrayList <DNSRecord> authorityRecords = new ArrayList<>();

    ArrayList <DNSRecord> additionalRecords  = new ArrayList<>();


    private DNSMessage()
    {

    }

    static DNSMessage decodeMessage(byte[] bytes) throws IOException {

        DNSMessage message = new DNSMessage();


        message.inputStream = new ByteArrayInputStream(bytes) ;

        message.messageInBytes = bytes ;

        message.header = DNSHeader.decodeHeader(message.inputStream) ;

        message.question  = DNSQuestion.decodeQuestion(message.inputStream, message) ;

        for (int i=0; i<message.header.ANCountShort; i++){
            message.answers.add(DNSRecord.decodeRecord(message.inputStream, message));
        }

        for (int i=0; i<message.header.NSCountShort; i++){
            message.authorityRecords.add(DNSRecord.decodeRecord(message.inputStream, message));
        }

        for (int i=0; i<message.header.ARCountShort; i++){
            message.additionalRecords.add(DNSRecord.decodeRecord(message.inputStream, message));
        }

        return message ;
    }

    String[] readDomainName(InputStream inputStream) throws IOException  //read the pieces of a domain name starting from the current position of the input stream
    {
        ArrayList<byte[]> labels = new ArrayList<>() ;

        byte[] length = inputStream.readNBytes(1);

        while (length[0] != 0)
        {
            byte[] label = inputStream.readNBytes(length[0]) ;
            labels.add(label) ;

            length = inputStream.readNBytes(1);
        }

        String[] labelsAsStrings = new  String[labels.size()] ;

        for( int i = 0 ; i < labels.size() ; i++)
        {
            String toAdd = new String(labels.get(i)) ;
            labelsAsStrings[i] = toAdd ;
        }
        return labelsAsStrings ;

    }

    String[] readDomainName(int firstByte) throws IOException //same, but used when there's compression and we need to find the domain from earlier in the message. This method should make a ByteArrayInputStream that starts at the specified byte and call the other version of this method
    {
        ByteArrayInputStream forReadDomainNameCompression = new ByteArrayInputStream(messageInBytes);

        // Read up until the indicated byte to get to the correct location (but we don't actually use these bytes for anything)
        byte [] toDiscard = forReadDomainNameCompression.readNBytes(firstByte);

        // Call on the other version of readDomainName
        String[] domainName = readDomainName(forReadDomainNameCompression);

        return domainName;
    }

    static DNSMessage buildResponse(DNSMessage request, DNSMessage responseFromGoogle) //build a response based on the request and the answers you intend to send back.
    {
        DNSMessage responseMessage = new DNSMessage() ;
        responseMessage.header = DNSHeader.buildHeaderForResponse(request, responseFromGoogle) ;

        responseMessage.question = request.question;
        responseMessage.answers = responseFromGoogle.answers ;
        responseMessage.authorityRecords = request.authorityRecords ;
        responseMessage.additionalRecords = request.additionalRecords ;

        // For Debugging

//        System.out.println(request);
//        System.out.println(responseFromGoogle);
//        System.out.println(responseMessage);

        return responseMessage ;
    }

    byte[] toBytes() throws IOException
    {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        header.writeBytes(outputStream);

        // Create a hashmap to keep track of where domain names are located within the message
        HashMap<String,Integer> domainNameLocations = new HashMap<>();

        question.writeBytes(outputStream, domainNameLocations);

        // For our purposes, we are only writing the first answer back to the client.
        if (answers.size() > 0){
            answers.get(0).writeBytes(outputStream, domainNameLocations);
        }

        // If there are any authority records, we want to write them back to the client.
        for (DNSRecord r : authorityRecords){
            r.writeBytes(outputStream, domainNameLocations);
        }

        // If there are any additional records, we want to write them back to the client.
        for (DNSRecord r: additionalRecords){
            r.writeBytes(outputStream, domainNameLocations);
        }
        // Creates a byte[] from the output stream we created.
        return outputStream.toByteArray();
    }

    static void writeDomainName(ByteArrayOutputStream byteArrayOutputStream, HashMap<String,Integer> domainLocations, String[] domainNamePieces) throws IOException {

        // Get the full domain name, including dots
        String domainNameString = joinDomainName(domainNamePieces);

        // If this name is not in the hashmap, we need to write it using encoding and add it to the hashmap.
        if (domainLocations.get(domainNameString) == null) {

            // out.size tells us how many we've read in so far, which keeps track of the domain name's location in the message
            domainLocations.put(domainNameString, byteArrayOutputStream.size());

            for (String s : domainNamePieces) {
                int length = s.length();

                // DNS encoding: write the length of the domain name segment followed by the segment
                byteArrayOutputStream.write(((byte) length));
                byteArrayOutputStream.write(s.getBytes());
            }
            // Terminate domain name with a length of zero
            byteArrayOutputStream.write((byte) 0);
        }

        // If this name is in the hashmap, it has been seen already and we need to write a pointer to its location.
        else {

            // Get the domain name's location in the stream (called the offset) from the hashmap
            int offset = domainLocations.get(domainNameString);

            // Indicate compression with two "1" bits
            int indicateCompression = 0xC000;

            // Represents 2 bytes with 2 "1" bits and the offset
            int compressedDomainName = (offset | indicateCompression);

            // Need to write one byte at a time (writing an int in a byteArrayStream will only write the last 8 bits)
            int writeFirstByte = compressedDomainName >> 8;

            byteArrayOutputStream.write(writeFirstByte);

            // Writes the last 8 bits from the original int
            byteArrayOutputStream.write(compressedDomainName);

        }
    }

    static String joinDomainName(String[] pieces)  //join the pieces of a domain name with dots ([ "utah", "edu"] -> "utah.edu" )
    {
        String domainNameWithDots ;

        if(pieces.length == 0)
        {
            domainNameWithDots = "";
        }
        else
        {
            domainNameWithDots = pieces[0] ;
        }
        for(int i = 1 ; i < pieces.length ; i++)
        {
            domainNameWithDots += "." + pieces[i] ;
        }
        return domainNameWithDots ;

    }

    @Override
    public String toString() {
        return "DNSMessage{" +
                "inputStream=" + inputStream +
                ", messageInBytes=" + Arrays.toString(messageInBytes) +
                ", header=" + header +
                ", question=" + question +
                ", answers=" + answers +
                ", authorityRecords=" + authorityRecords +
                ", additionalRecords=" + additionalRecords +
                '}';
    }
}