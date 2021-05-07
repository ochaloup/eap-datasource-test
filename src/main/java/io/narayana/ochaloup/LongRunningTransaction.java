package io.narayana.ochaloup;

import org.jboss.as.test.integration.transactions.TestXAResource;
import org.jboss.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import java.sql.SQLException;
import java.sql.Statement;

@Stateless
public class LongRunningTransaction {
    private static final Logger log = Logger.getLogger(LongRunningTransaction.class);
    private static int SLEEP_TIME = Integer.getInteger("long.running.transaction.sleep.time.ms", 60_000);

    @Resource(lookup = "java:/TransactionManager")
    private TransactionManager tm;

    /**
     * 2 participants, 2PC processing
     */
    public void runLong() {
        try {
            TestXAResource testXAResource = new TestXAResource();
            tm.getTransaction().enlistResource(testXAResource);
        } catch (RollbackException | IllegalStateException | SystemException ex) {
            throw new RuntimeException("Cannot enlist " + TestXAResource.class.getName(), ex);
        }
        try (Statement st = DatasourceUtils.getXADs().getConnection().createStatement()) {
            st.execute("INSERT INTO test VALUES ('runLong')");
        } catch (SQLException sqle) {
            throw new RuntimeException("Error on inserting data to table 'test'", sqle);
        }
        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Error while sleeping", ie);
        }
    }
}
