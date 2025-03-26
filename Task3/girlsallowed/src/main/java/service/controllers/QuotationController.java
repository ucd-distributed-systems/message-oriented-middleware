package service.controllers;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.girlsallowed.GAQService;
import service.core.ClientInfo;
import service.core.Quotation;

// declares that class implements a controller
// allows you to annotate methods as handlers for HTTP requests on
// specific endpoints
@RestController
public class QuotationController {
    // maps ids to quotations
    private Map<String, Quotation> quotations = new TreeMap<>();
    private GAQService service = new GAQService();

    // importing the server port from application.properties
    @Value("${server.port}")
    private int serverPort;

    // GET request on /quotations endpoint
    // produces application/json content
    // returns object of type ResponseEntity
    // produces relates to format of return body
    @GetMapping(value="/quotations",
            produces="application/json")
    public ResponseEntity<ArrayList<String>> getQuotations()
    {
        ArrayList<String> list = new ArrayList<>();
        for (Quotation quotation : quotations.values()) {
            list.add("http:" + getHost()
                    + "/quotations/" + quotation.reference);
        }
        // return a list of strings (urls) instead of quotation objects
        // improvement as it gives us a way to access / query the returned quotatations
        // can use to retrieve an individual quotation
        // more closely follows Hypermedia As The Engine of Application State (HATEOAS)
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }

    // consumes relates to the format of payload
    @PostMapping(value="/quotations", consumes="application/json")
    // @RequestBody tag maps incoming request body to a ClientInfo object
    public ResponseEntity<Quotation> createQuotation(@RequestBody ClientInfo info) {
        Quotation quotation = service.generateQuotation(info);
        quotations.put(quotation.reference, quotation);
        String url = "http://"+getHost()+"/quotations/"
                + quotation.reference;
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location", url)
                .header("Content-Location", url)
                .body(quotation);
    }

    @GetMapping(value="/quotations/{id}", produces="application/json")
    // PathVariable creates variable from passed {id}
    public ResponseEntity<Quotation> getQuotation(@PathVariable String id) {
        Quotation quotation = quotations.get(id);

        if (quotation == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.status(HttpStatus.OK).body(quotation);
    }

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