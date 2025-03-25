package service;

import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import service.core.ClientInfo;
import service.core.Quotation;
import service.message.ClientMessage;
import service.message.OfferMessage;
import service.message.QuotationMessage;

import java.text.NumberFormat;

public class Main {
    public static void main(String[] args) throws JMSException {
        try {
            // URL references Messaging Server instance
            // can have multiple, if one host fails to connect, another is used
            String brokerUrl = "tcp://localhost:61616";
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                    brokerUrl);
            String brokerUser = "artemis";
            String brokerPassword = "artemis";
            Connection connection = connectionFactory.createConnection(
                    brokerUser, brokerPassword);
            // each client / implemented service must have a unique id
            // problematic when duplicating services
            String connectionId = "clientMain";
            connection.setClientID(connectionId);
            Session session = connection.createSession(
                    false, Session.CLIENT_ACKNOWLEDGE);

            Topic topic = session.createTopic("ORDERS");
            Queue queue = session.createQueue("OFFERS");
            // client posts orders, consumes offers
            MessageProducer producer = session.createProducer(topic);
            MessageConsumer consumer = session.createConsumer(queue);
            connection.start();

            int numc = 0;
            for (ClientInfo info : clients) {
                numc++;

                // create and send client message (token, clientInfo) to orders topic
                Message clientOrder = session.createObjectMessage(
                        new ClientMessage(numc, info)
                );
                producer.send(clientOrder);
            }

            // respond to offers from Offers queue
            consumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    try {
                        // receive offer
                        OfferMessage offerMessage = (OfferMessage) (
                                (ObjectMessage) message).getObject();

                        // display quotes from offer
                        for (Quotation quotation : offerMessage.getQuotations()) {
                            ClientInfo client = offerMessage.getInfo();
                            displayProfile(client);
                            displayQuotation(quotation);
                            System.out.println("\n\n");
                        }

                        message.acknowledge();
                    } catch (JMSException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Display the client info nicely.
     *
     * @param info Client info
     */
    public static void displayProfile(ClientInfo info) {
        System.out.println("|=================================================================================================================|");
        System.out.println("|                                     |                                     |                                     |");
        System.out.println(
                "| Name: " + String.format("%1$-29s", info.name) +
                        " | Gender: " + String.format("%1$-27s", (info.gender==ClientInfo.MALE?"Male":"Female")) +
                        " | Age: " + String.format("%1$-30s", info.age)+" |");
        System.out.println(
                "| Weight/Height: " + String.format("%1$-20s", info.weight+"kg/"+info.height+"m") +
                        " | Smoker: " + String.format("%1$-27s", info.smoker?"YES":"NO") +
                        " | Medical Problems: " + String.format("%1$-17s", info.medicalIssues?"YES":"NO")+" |");
        System.out.println("|                                     |                                     |                                     |");
        System.out.println("|=================================================================================================================|");
    }

    /**
     * Display a quotation nicely - note that the assumption is that the quotation will follow
     * immediately after the profile (so the top of the quotation box is missing).
     *
     * @param quotation From quotation services
     */
    public static void displayQuotation(Quotation quotation) {
        System.out.println(
                "| Company: " + String.format("%1$-26s", quotation.company) +
                        " | Reference: " + String.format("%1$-24s", quotation.reference) +
                        " | Price: " + String.format("%1$-28s", NumberFormat.getCurrencyInstance().format(quotation.price))+" |");
        System.out.println("|=================================================================================================================|");
    }

    /**
     * Test Data
     */
    public static final ClientInfo[] clients = {
            new ClientInfo("One Two", ClientInfo.FEMALE, 49, 1.5494, 80, false, false),
            new ClientInfo("Three Four", ClientInfo.MALE, 65, 1.6, 100, true, true),
            new ClientInfo("Five Six'", ClientInfo.FEMALE, 21, 1.78, 65, false, false),
            new ClientInfo("Seven Eight", ClientInfo.MALE, 49, 1.8, 120, false, true),
            new ClientInfo("Nine Ten", ClientInfo.MALE, 55, 1.9, 75, true, false),
            new ClientInfo("Eleven Twelve", ClientInfo.MALE, 35, 0.45, 1.6, false, false)
    };

}
