/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jbpm;

import org.mule.api.MuleMessage;
import org.mule.module.bpm.BPMS;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.bpm.ProcessConnector;

import org.jbpm.api.ProcessInstance;

/**
 * Tests the connector against jBPM with a simple process.
 * 
 * @deprecated It is recommended to configure BPM as a component rather than a transport for 3.x
 */
public class ForkedProcessTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "jbpm-functional-test.xml";
    }

    public void testForkedProcess() throws Exception 
    {
        ProcessConnector connector = (ProcessConnector) muleContext.getRegistry().lookupConnector("bpmConnector");
        BPMS bpms = connector.getBpms();
        assertNotNull(bpms);

        MuleClient client = new MuleClient(muleContext);
        try
        {
            // Create a new process.
            MuleMessage response = client.send("bpm://fork", "data", null);                      
            ProcessInstance process = (ProcessInstance) response.getPayload();
            
            // The process should be waiting for asynchronous responses from both services
            String state = (String) bpms.getState(process);
            assertTrue(state.contains("waitForResponseA"));
            assertTrue(state.contains("waitForResponseB"));

            Thread.sleep(2000);

            // ServiceA is initially stopped, so we're still waiting for a response from ServiceA
            process = (ProcessInstance) bpms.lookupProcess(process.getId());
            assertEquals("waitForResponseA", bpms.getState(process));

            // Start ServiceA
            muleContext.getRegistry().lookupService("ServiceA").resume();
            Thread.sleep(2000);
                        
            // The process should have ended.
            process = (ProcessInstance) bpms.lookupProcess(process.getId());
            assertTrue("Process should have ended, but is in state " + bpms.getState(process), 
                    bpms.hasEnded(process));
        }
        finally
        {
            client.dispose();
        }
    }
}