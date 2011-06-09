package hudson.plugins.ec2.ssh;

import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.KeyPairInfo;
import hudson.model.TaskListener;
import hudson.plugins.ec2.EC2Cloud;
import hudson.plugins.ec2.EC2Computer;
import hudson.plugins.ec2.EC2ComputerLauncher;
import hudson.plugins.ec2.EC2Slave;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.tools.JDKInstaller;
import hudson.util.IOException2;
import org.jets3t.service.S3ServiceException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.URL;

/**
 * Created by bitter [2011-06-08]
 */
public class EC2UnixLauncher extends EC2ComputerLauncher {

    public class EC2JDKInstaller extends SSHLauncher.DefaultJDKInstaller {
        public URL locate(TaskListener log, JDKInstaller.Platform platform, JDKInstaller.CPU cpu) throws IOException {
            if (platform == JDKInstaller.Platform.LINUX && cpu == JDKInstaller.CPU.i386) {
                try {
                    return EC2Cloud.get().buildPresignedURL("/hudson-ci/jdk/linux-i586/java1.6.0_12.tgz");
                } catch (S3ServiceException e) {
                    e.printStackTrace(log.error("Unable to "));
                }
            }
            return super.locate(log, platform, cpu);
        }
    }

    public synchronized void connect(final EC2Computer computer, final TaskListener listener) throws InterruptedException, EC2Exception, IOException {

        waitForSshToBecomeAvailable(computer, listener.getLogger());

        new SSHLauncher(computer.describeInstance().getDnsName(), computer.getNode().getSshPort(), computer.getRemoteAdmin(), "", storeKey(EC2Cloud.get().getKeyPair()), computer.getNode().jvmopts, "", new EC2JDKInstaller()).launch(computer, listener);
    }

    private void waitForSshToBecomeAvailable(EC2Computer computer, PrintStream logger) throws EC2Exception, InterruptedException {
        String hostname = computer.describeInstance().getDnsName();
        int port = computer.getNode().getSshPort();
        for (int i = NUMBER_OF_RETRIES; i >= 0; i--) {
            try {
                new Socket(hostname, port).close();
                return;
            } catch (IOException ignore) {
                logger.format("Waiting for SSH to come up [tries left %d]\n", i);
                Thread.sleep(5000);
            }
        }
        throw new InterruptedException("Unable to establish a connection to ssh://" + hostname + ":" + port + "");
    }

    private String storeKey(KeyPairInfo keyPair) throws IOException {
        File keyFile = null;
        try {
            keyFile = File.createTempFile("rsa", "key");
            keyFile.setWritable(true, true);
            keyFile.setReadable(true, true);
            keyFile.setExecutable(false);
            FileWriter writer = new FileWriter(keyFile);
            writer.append(new String(keyPair.getKeyMaterial().toCharArray()));
            writer.close();
            return keyFile.getAbsolutePath();

        } catch(IOException ioe) {
            throw new IOException2("Unable to create key file" + ((keyFile != null) ? " " + keyFile.getAbsolutePath() : ""), ioe);
        }
    }
}
