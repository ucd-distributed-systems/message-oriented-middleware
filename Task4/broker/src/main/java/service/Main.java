package service;


import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import service.core.ClientInfo;
import service.core.Quotation;
import service.message.ClientMessage;
import service.message.OfferMessage;
import service.message.QuotationMessage;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    private static Map<Long, OfferMessage> tokenToOffer = new HashMap<>();
    private static AtomicBoolean threadStarted = new AtomicBoolean(false);

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

                        long token = request.getToken();
                        System.out.println("Broker received client message with token: " + token);
                        ClientInfo info = request.getClientInfo();

                        // create partially complete offer and associate with token from client
                        // no quotations yet
                        OfferMessage partialOffer = new OfferMessage(info, new LinkedList<>());
                        tokenToOffer.put(token, partialOffer);

                        // Start the thread only once when the first order arrives
                        if (threadStarted.compareAndSet(false, true)) {
                            new Thread(() -> processOffers(tokenToOffer, produceOffers, session)).start();
                            System.out.println("Offer processing thread started.");
                        }

                        // messages from client are to be explicitly acknowledged
                        message.acknowledge();
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            });
//          
            // consume quotes in the Quotations queue
            consumeQuotes.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    try {
                        // receive message from quotes queue
                        QuotationMessage request = (QuotationMessage) (
                                (ObjectMessage) message).getObject();

                        long token = request.getToken();
                        System.out.println("Broker received quotation message with token: " + token);
                        Quotation quote = request.getQuotation();

                        // update the offer message by adding new quotes
                        OfferMessage currOffer = tokenToOffer.get(token);
                        currOffer.addQuotation(quote);
                        // messages from client are to be explicitly acknowledged
                        message.acknowledge();
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static void processOffers (Map <Long, OfferMessage> tokenToOffer, MessageProducer produceOffers, Session session) {
        while (true) {
            try {
                Thread.sleep(3000);
                // synchronized to ensure thread safety
                synchronized (tokenToOffer) {
                    if (!tokenToOffer.isEmpty()) {
                        for (OfferMessage offer : tokenToOffer.values()) {
                            try {
                                // use Message interface
                                // abstracts communication with common type of message
                                Message response = session.createObjectMessage(offer);
                                System.out.println("Broker sending response to Offers with quotations: " + offer.getQuotations());
                                produceOffers.send(response);
                            } catch (JMSException e) {
                                System.err.println("Error sending message: " + e.getMessage());
                            }
                        }
                    }
                    // clear processed offers
                    tokenToOffer.clear();
                }
            } catch (InterruptedException e) {
                System.err.println("Thread interrupted");
            } catch (Exception e) {
                System.err.println("Unexpected error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}


