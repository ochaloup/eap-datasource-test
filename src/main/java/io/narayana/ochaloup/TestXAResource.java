package io.narayana.ochaloup;

import org.jboss.logging.Logger;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.util.ArrayList;
import java.util.List;

/**
 * From org.wildfly:wildfly-testsuite-shared
 */
public class TestXAResource implements XAResource {
    private static final Logger log = Logger.getLogger(TestXAResource.class);

    public enum TestAction {
        NONE,

        PREPARE_THROW_XAER_RMERR, PREPARE_THROW_XAER_RMFAIL, PREPARE_THROW_UNKNOWN_XA_EXCEPTION, PREPARE_CRASH_VM,
        PREPARE_SLEEP_15S,

        COMMIT_THROW_XAER_RMERR, COMMIT_THROW_XAER_RMFAIL, COMMIT_THROW_UNKNOWN_XA_EXCEPTION, COMMIT_CRASH_VM,
        COMMIT_SLEEP_15S,
    }

    // prepared xids are shared over all the TestXAResource instances in the JVM
    // used for the recovery purposes as the XAResourceRecoveryHelper works with a different instance
    // of the XAResource than the one which is used during 2PC processing
    private static final List<Xid> preparedXids = new ArrayList<>();

    private int transactionTimeout;

    protected TestXAResource.TestAction testAction;

    public TestXAResource() {
        this(TestXAResource.TestAction.NONE);
    }

    public TestXAResource(TestXAResource.TestAction testAction) {
        this.testAction = testAction;
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        log.debugf("prepare xid: [%s], test action: %s", xid, testAction);

        switch (testAction) {
            case PREPARE_THROW_XAER_RMERR:
                throw new XAException(XAException.XAER_RMERR);
            case PREPARE_THROW_XAER_RMFAIL:
                throw new XAException(XAException.XAER_RMFAIL);
            case PREPARE_THROW_UNKNOWN_XA_EXCEPTION:
                throw new XAException(null);
            case PREPARE_CRASH_VM:
                Runtime.getRuntime().halt(0);
            case PREPARE_SLEEP_15S:
                try {
                    Thread.sleep(15_000);
                } catch (InterruptedException ie) {
                    log.infof("prepare xid: [%s], test action: %s was INTERRUPTED", xid, testAction);
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ie);
                }
                // intentionally fall-through !
            case NONE:
            default:
                preparedXids.add(xid);
                log.infof("prepare xid: [%s], test action: %s finished", xid, testAction);
                return XAResource.XA_OK;
        }
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        log.debugf("commit xid:[%s], %s one phase, test action: %s", xid, onePhase ? "with" : "without", testAction);

        switch (testAction) {
            case COMMIT_THROW_XAER_RMERR:
                throw new XAException(XAException.XAER_RMERR);
            case COMMIT_THROW_XAER_RMFAIL:
                throw new XAException(XAException.XAER_RMFAIL);
            case COMMIT_THROW_UNKNOWN_XA_EXCEPTION:
                throw new XAException(null);
            case COMMIT_CRASH_VM:
                Runtime.getRuntime().halt(0);
            case COMMIT_SLEEP_15S:
                try {
                    Thread.sleep(15_000);
                } catch (InterruptedException ie) {
                    log.warnf("commit xid:[%s], %s one phase, test action: %s was INTERRUPTED", xid, onePhase ? "with" : "without", testAction);
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ie);
                }
                // intentionally fall-through !
            case NONE:
            default:
                preparedXids.remove(xid);
        }
        log.infof("commit xid:[%s], %s one phase, test action: %s was finished", xid, onePhase ? "with" : "without", testAction);
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        log.debugf("rollback xid: [%s]", xid);
        preparedXids.remove(xid);
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
        log.debugf("end xid:[%s], flag: %s", xid, flags);
    }

    @Override
    public void forget(Xid xid) throws XAException {
        log.debugf("forget xid:[%s]", xid);
        preparedXids.remove(xid);
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        log.debugf("getTransactionTimeout: returning timeout: %s", transactionTimeout);
        return transactionTimeout;
    }

    @Override
    public boolean isSameRM(XAResource xares) throws XAException {
        log.debugf("isSameRM returning false to xares: %s", xares);
        return false;
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
        log.debugf("recover with flags: %s", flag);
        return preparedXids.toArray(new Xid[0]);
    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
        log.debugf("setTransactionTimeout: setting timeout: %s", seconds);
        this.transactionTimeout = seconds;
        return true;
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
        log.debugf("start xid: [%s], flags: %s", xid, flags);
    }

    public List<Xid> getPreparedXids() {
        return preparedXids;
    }
}
