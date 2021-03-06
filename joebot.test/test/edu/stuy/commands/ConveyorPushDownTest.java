/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stuy.commands;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import edu.stuy.*;
import edu.stuy.commands.*;
import static edu.stuy.assertions.AcquirerAssertions.*;
import static edu.stuy.assertions.ConveyorAssertions.*;
import static edu.stuy.commands.util.ConveyorTools.*;
import static edu.stuy.commands.util.AcquirerTools.*;

/**
 *
 * @author Q
 */
public class ConveyorPushDownTest {

    public ConveyorPushDownTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        InitTests.setUpTests(InitTests.NO_PHYSICS);
        JoeBot j = new JoeBot();
        j.robotInit();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        InitTests.tearDownTests();
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}

    @Test
    public void testSubsystemsExist(){
        assertNotNull(CommandBase.acquirer);
        assertNotNull(CommandBase.conveyor);
    }

    @Test
    public void testBallNotAtBottom(){
        lowerSensorDoesNotSense();
        ConveyorPushDown cmd = new ConveyorPushDown();
        cmd.execute();
        assertConveyorIsRunningBackwards();
    }

    @Test
    public void testBallAtBottom(){
        lowerSensorDoesSense();
        ConveyorPushDown cmd = new ConveyorPushDown();
        cmd.execute();
        assertConveyorIsNotRunning();
    }

    @Test
    public void testInit(){
        ConveyorPushDown cmd = new ConveyorPushDown();
        runCommandOnce(cmd);
        runCommandOnce(cmd);
    }

    private void runCommandOnce(ConveyorPushDown cmd){
        startAcquirerForward();
        cmd.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(ConveyorPushDownTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        cmd.end();
        assertAcquirerIsNotRunning();
    }
}