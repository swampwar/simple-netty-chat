package wind.yang.server;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

public class ServerRunBySpring {
    public static void main(String[] args) {
        AbstractApplicationContext context;

        context = new GenericXmlApplicationContext("classpath:applicationContext.xml");
        HttpNettyServer server = context.getBean("httpNettyServer", HttpNettyServer.class);
        server.start();
    }
}
