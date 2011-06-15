package hudson.plugins.ec2;

import com.xerox.amazonws.ec2.InstanceType;
import hudson.plugins.sshslaves.SSHConnector;
import org.jvnet.hudson.test.HudsonTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic test to validate SlaveTemplate.
 */
public class SlaveTemplateTest extends HudsonTestCase {

    public void testConfigRoundtrip() throws Exception {
        String ami = "ami1";
        SlaveTemplate orig = new SlaveTemplate(ami, "foo", InstanceType.LARGE, "label1 label2", "desc", "10", "", new SSHConnector(1024, "name", "pwd", "key", "-Xmx256", "/usr/java"));
        List<SlaveTemplate> templates = new ArrayList<SlaveTemplate>();
        templates.add(orig);
        AmazonEC2Cloud ac = new AmazonEC2Cloud(AwsRegion.US_EAST_1, "abc", "def", "ghi", "3", templates);
        hudson.clouds.add(ac);
        submit(createWebClient().goTo("configure").getFormByName("config"));
        SlaveTemplate received = ((EC2Cloud)hudson.clouds.iterator().next()).getTemplate(ami);
        assertEqualBeans(orig, received, "ami,description,remoteFS,type,userData,labels,numExecutors,computerConnector.class");
    }
}
