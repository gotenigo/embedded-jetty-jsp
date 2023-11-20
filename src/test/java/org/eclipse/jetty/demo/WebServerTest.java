package org.eclipse.jetty.demo;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;



@Disabled
public class WebServerTest {

    @Test
    public void testWebServer() throws Exception {

        String[] argument = new String[]{"src/main/resources/tssapiclient-latest.jks","qwertyuioppoiuytrewq"};
        WebServer.main(argument);

    }

}
