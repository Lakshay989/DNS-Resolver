import java.util.HashMap;

public class DNSCache {
    HashMap<DNSQuestion, DNSMessage> cachedData;

    public DNSCache() {
        cachedData = new HashMap<>();
    }

    DNSMessage validQuery(DNSQuestion key) {
        if ((cachedData.get(key) != null) && (!cachedData.get(key).answers.get(0).timestampValid())) {
            cachedData.remove(key);
            System.out.println(" Record does not exist");
            return null;
        }
        return cachedData.get(key);
    }
}
