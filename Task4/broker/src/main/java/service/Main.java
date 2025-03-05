package service;


import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import service.core.ClientInfo;
import service.message.ClientMessage;
import service.message.OfferMessage;

import java.util.HashMap;
import java.util.Map;

public class Main {
    private static Map<Long, OfferMessage> tokenToOffer = new HashMap<>();

    public static void main(String[] args) throws JMSException {
        try {
            String brokerUrl = "tcp://localhost:61616";
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                    brokerUrl);
            String brokerUser = "artemis";
            String brokerPassword = "artemis";
            Connection connection = connectionFactory.createConnection(
                    brokerUser, brokerPassword);
            String connectionId = "brokerServiceMain";
            connection.setClientID(connectionId);
            Session session = connection.createSession(
                    false, Session.CLIENT_ACKNOWLEDGE);

            Topic orders = session.createTopic("ORDERS");
            Queue quotes = session.createQueue("QUOTATIONS");
            Queue offers = session.createQueue("OFFERS");
            // consume messages from ORDERS topic
            // consume messages from QUOTATIONS queue
            MessageConsumer consumeOrders = session.createConsumer(orders);
            MessageConsumer consumeQuotes = session.createConsumer(quotes);
            // produce OfferMessages for OFFERS queue
            MessageProducer produceOffers = session.createProducer(offers);

            // begin delivery of messages
            connection.start();
            // consume a ClientMessage
            // create partially completed OfferMessage
            consumeOrders.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    try {
                        // receive message from orders topic
                        ClientMessage request = (ClientMessage) (
                                (ObjectMessage) message).getObject();

                        System.out.println(request.getClientInfo());
                        long token = request.getToken();
                        ClientInfo info = request.getClientInfo();

                        OfferMessage partialOffer = new OfferMessage(info);
                        // token associated with null because no offer generated yet
                        tokenToOffer.put(token, partialOffer);
                        // messages from client are to be explicitly acknowledged
                        message.acknowledge();
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            });
            // time limit to receive quotations before adding to OFFERS queue
            // anonymous object
            new Thread() {
                public void run() {
                    // consume messages from QUOTATIONS queue
                    // add quotations to OfferMessage
                    try {
                        Thread.sleep(3000);
                        // send all offers
                        for (OfferMessage offer : tokenToOffer.values()) {
                            // use Message interface
                            // abstracts communication with common type of message
                            Message response = session.createObjectMessage(offer);
                            produceOffers.send(response);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (JMSException e) {
                        throw new RuntimeException(e);
                    }
                }
            }.start();


        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
