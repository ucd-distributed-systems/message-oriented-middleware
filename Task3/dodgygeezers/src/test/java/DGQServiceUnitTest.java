import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import service.Main;
import service.core.ClientInfo;
import service.message.ClientMessage;
import service.message.QuotationMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DGQServiceUnitTest {
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
            String connectionId = "dodgygeezersTest";
            connection.setClientID(connectionId);
            Session session = connection.createSession(
                    false, Session.CLIENT_ACKNOWLEDGE);
            Queue queue = session.createQueue("QUOTATIONS");
            Topic topic = session.createTopic("ORDERS");
            // producer mimics client (adds to orders topic)
            // consumer mimics service (consumes from orders, adds to quotations queue)
            MessageConsumer consumer = session.createConsumer(queue);
            MessageProducer producer = session.createProducer(topic);
            connection.start();
            // add to the orders topic (mimics a client posting a publishing a message)
            producer.send(
                    session.createObjectMessage(
                            new ClientMessage(1L, new ClientInfo("FirstName LastName",
                                    ClientInfo.FEMALE, 49, 1.5494, 80, false,
                                    false))));
            // uses older blocking approach receive
            // ensures that test does not finish before QuotationMessage received
            Message message = consumer.receive();
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
