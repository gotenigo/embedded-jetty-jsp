package org.eclipse.jetty.demo;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;


@Disabled
public class WebServerTest {

    @Test
    public void testWebServer() throws Exception {

        String[] argument = new String[]{"src/main/resources/tssapiclient-latest.jks","qwertyuioppoiuytrewq"};
        WebServer.main(argument);

    }



    @Test
    public void testUNICODE_decode(){

        String encodedString = "configuration.ems_file_connector.provider_folder_setup%5B0%5D.folder=sadfsdfsdf&configuration.ems_file_connector.provider_folder_setup%5B0%5D.extension=sssss&..."; // Your encoded string
        try {
            String decodedString = URLDecoder.decode(encodedString, StandardCharsets.UTF_8.toString());
            System.out.println("decodedString ="+decodedString);
        } catch (UnsupportedEncodingException e) {
            // Handle the exception appropriately
            e.printStackTrace();
        }

    }

}
