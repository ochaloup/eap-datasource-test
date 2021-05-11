package io.narayana.ochaloup;

import org.jboss.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalTime;

@Stateless
public class LongRunningTransaction {
    private static final Logger log = Logger.getLogger(LongRunningTransaction.class);
    private static final int SLEEP_TIME = Integer.getInteger("long.running.transaction.sleep.time.ms", 15_000);

    public enum Action {
        WAIT_IN, WAIT_IN_EJB, WAIT_PREPARE, WAIT_COMMIT
    }

    @Resource(lookup = "java:/TransactionManager")
    private TransactionManager tm;

    @Resource(lookup = "java:jboss/datasources/xaDs")
    private DataSource ds;

    @EJB
    LongRunningTransaction thisEjb;

    /**
     * 2 participants, 2PC processing
     */
    public void runLong(LongRunningTransaction.Action action) {
        LocalTime startTime = LocalTime.now();
        log.info("Entering the runLong method : " + startTime);

        dataInsert(action, startTime.toString());

        log.info("Leaving the runLong method : " + LocalTime.now());
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private void waitStuff() {
        try {
            // log.info("Txn: " + tm.getTransaction());
            log.info("TM: " + tm + ", tm2: " + new InitialContext().lookup("java:/TransactionManager") + ", ejb: " + thisEjb + ", ds: " + ds);
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ie);
        } catch (Exception e) {
            throw new IllegalStateException("Error on transaction getting", e);
        }
    }

    private void dataInsert(LongRunningTransaction.Action action, String dataInsertion) {
        TestXAResource.TestAction testXAResourceAction = TestXAResource.TestAction.NONE;
        if (action == Action.WAIT_PREPARE) {
            testXAResourceAction = TestXAResource.TestAction.PREPARE_SLEEP_15S;
        }
        if (action == Action.WAIT_COMMIT) {
            testXAResourceAction = TestXAResource.TestAction.COMMIT_SLEEP_15S;
        }

        try (Statement st = DatasourceUtils.getXADs().getConnection().createStatement()) {
            st.execute("INSERT INTO test VALUES ('runLong " + dataInsertion + "')");
        } catch (SQLException sqle) {
            throw new RuntimeException("Error on inserting data to table 'test'", sqle);
        }
        try {
            TestXAResource testXAResource = new TestXAResource(testXAResourceAction);
            tm.getTransaction().enlistResource(testXAResource);
        } catch (RollbackException | IllegalStateException | SystemException ex) {
            throw new RuntimeException("Cannot enlist " + TestXAResource.class.getName(), ex);
        }

        if (action == Action.WAIT_IN) {
            log.info("...wait in only one ejb");
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Error while sleeping", ie);
            }
        }
        if (action == Action.WAIT_IN_EJB) {
            log.info("...wait in multiple ejb");
            long startTimeStamp = System.currentTimeMillis();
            while(startTimeStamp + SLEEP_TIME > System.currentTimeMillis()) {
                thisEjb.waitStuff();
            }
        }
    }
}