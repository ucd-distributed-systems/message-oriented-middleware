import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import service.Main;
import service.core.ClientInfo;
import service.message.ClientMessage;
import service.message.QuotationMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BrokerServiceUnitTest {
    @BeforeAll
    public static void setup() throws JMSException {
        Main.main(new String[0]);
    }
    @Test
    public void testService() throws Exception {
        try {
            String brokerUrl = "tcp://localhost:61616";
            ConnectionFactory connectionFactory =
                    new ActiveMQConnectionFactory(brokerUrl);
            String brokerUser = "artemis";
            String brokerPassword = "artemis";
            // create a connection at specified url, access granted via user / password
            Connection connection = connectionFactory.createConnection(
                    brokerUser, brokerPassword);
            // unique client id for service
            String connectionId = "brokerServiceTest";
            connection.setClientID(connectionId);
            Session session = connection.createSession(
                    false, Session.CLIENT_ACKNOWLEDGE);
            Queue quotes = session.createQueue("QUOTATIONS");
            Topic orders = session.createTopic("ORDERS");
            Queue offers = session.createQueue("OFFERS");

            // mimic client sending messages to orders topic
            MessageProducer client = session.createProducer(orders);
            MessageProducer qServices = session.createProducer(quotes);
            // consume messages from ORDERS topic (received from client)
            // consume messages from QUOTATIONS queue (quote services produce to this)
            MessageConsumer brokerQuotes = session.createConsumer(quotes);
            MessageConsumer brokerOrders = session.createConsumer(orders);
            // produce OfferMessages for OFFERS queue
            // broker matches quotes to orders
            MessageProducer produceOffers = session.createProducer(offers);

            connection.start();
            // add to the orders topic (mimics a client posting a publishing a message)
            client.send(
                    session.createObjectMessage(
                            new ClientMessage(1L, new ClientInfo("FirstName LastName",
                                    ClientInfo.FEMALE, 49, 1.5494, 80, false,
                                    false))));
            // uses older blocking approach receive
            // ensures that test does not finish before QuotationMessage received
            Message message = .receive();
            QuotationMessage quotationMessage =
                    (QuotationMessage) (
                            (ObjectMessage) message).getObject();
            System.out.println("\nToken: " + quotationMessage.getToken());
            System.out.println("Quotation: " +
                    quotationMessage.getQuotation() + "\n");
            message.acknowledge();
            assertEquals(1L, quotationMessage.getToken());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
