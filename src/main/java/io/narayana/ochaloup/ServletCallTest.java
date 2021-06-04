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
    DatasourceEjb ejbDs;
    @EJB
    LongRunningTransaction ejbLongTx;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("Testing " + new Date());

        if (request.getQueryString() == null || request.getQueryString().isEmpty()) {
            return;
        }
        if(request.getQueryString().contains("crash")) {
            ejbDs.crashWithPreparedTxn();
        }
        if(request.getQueryString().contains("recover")) {
            ejbDs.recover(DatasourceUtils.JNDI_XA_DS);
        }
        if(request.getQueryString().contains("sleep_in")) {
            ejbLongTx.runLong(LongRunningTransaction.Action.WAIT_IN);
        }
        if(request.getQueryString().contains("sleep_ejb")) {
            ejbLongTx.runLong(LongRunningTransaction.Action.WAIT_IN_EJB);
        }
        if(request.getQueryString().contains("sleep_prepare")) {
            ejbLongTx.runLong(LongRunningTransaction.Action.WAIT_PREPARE);
        }
        if(request.getQueryString().contains("sleep_commit")) {
            ejbLongTx.runLong(LongRunningTransaction.Action.WAIT_COMMIT);
        }
    }
}
