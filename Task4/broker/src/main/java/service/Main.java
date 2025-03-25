package service;


import jakarta.jms.*;
import jakarta.jms.Queue;
import org.apache.activemq.ActiveMQConnectionFactory;
import service.core.ClientInfo;
import service.core.Quotation;
import service.message.ClientMessage;
import service.message.OfferMessage;
import service.message.QuotationMessage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    private static final Map<Long, OfferMessage> tokenToOffer = new ConcurrentHashMap<>();
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
                        ClientInfo info = request.getClientInfo();
                        System.out.println("Received Order for Client: " + token);
                        // create partially complete offer and associate with token from client
                        // no quotations yet
                        OfferMessage partialOffer = new OfferMessage(info, new LinkedList<>());
                        tokenToOffer.put(token, partialOffer);

                        // Start the thread only once when the first order arrives
                        if (threadStarted.compareAndSet(false, true)) {
                            // spawn thread to process quotes received from quotes queue
                            // process into OfferMessages to be added to Offers queue
                            new Thread(() -> processOffers(tokenToOffer.values(), produceOffers, session)).start();
                            System.out.println("Offer processing thread started.");
                        }

                        // messages from client are to be explicitly acknowledged
                        message.acknowledge();
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            });
            // consume quotes in the Quotations queue
            consumeQuotes.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    try {
                        // receive message from quotes queue
                        QuotationMessage request = (QuotationMessage) (
                                (ObjectMessage) message).getObject();

                        long token = request.getToken();
                        Quotation quote = request.getQuotation();
                        // update the offer message by adding new quotes
                        System.out.println("Received Quote for Client: " + token);
                        // tokens from QuotationMessages must correspond to a token from a ClientMessage
                        // quotation is for the client
                        if (tokenToOffer.containsKey(token)) {
                            // synchronized for thread safety?
                            synchronized (tokenToOffer) {
                                // update an OfferMessage with more quotations
                                OfferMessage currOffer = tokenToOffer.get(token);
                                List<Quotation> quotations = currOffer.getQuotations();
                                quotations.add(quote);
                            }
                        }

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

    private static void processOffers (Collection<OfferMessage> offers, MessageProducer offerProducer, Session session) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            System.err.println("Thread interrupted");
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }

        // send all offers to Offer queue received during sleep
        Iterator<OfferMessage> iterator = offers.iterator();
        while (iterator.hasNext()) {
            OfferMessage offer = iterator.next();
            try {
                Message response = session.createObjectMessage(offer);
                offerProducer.send(response);
                System.out.println("Sending Offer for: " + offer.getInfo().name);
                iterator.remove();
            } catch (JMSException e) {
                System.err.println("Error sending message" + e.getMessage());
            }
        }
    }
}


