// use modeline to configure properties directly in the same source file
// camel-k: language=java name=Cool property=period=1000

public class Hey extends org.apache.camel.builder.RouteBuilder {

  @Override
  public void configure() throws Exception {
      // Write your routes here, for example:
      from("timer:java?period={{period}}")
        .routeId("java")
        .process(e -> {
           e.getMessage().setBody("Hello from Camel");
        })
        .to("log:info");
  }
}
