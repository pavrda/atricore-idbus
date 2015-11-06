package org.atricore.idbus.kernel.main.mediation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.kernel.main.util.UUIDGenerator;

import javax.jms.ConnectionFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
public class MemoryMessageQueueManager implements MessageQueueManager {

    private static final Log logger = LogFactory.getLog(MemoryMessageQueueManager.class);

    private UUIDGenerator uuidGenerator = new UUIDGenerator();

    private int artifactTTL = 600; // seconds

    private int monitorInterval = 600; // seconds

    private OldArtifactsMonitor monitor;

    private Thread monitorThread;

    private Map<String, Message> msgs = new HashMap<String, Message>();

    public ConnectionFactory getConnectionFactory() {
        return null;
    }

    public String getJmsProviderDestinationName() {
        return null;
    }

    /**
     * Monitor interval, in seconds
     */
    public int getMonitorInterval() {
        return monitorInterval;
    }

    public void setMonitorInterval(int monitorInterval) {
        this.monitorInterval = monitorInterval;
    }


    /**
     * Message max. time to live, in seconds
     */
    public int getArtifactTTL() {
        return artifactTTL;
    }

    public void setArtifactTTL(int artifactTTL) {
        this.artifactTTL = artifactTTL;
    }

    public void init() throws Exception {
        msgs = new HashMap<String, Message>();

        // Start session monitor.
        monitor = new OldArtifactsMonitor(this, monitorInterval * 1000L);

        monitorThread = new Thread(monitor);
        monitorThread.setDaemon(true);
        monitorThread.setName("MemoryMessageQueueManagerMonitor");
        monitorThread.start();
    }

    public synchronized Object pullMessage(Artifact artifact) throws Exception {
        Message m = msgs.remove(artifact.getContent());
        if (m != null)
            return m.getContent();

        return null;
    }

    public synchronized Object peekMessage(Artifact artifact) throws Exception {
        Message m = msgs.get(artifact.getContent());
        if (m != null)
            return m.getContent();

        return null;
    }

    public synchronized Artifact pushMessage(Object msg) throws Exception {
        Artifact a = ArtifactImpl.newInstance(uuidGenerator.generateId());
        msgs.put(a.getContent(), new Message(a, msg));
        return a;
    }

    public void shutDown() throws Exception {
        msgs.clear();
        monitor.stop = true;
    }

    public ArtifactGenerator getArtifactGenerator() {
        return null;
    }

    protected synchronized void purgeOldMessages() {

        long now = System.currentTimeMillis();
        List<String> toRemove = null;

        for (String key : msgs.keySet()) {
            Message m = msgs.get(key);
            if (m.getCreationTime() + (artifactTTL * 1000L) < now) {
                if(toRemove == null) toRemove = new ArrayList<String>();
                toRemove.add(key);
            }
        }

        if (toRemove != null) {
            for (String keyToRemove : toRemove) {
                msgs.remove(keyToRemove);
            }
        }


    }

    public class Message {
        private Artifact artifact;

        private Object content;
        private long creationTime;

        public Message(Artifact artifact, Object content) {
            this.artifact = artifact;
            this.content = content;
            this.creationTime = System.currentTimeMillis();
        }

        public Artifact getArtifact() {
            return artifact;
        }

        public Object getContent() {
            return content;
        }

        public long getCreationTime() {
            return creationTime;
        }
    }

    public class OldArtifactsMonitor implements  Runnable {

        protected boolean stop = false;

        protected long interval = 1000 * 60 * 10; // Ten minutes interval

        private MemoryMessageQueueManager aqm;

        /**
         *
         * @param aqm Artifact Queue Manager instance
         *
         * @param monitorInterval monitor interval in millis
         */
        public OldArtifactsMonitor(MemoryMessageQueueManager aqm, long monitorInterval) {
            this.aqm = aqm;
            this.interval = monitorInterval;
        }

        public void run() {
            stop = false;
            do {
                try {

                    aqm.purgeOldMessages();

                    synchronized (this) {
                        try {

                            wait(interval);

                        } catch (InterruptedException e) { /**/ }
                    }

                } catch (Exception e) {
                    logger.warn("Exception received : " + e.getMessage() != null ? e.getMessage() : e.toString(), e);
                }

            } while (!stop);
        }
    }

}
