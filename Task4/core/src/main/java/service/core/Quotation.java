package service.core;

/**
 * Class to store the quotations returned by the quotation services
 * 
 * @author Rem
 *
 */
public class Quotation implements java.io.Serializable {
	public Quotation(String company, String reference, double price) {
		this.company = company;
		this.reference = reference;
		this.price = price;
		
	}

	// default constructor needed by library which transforms java object to XML and vice versa
	// library first creates object then updates fields
	public Quotation() {}
	
	public String company;
	public String reference;
	public double price;
}
