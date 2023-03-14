import java.io.IOException;
import java.net.*;


// NOTE : I have used cache <DNSQuestion, DNSMessage> instead of cache <DNSQuestion , DNSRecord>.
// I have also modified few functions accordingly

public class DNSServer
{
    // Port
    private static final int port = 8053 ; // Port No. to listen on
    private static final int portGoogle = 9000 ;

    // DatagramSockets
    DatagramSocket clientSocket;
    DatagramSocket googleSocket;

    public static byte[] receivedPacketFromClient = new byte[512];
    byte[] forwardData ; //packet sent to google
    byte[] receivedPacketFromGoogle;

    DatagramPacket clientPacket;
    DatagramPacket responsePacket;

    static DNSCache cache = new DNSCache() ;  // DNSCache object

    DNSMessage responseFromGoogle ;

    boolean isServerClosed = false ;


    void start() // starts the server and listens for incoming requests
    {
        clientPacket = new DatagramPacket(receivedPacketFromClient, receivedPacketFromClient.length);

        try {

            clientSocket = new DatagramSocket(port);
            googleSocket = new DatagramSocket(portGoogle);

            while (!isServerClosed) {
                clientSocket.receive(clientPacket);
                receivedPacketFromClient = clientPacket.getData(); // For debugging purpose

                DNSMessage new_message = DNSMessage.decodeMessage(receivedPacketFromClient);

                if ( ! cache.cachedData.containsKey(new_message.question)) {
                    responseFromGoogle = sendToGoogle();
                    System.out.println("Sent Query to Google");

                    cache.cachedData.put(responseFromGoogle.question, responseFromGoogle);
                }
                else {
                        System.out.println("From cache ");
                        cache.validQuery(new_message.question) ;
                        //cache.cachedData.get(new_message.question);
                    }


                DNSMessage responseMessage = DNSMessage.buildResponse(new_message, responseFromGoogle);
//                System.out.println(responseMessage);
                byte[] responsePacketInBytes = responseMessage.toBytes();


                // Want the response to be sent over the same port and to the same address as the initial query
                responsePacket = new DatagramPacket(responsePacketInBytes, responsePacketInBytes.length, clientPacket.getAddress(), clientPacket.getPort());
                clientSocket.send(responsePacket);
            }
        } catch (SocketException e) {
            System.err.println("Unable to open socket.");

            // close the socket
            isServerClosed = true;
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Unable to receive packet.");
            e.printStackTrace();
        }
    }
    DNSMessage sendToGoogle() throws IOException
    {
        // Forward query to google
        forwardData = receivedPacketFromClient;
        InetAddress googleAddress = InetAddress.getByName("8.8.8.8");
        DatagramPacket packetToGoogle = new DatagramPacket(forwardData, forwardData.length, googleAddress, 53);
        googleSocket.send(packetToGoogle);


        // Receive Google's DNS response and store in a message
        receivedPacketFromGoogle = new byte[512];

        DatagramPacket responseGooglePacket = new DatagramPacket(receivedPacketFromGoogle, receivedPacketFromGoogle.length);

        googleSocket.receive(responseGooglePacket);
        receivedPacketFromGoogle = responseGooglePacket.getData();
        responseFromGoogle = DNSMessage.decodeMessage(receivedPacketFromGoogle);

        return responseFromGoogle;
    }

    public static void main(String[] args)
    {
        DNSServer server = new DNSServer();
        server.start();
    }
}
