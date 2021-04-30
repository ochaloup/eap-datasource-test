package io.narayana.ochaloup;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@WebServlet(name="ServletCallTest", urlPatterns={"/"})
public class ServletCallTest extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    @EJB
    DatasourceEjb ejb;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("Testing " + new Date());

        if(request.getQueryString().contains("crash")) {
            ejb.crashWithPreparedTxn();
        }
        if(request.getQueryString().contains("recover")) {
            ejb.recover(DatasourceEjb.JNDI_XA_DS);
        }
    }
}
