import java.util.Random;

import org.apache.camel.builder.RouteBuilder;

public class MyRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("timer:foo?period=2s")
            .setBody(method(MyRouteBuilder.class, "randomNumber"))
            .log("Random number ${body}");
    }

    public static int randomNumber() {
        return new Random().nextInt(100);
    }
}
