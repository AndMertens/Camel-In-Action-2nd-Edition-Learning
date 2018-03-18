package camelinaction;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

public class OrderRouterWithWireTapTest extends CamelTestSupport {

    @Override
    protected CamelContext createCamelContext() throws Exception {
        // create CamelContext
        CamelContext camelContext = super.createCamelContext();
        
        // connect to embedded ActiveMQ JMS broker
        ConnectionFactory connectionFactory = 
            new ActiveMQConnectionFactory("vm://localhost");
        camelContext.addComponent("jms",
            JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));
        
        return camelContext;
    }
    
    @Test
    public void testPlacingOrders() throws Exception {
        getMockEndpoint("mock:wiretap").expectedMessageCount(1);
    	getMockEndpoint("mock:xml").expectedMessageCount(0);
        getMockEndpoint("mock:csv").expectedMessageCount(1);
        assertMockEndpointsSatisfied();
    }
    
    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        final String incomingOrdersUri = "jms:incomingOrders";
        final String orderAuditUri = "jms:orderAudit";
        final String xmlOrdersUri = "jms:xmlOrders";
        final String csvOrdersUri = "jms:csvOrders";


        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // load file orders from src/data into the JMS queue
                from("file:src/data?noop=true").to(incomingOrdersUri);
        
                // content-based router
                from(incomingOrdersUri)
                    .wireTap(orderAuditUri)
	                .choice()
	                    .when(header("CamelFileName").endsWith(".xml"))
	                        .to(xmlOrdersUri)
	                    .when(header("CamelFileName").regex("^.*(csv|csl)$"))
	                        .to(csvOrdersUri);
                
                // test that our route is working
                from(xmlOrdersUri)
	                .log("Received XML order: ${header.CamelFileName}")
	                .to("mock:xml");                
                
                from(csvOrdersUri)
	                .log("Received CSV order: ${header.CamelFileName}")
	                .to("mock:csv");
                
                from(orderAuditUri)
	                .log("Wire tap received order: ${header.CamelFileName}")
	                .to("mock:wiretap");   
            }
        };
    }
}
