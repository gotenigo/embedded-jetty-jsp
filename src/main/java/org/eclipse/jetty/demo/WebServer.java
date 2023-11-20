package org.eclipse.jetty.demo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;

import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;
import org.eclipse.jetty.demo.jsp.EmbeddedJspStarter;
import org.eclipse.jetty.jsp.JettyJspServlet;
import org.eclipse.jetty.security.*;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.util.ssl.SslContextFactory;


/**
 * Example of using JSP's with embedded jetty and using a
 * lighter-weight ServletContextHandler instead of a WebAppContext.
 *
 * This example is somewhat odd in that it uses custom tag libs which reside
 * in a WEB-INF directory, even though WEB-INF is not meaningful to
 * a ServletContextHandler. This just shows that once we have
 * properly initialized the jsp engine, you can even use this type of
 * custom taglib, even if you don't have a full-fledged webapp.
 */


@Slf4j
public class WebServer {

    // Resource path pointing to where the WEBROOT is
    public static final String WEBROOT_INDEX = "/webroot/";
    public static final Integer DEFAULT_PORT=8090;
    private static final String DEFAULT_LOGIN="admin";
    private static final String DEFAULT_PASSWORD="password";
    private final int port;
    private final Server server;

    private final String webConsoleLogin;
    private final String webConsolePassword;

    private final String keyStorePath;
    private final String keyStorePassword;


    public static void main(String[] args) throws Exception {
        Integer port=DEFAULT_PORT;
        String webConsoleLogin=DEFAULT_LOGIN;
        String webConsolePassword=DEFAULT_PASSWORD;
        String webSource=null;
        WebServer webServer = null;

        String keyStorePath = null,keyStorePassword=null;
        if(args!=null && args.length>=2){
            keyStorePath=args[0];
            keyStorePassword=args[1];
        }else{
            throw new IllegalArgumentException("keyStorePath and keyStorePassword are mandatory for 1-way TLS.");
        }

        if(args.length>=3) {
            port = Integer.valueOf(args[2]);
            if (args.length >= 5) {
                webConsoleLogin = args[3];
                webConsolePassword = args[4];
            }
            if (args.length >= 6) {
                webSource = args[5];
            }
        }
        webServer = new WebServer(port, webConsoleLogin, webConsolePassword, keyStorePath, keyStorePassword);
        webServer.start(webSource);
        webServer.waitForInterrupt();
    }

    public WebServer(String keyStorePath, String keyStorePassword){
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
        this.port = DEFAULT_PORT;
        this.webConsoleLogin = DEFAULT_LOGIN;
        this.webConsolePassword = DEFAULT_PASSWORD;
        this.server=new Server();
    }


    public WebServer(Integer port, String webConsoleLogin, String webConsolePassword, String keyStorePath, String keyStorePassword){
        this.port = (port==null)?DEFAULT_PORT:port;
        this.webConsoleLogin = (webConsoleLogin==null)?DEFAULT_LOGIN:webConsoleLogin;
        this.webConsolePassword = (webConsolePassword==null)?DEFAULT_PASSWORD:webConsolePassword;
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
        this.server=new Server();
    }




    public void start() throws Exception{
        start(null);
    }


    public void start(String webSource) throws Exception{

        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSecureScheme("https");
        httpConfig.setSecurePort(port);
        httpConfig.setSendServerVersion(true);

        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath(new File(keyStorePath).getAbsolutePath());
        sslContextFactory.setKeyStorePassword(keyStorePassword);
        sslContextFactory.setSniRequired(false);

        ServerConnector connector = new ServerConnector(
                server,
                sslContextFactory,
                new HttpConnectionFactory(httpConfig));
        connector.setPort(port);
        server.addConnector(connector);

        // Base URI for servlet context
        URI baseUri = getWebRootResourceUri();

        // Create Servlet context
        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletContextHandler.setContextPath("/");
        //WebAppContext root = new WebAppContext();
        //servletContextHandler.setDescriptor(webappDirLocation+"/WEB-INF/web.xml");
        //baseUri.toASCIIString()
        servletContextHandler.setResourceBase(webSource);
        servletContextHandler.setSecurityHandler(getSecurityHandler());

        // Since this is a ServletContextHandler we must manually configure JSP support.
        enableEmbeddedJspSupport(servletContextHandler);

        if(webSource!=null){
            webSource = baseUri.toASCIIString();
        }
        ServletHolder holderDefault = new ServletHolder("createConfig",new CreateConfigController());
        holderDefault.setInitParameter("resourceBase", webSource);
        holderDefault.setInitParameter("dirAllowed", "true");
        servletContextHandler.addServlet(holderDefault, "/createConfig");


        servletContextHandler.setWelcomeFiles(new String[] { "home.jsp" });
        ServletHolder holderPwd = new ServletHolder("default", DefaultServlet.class);
        //baseUri.toASCIIString()
        holderPwd.setInitParameter("resourceBase",webSource);
        holderPwd.setInitParameter("dirAllowed","true");
        servletContextHandler.addServlet(holderPwd,"/");

        server.setHandler(servletContextHandler);

        // Start Server
        // server.setDumpAfterStart(true);
        server.start();
        log.debug("Web Server started under URI '{}'",server.getURI());
    }



    /**
     * Setup JSP Support for ServletContextHandlers.
     * <p>
     * NOTE: This is not required or appropriate if using a WebAppContext.
     * </p>
     *
     * @param servletContextHandler the ServletContextHandler to configure
     * @throws IOException if unable to configure
     */
    private void enableEmbeddedJspSupport(ServletContextHandler servletContextHandler) throws IOException{
        // Establish Scratch directory for the servlet context (used by JSP compilation)
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File scratchDir = new File(tempDir.toString(), "embedded-jetty-jsp");

        if (!scratchDir.exists()){
            if (!scratchDir.mkdirs()){
                throw new IOException("Unable to create scratch directory: " + scratchDir);
            }
        }
        servletContextHandler.setAttribute("javax.servlet.context.tempdir", scratchDir);

        // Set Classloader of Context to be sane (needed for JSTL)
        // JSP requires a non-System classloader, this simply wraps the
        // embedded System classloader in a way that makes it suitable
        // for JSP to use
        ClassLoader jspClassLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader());
        servletContextHandler.setClassLoader(jspClassLoader);

        // Manually call JettyJasperInitializer on context startup
        servletContextHandler.addBean(new EmbeddedJspStarter(servletContextHandler));

        // Create / Register JSP Servlet (must be named "jsp" per spec)
        ServletHolder holderJsp = new ServletHolder("jsp", JettyJspServlet.class);
        holderJsp.setInitOrder(0);
        holderJsp.setInitParameter("scratchdir", scratchDir.toString());
        holderJsp.setInitParameter("logVerbosityLevel", "DEBUG");
        holderJsp.setInitParameter("fork", "false");
        holderJsp.setInitParameter("xpoweredBy", "false");
        holderJsp.setInitParameter("compilerTargetVM", "1.8");
        holderJsp.setInitParameter("compilerSourceVM", "1.8");
        holderJsp.setInitParameter("keepgenerated", "true");
        servletContextHandler.addServlet(holderJsp, "*.jsp");

        servletContextHandler.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());
    }

    private URI getWebRootResourceUri() throws FileNotFoundException, URISyntaxException{
        URL indexUri = this.getClass().getResource(WEBROOT_INDEX);
        if (indexUri == null){
            throw new FileNotFoundException("Unable to find resource " + WEBROOT_INDEX);
        }
        // Points to wherever /webroot/ (the resource) is
        return indexUri.toURI();
    }


    public void stop() throws Exception{
        server.stop();
    }

    /**
     * Cause server to keep running until it receives a Interrupt.
     * <p>
     * Interrupt Signal, or SIGINT (Unix Signal), is typically seen as a result of a kill -TERM {pid} or Ctrl+C
     *
     * @throws InterruptedException if interrupted
     */
    public void waitForInterrupt() throws InterruptedException{
        server.join();
    }




    private SecurityHandler getSecurityHandler() {

        HashLoginService hashLoginService = new HashLoginService();
        hashLoginService.setName("Private");

        var userStore = new UserStore();
        userStore.addUser(webConsoleLogin, Credential.getCredential(webConsolePassword), new String[] {"user"});
        hashLoginService.setUserStore(userStore);

        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(new String[]{"user"});
        constraint.setAuthenticate(true);

        ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint(constraint);
        cm.setPathSpec("/*");

        ConstraintSecurityHandler csh = new ConstraintSecurityHandler();
        csh.setAuthenticator(new BasicAuthenticator());
        csh.setRealmName("REALM");
        csh.addConstraintMapping(cm);
        csh.setLoginService(hashLoginService); //AbstractLoginService , HashLoginService
        return csh;
    }






}
