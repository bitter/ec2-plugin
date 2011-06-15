package hudson.plugins.ec2;

import com.xerox.amazonws.ec2.ReservationDescription;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.plugins.ec2.EC2Computer;
import hudson.plugins.ec2.InstanceState;
import hudson.slaves.ComputerConnector;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;

import java.io.IOException;
import java.io.PrintStream;

import org.jets3t.service.S3ServiceException;

import com.xerox.amazonws.ec2.EC2Exception;

/**
 * {@link ComputerLauncher} for EC2 that waits for the instance to really come up before proceeding to
 * the real user-specified {@link ComputerLauncher}.
 *
 * @author Kohsuke Kawaguchi
 */
public class EC2ComputerLauncher extends ComputerLauncher {

    protected static final int NUMBER_OF_RETRIES = 120;
    public final ComputerConnector computerConnector;

    protected EC2ComputerLauncher(ComputerConnector computerConnector) {
        this.computerConnector = computerConnector;
    }

    @Override
    public void launch(SlaveComputer _computer, TaskListener listener) {
        EC2Computer computer = (EC2Computer)_computer;
        try {
            waitForComputerToEnterRunningState(computer, listener.getLogger());
            waitForComputerToRecieveIpAddress(computer, listener.getLogger());

            computerConnector.launch(computer.describeInstance().getDnsName(), listener).launch(_computer, listener);

        } catch (EC2Exception e) {
            e.printStackTrace(listener.error(e.getMessage()));
        } catch (InterruptedException e) {
            e.printStackTrace(listener.error(e.getMessage()));
        } catch (IOException e) {
            e.printStackTrace(listener.error(e.getMessage()));
        }
    }

    private void waitForComputerToRecieveIpAddress(EC2Computer computer, PrintStream logger) throws EC2Exception, InterruptedException {
        ReservationDescription.Instance inst = computer.updateInstanceDescription();
        for (int i = NUMBER_OF_RETRIES; i >= 0 && "0.0.0.0".equals(computer.updateInstanceDescription().getDnsName()); i--) {
            logger.format("Waiting instance to receive an ip address [tries left %d]\n", i);
            Thread.sleep(5000);
        }
        if ("0.0.0.0".equals(computer.updateInstanceDescription().getDnsName())) {
            throw new InterruptedException("Unable to determine ip address of " + computer.getInstanceId() + ".");
        }
    }

    private void waitForComputerToEnterRunningState(EC2Computer computer, PrintStream logger) throws EC2Exception, InterruptedException {
        logger.println("Waiting for instance to enter 'running' state.");
        while (computer.getState() == InstanceState.PENDING) {
            Thread.sleep(5000);
        }
        if (computer.getState() != InstanceState.RUNNING) {
            throw new InterruptedException("The instance " + computer.getInstanceId() + " never reached a running state.");
        }
    }

    public Descriptor<ComputerLauncher> getDescriptor() {
        throw new UnsupportedOperationException();
    }
}