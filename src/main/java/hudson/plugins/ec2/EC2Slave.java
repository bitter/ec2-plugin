package hudson.plugins.ec2;

import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.Jec2;
import hudson.Extension;
import hudson.model.Descriptor.FormException;
import hudson.model.TaskListener;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.ComputerConnector;
import hudson.slaves.NodeProperty;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Slave running on EC2.
 * 
 * @author Kohsuke Kawaguchi
 */
public final class EC2Slave extends AbstractCloudSlave {

    public EC2Slave(String instanceId, String description, String remoteFS, int numExecutors, Mode mode, String labelString, ComputerConnector connector) throws FormException, IOException {
        this(instanceId, description, remoteFS, numExecutors, mode, labelString, Collections.<NodeProperty<?>>emptyList(), connector);
    }

    @DataBoundConstructor
    public EC2Slave(String instanceId, String description, String remoteFS, int numExecutors, Mode mode, String labelString, List<? extends NodeProperty<?>> nodeProperties, ComputerConnector connector) throws FormException, IOException {
        super(instanceId, description, remoteFS, numExecutors, mode, labelString, new EC2ComputerLauncher(connector), new EC2RetentionStrategy(), nodeProperties);
    }

    /**
     * Constructor for debugging.
     */
    public EC2Slave(String instanceId) throws FormException, IOException {
        this(instanceId,"debug","/tmp/hudson", 1, Mode.NORMAL, "debug", Collections.<NodeProperty<?>>emptyList(), null);
    }


    /**
     * EC2 instance ID.
     */
    public String getInstanceId() {
        return getNodeName();
    }

    @Override
    public AbstractCloudComputer createComputer() {
        return new EC2Computer(this);
    }

    /**
     * Terminates the instance in EC2.
     */
    protected void _terminate(TaskListener listener) {
        try {
            Jec2 ec2 = EC2Cloud.get().connect();
            ec2.terminateInstances(Collections.singletonList(getInstanceId()));
            listener.getLogger().println("Terminated EC2 instance: " + getInstanceId());
        } catch (EC2Exception e) {
            listener.error("Failed to terminate EC2 instance: %s\n%s", getInstanceId(), e);
        }
    }

    @Extension
    public static final class DescriptorImpl extends SlaveDescriptor {
        public String getDisplayName() {
            return "Amazon EC2";
        }

        @Override
        public boolean isInstantiable() {
            return false;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(EC2Slave.class.getName());
}
