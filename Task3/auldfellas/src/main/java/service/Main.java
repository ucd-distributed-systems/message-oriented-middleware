package service;

import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import service.auldfellas.AFQService;
import service.core.Quotation;
import service.message.ClientMessage;
import service.message.QuotationMessage;

public class Main {
    private static AFQService afqService = new AFQService();
    public static void main(String[] args) throws JMSException {
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
        String connectionId = "auldfellasMain";
        connection.setClientID(connectionId);
        Session session = connection.createSession(
                false, Session.CLIENT_ACKNOWLEDGE);

        Queue queue = session.createQueue("QUOTATIONS");
        Topic topic = session.createTopic("ORDERS");
        // connect auldfellas to Quotations queue and Orders topic
        MessageConsumer consumer = session.createConsumer(topic);
        MessageProducer producer = session.createProducer(queue);
        connection.start();

        consumer.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                try {
                    // convert Message Object to ObjectMessage
                    // assumes ORDERS topic only produces ObjectMessages
                    // extract content of message
                    ClientMessage request = (ClientMessage) (
                            (ObjectMessage) message).getObject();
                    // generate quotation using the clientInfo contained in the message
                    Quotation quotation = afqService.generateQuotation(
                            request.getClientInfo());
                    // create a quotation message in form of ObjectMessage
                    Message response = session.createObjectMessage(
                            new QuotationMessage(request.getToken(), quotation)
                    );
                    // add QuotationMessage to Quotations queue
                    producer.send(response);
                    message.acknowledge();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
