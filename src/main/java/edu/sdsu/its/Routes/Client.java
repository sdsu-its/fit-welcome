package edu.sdsu.its.Routes;

import com.google.gson.Gson;
import org.jtwig.web.servlet.JtwigRenderer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * @author Tom Paulus
 *         Created on 6/24/17.
 */
public class Client extends HttpServlet {
    private final JtwigRenderer renderer = JtwigRenderer.defaultRenderer();
    final String TEMPLATE_PATH = "/WEB-INF/templates/client/index.twig";

    private static Page[] SITEMAP = null;

    public Client() {
        super();

        if (SITEMAP == null) {
            SITEMAP = new Gson().fromJson(
                    new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("sitemap.json")),
                    Page[].class);
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setAttribute("pages", SITEMAP);

        response.addHeader("content-type", MediaType.TEXT_HTML);
        renderer.dispatcherFor(TEMPLATE_PATH)
                .render(request, response);
    }

    @SuppressWarnings("unused")
    private static class Page {
        private String pageID;
        private String pageHead;
        private Button[] buttons;

        private static class Button{
            private String title;
            private String action;
        }
    }
}
