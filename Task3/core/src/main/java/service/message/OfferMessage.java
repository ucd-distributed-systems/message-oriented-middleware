package service.message;
import java.util.LinkedList;
import service.core.ClientInfo;
import service.core.Quotation;

public class OfferMessage implements java.io.Serializable {
    private ClientInfo info;
    private LinkedList<Quotation> quotations;
    public OfferMessage(ClientInfo info,
                        LinkedList<Quotation> quotations) {
        this.info = info;
        this.quotations = quotations;
    }

    public ClientInfo getInfo() {
        return info;
    }
    public LinkedList<Quotation> getQuotations() {
        return quotations;
    }
}