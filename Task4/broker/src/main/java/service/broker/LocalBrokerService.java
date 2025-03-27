package service.broker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import service.core.ClientInfo;
import service.core.Offer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

@RestController
public class LocalBrokerService {
    private Map<String, Offer> offers = new TreeMap<>;

    @Value("${server.port}")
    private int serverPort;

    @GetMapping(value="offers", produces="application/json")
    public ResponseEntity<ArrayList<String>> getOffers() {
        // like quotation controllers, return a list of urls
        // that reference offer resources
        ArrayList<String> urls = new ArrayList<>();
        for (Offer offer : offers.values()) {
            urls.add("http://" + getHost() + "/offers/" + offer.id);
        }

        return ResponseEntity.status(HttpStatus.OK).body(urls);
    }

    @PostMapping(value="offers", consumes="application/json")
    public ResponseEntity<Offer> createOffer(@RequestBody ClientInfo info) {}

    // utility method to get localhost address
    private String getHost() {
        try {
            InetAddress host = InetAddress.getLocalHost();
            return "%s:%d".formatted(host.getHostAddress(),
                    serverPort);
        } catch (UnknownHostException unknownHostException) {
            unknownHostException.printStackTrace();
        }

        return null;
    }

}
