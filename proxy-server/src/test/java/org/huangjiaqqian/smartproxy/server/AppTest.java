package org.huangjiaqqian.smartproxy.server;

import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import com.jfinal.template.Engine;
import com.jfinal.template.Template;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     * @throws UnsupportedEncodingException 
     */
    public void testApp() throws UnsupportedEncodingException
    {
    	Engine engine = Engine.use();
    	engine.setDevMode(true);
    	engine.setBaseTemplatePath(System.getProperty("user.dir") + "/webapps/templates");
    	Template template = engine.getTemplate("index.html");
    	Writer writer = new OutputStreamWriter(System.out, "UTF-8");
    	template.render(writer);
        assertTrue( true );
    }
}
