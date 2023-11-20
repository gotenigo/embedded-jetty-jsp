package app;


import org.eclipse.jetty.demo.WebServer;


import static org.eclipse.jetty.demo.WebServer.DEFAULT_PORT;

public class App {


    public static void main(String[] args) throws Exception {

        Integer port=DEFAULT_PORT;
        String webConsoleLogin="admin";
        String webConsolePassword="password" ;
        String keyStorePath = "src/main/resources/tssapiclient-latest.jks";
        String password= "qwertyuioppoiuytrewq";


        WebServer webServer = new WebServer(port, webConsoleLogin, webConsolePassword, keyStorePath, password);
        webServer.start("E:\\GUI_IN_JAR2\\embedded-jetty-jsp\\app-gui\\src\\main\\resources\\webroot2\\");
        webServer.waitForInterrupt();

    }



}
