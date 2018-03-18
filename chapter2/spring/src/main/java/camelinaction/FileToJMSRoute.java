package camelinaction;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class FileToJMSRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("ftp://rider.com/orders?username=rider&password=secret")
                .to("jms:incomingOrders");
    }
}
