package service.core;

import java.util.ArrayList;

public class Offer {
    // provide a unique number to each offer
    private static int COUNTER = 1000;
    public int id;
    public ClientInfo info;
    public ArrayList<Quotation> quotations;

    public Offer(ClientInfo info) {
        this.id = COUNTER++;
        this.info = info;
        this.quotations = new ArrayList<>();
    }
    public Offer() {}
}
