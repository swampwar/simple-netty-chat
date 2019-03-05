package wind.yang.server.spring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import wind.yang.server.HttpNettyServer;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:applicationContext.xml")
public class PropertiesTest {
    @Autowired
    ApplicationContext context;

    @Test
    public void readProperties(){
        HttpNettyServer server = context.getBean("httpNettyServer", HttpNettyServer.class);
        System.out.println(server.PORT);
    }
}
