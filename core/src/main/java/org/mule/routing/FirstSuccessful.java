/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * FirstSuccessful is a message processor that has a list of target MPs.  Each reveiced event is routed to each
 * target, in order,  until one succeeds by not throwing an exception.
 */
package org.mule.routing;

import org.mule.DefaultMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.routing.outbound.AbstractOutboundRouter;


/**
 *  FirstSuccessful routes an event to the first target route that can accept it without throwing or returning an 
 * exception.  If no such route can be found, an exception is thrown. Note that this works more reliable with
 * synchronous targets, but no such restriction is imposed.
 */
public class FirstSuccessful extends AbstractOutboundRouter implements MessageProcessor
{
    /**
     * Route the given event to one of our targets
     */
    @Override
    public MuleEvent route(MuleEvent event) throws MessagingException
    {
        MuleEvent retval = null;

        boolean failed = true;
        for (MessageProcessor mp : routes)
        {
            try
            {
                MuleEvent toProcess = event;
                if (mp instanceof OutboundEndpoint)
                {
                    toProcess = new DefaultMuleEvent(event.getMessage(), (OutboundEndpoint)mp, event.getSession());
                }
                retval = mp.process(toProcess);
                if (retval == null)
                {
                    failed = false;
                }
                else
                {
                    MuleMessage msg = retval.getMessage();
                    failed = msg != null && msg.getExceptionPayload() != null;
                }
            }
            catch (Exception ex)
            {
                failed = true;
            }
            if (!failed)
                break;
        }

        if (failed)
        {
            throw new CouldNotRouteOutboundMessageException(event, this);
        }

        return retval;
    }

    public boolean isMatch(MuleMessage message) throws MuleException
    {
        return true;
    }
}