package com.fraudshield.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** /about - project description page. */
@WebServlet(name = "AboutServlet", urlPatterns = "/about")
public class AboutServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("modelReport", services().ml().getReport());
        render(req, resp, "about", "About the project", "about");
    }
}
