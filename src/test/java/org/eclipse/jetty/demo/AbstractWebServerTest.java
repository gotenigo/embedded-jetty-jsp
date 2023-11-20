package org.eclipse.jetty.demo;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

@Disabled
public abstract class AbstractWebServerTest {
    private WebServer webServer;

    @BeforeEach
    public void aJettyServer() throws Exception{
        webServer = new WebServer("keyStorePath", "keyStorePassword");
        webServer.start();
    }

    @AfterEach
    public void stopServer() throws Exception
    {
        webServer.stop();
    }

    public String resourceWithUrl(String uri) throws Exception
    {
        URL url = new URL(uri);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        InputStream inputStream = connection.getInputStream();
        byte[] response = new byte[inputStream.available()];
        inputStream.read(response);

        return new String(response);
    }
}
