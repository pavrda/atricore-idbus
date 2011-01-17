package com.atricore.idbus.console.liveservices.liveupdate.main.test;

import com.atricore.idbus.console.liveservices.liveupdate.main.repository.impl.FileRepositoryTransport;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.Selectors;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.net.URI;

public class FileRepositoryTransportTest extends VFSTestSupport {

    private static FileRepositoryTransport repositoryTransport;

    private static String testFile = "";

    @BeforeClass
    public static void setupTestSuite() throws Exception {
        applicationContext = new ClassPathXmlApplicationContext(
                new String[]{"classpath:com/atricore/idbus/console/liveservices/liveupdate/main/test/transport-beans.xml"}
        );

        repositoryTransport = (FileRepositoryTransport) applicationContext.getBean("fileRepositoryTransport");

        String buildDir = (String) applicationContext.getBean("buildDir");
        testFile = "file://" + buildDir + "/liveservices/liveupdate/repos/md/cache/repo1/repo1-updates.xml";
        
        // copy test files to repository
        String baseDir = (String) applicationContext.getBean("baseDir");
        FileObject testUpdatesSrc = getFileSystemManager().resolveFile(baseDir + "/src/test/resources/com/atricore/idbus/console/liveservices/liveupdate/main/test/repo1-updates.xml");
        FileObject testUpdatesDest = getFileSystemManager().resolveFile(testFile);
        testUpdatesDest.createFile();
        testUpdatesDest.copyFrom(testUpdatesSrc, Selectors.SELECT_SELF);
    }

    @AfterClass
    public static void tearDownTestSuite() throws Exception {
    }

    @Test
    public void testCanHandle() throws Exception {
        URI uri = new URI("file:///tmp/file.xml");
        Assert.assertTrue(repositoryTransport.canHandle(uri));

        uri = new URI("http://localhost/file.xml");
        Assert.assertFalse(repositoryTransport.canHandle(uri));
    }

    @Test
    public void testLoadContent() throws Exception {
        URI uri = new URI(testFile);
        byte[] content = repositoryTransport.loadContent(uri);
        Assert.assertNotNull(content);
    }
}