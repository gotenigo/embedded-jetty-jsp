package org.eclipse.jetty.demo;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@WebServlet("/createConfig")
public class CreateConfigController extends HttpServlet {


    public CreateConfigController(){}

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Forward the request to your JSP login page
//        RequestDispatcher rs =request.getRequestDispatcher("LoginForm.html");
//        rs.forward(request,response);
        response.setContentType("text/html; charset=UTF-8");
        System.out.println("....gg say ....WE ARE ON GET");
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("....gg say ....WE ARE ON POST");

        System.out.println("req.getParameterMap()="+req.getParameterMap());
//        String json = new ObjectMapper().writeValueAsString(req.getParameterMap());
//        System.out.println("json ="+json);

        // Set up SnakeYAML options
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        // Create a YAML object
        Yaml yaml = new Yaml(options);

        // Convert the Map to a YAML string
        String valString =  yaml.dump(req.getParameterMap());

        System.out.println("valString="+valString);
        req.setCharacterEncoding("UTF-8");
        String val = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("val="+val);


        resp.sendRedirect("home.jsp");

    }



}