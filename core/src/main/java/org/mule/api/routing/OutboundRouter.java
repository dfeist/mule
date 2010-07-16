/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.routing;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transaction.TransactionConfig;

import java.util.List;

/**
 * <code>OutboundRouter</code> is used to control outbound routing behaviour for
 * an event. One or more Outbound routers can be associated with an
 * <code>OutboundRouterCollection</code> and will be selected based on the filters
 * set on the individual Outbound Router.
 * 
 * @see OutboundRouterCollection
 */

public interface OutboundRouter extends Router, MessageProcessor
{
    /**
     * Sets a list of MessageProcessor instances associated with this router
     * 
     * @param routes a list of MessageProcessor instances
     */
    void setRoutes(List<MessageProcessor> routes);

    /**
     * Gets a list of MessageProcessor instances associated with this router
     * 
     * @return a list of MessageProcessor instances
     */
    List<MessageProcessor> getRoutes();

    /**
     * Adds a route to this router
     * 
     * @param route the route to add to the router
     */
    void addRoute(MessageProcessor route);

    /**
     * Removes a specific route from the router
     * 
     * @param route the route to remove
     * @return true if the route was removed
     */
    boolean removeRoute(MessageProcessor route);

    /**
     * This method is responsible for routing the Message. The logic for this method
     * will change for each type of router depending on expected behaviour. For
     * example, a MulticastingRouter might just iterate through the list of
     * assoaciated routes sending the message. Another type of router such as the
     * ExceptionBasedRouter will hit the first route, if it fails try the second,
     * and so on. Most router implementations will extends the
     * FilteringOutboundRouter which implements all the common logic need for a
     * router.
     * 
     * @param event the event to send via one or more routes on this router
     * @throws MessagingException if any errors occur during the sending of messages
     * @see org.mule.routing.outbound.FilteringOutboundRouter
     * @see org.mule.routing.outbound.ExceptionBasedRouter
     * @see org.mule.routing.outbound.MulticastingRouter
     */
    public MuleEvent process(MuleEvent event) throws MuleException;

    /**
     * Determines if the event should be processed by this router. Routers can be
     * selectively invoked by configuring a filter on them. Usually the filter is
     * applied to the message when calling this method. All core Mule outbound
     * routers extend the FilteringOutboundRouter router that handles this method
     * automatically.
     * 
     * @param message the current message to evaluate
     * @return true if the event should be processed by this router
     * @throws MessagingException if the event cannot be evaluated
     * @see org.mule.routing.inbound.SelectiveConsumer
     */
    boolean isMatch(MuleMessage message) throws MessagingException;

    TransactionConfig getTransactionConfig();

    void setTransactionConfig(TransactionConfig transactionConfig);

    /**
     * Gets the replyTo route for any outgoing messages. This will then be used by
     * other Mule routers to send replies back for this message. If the underlying
     * protocol supports replyTo messages, such as Jms, a Jms Destination will be
     * attached to the outbound message
     * 
     * @return the replyTo route or null if one has not been set.
     */
    String getReplyTo();

    /**
     * Sets the replyTo route for any outgoing messages. This will then be used by
     * other Mule routers to send replies back for this message. If the underlying
     * protocol supports replyTo messages, such as Jms, a Jms Destination will be
     * attached to the outbound message
     * 
     * @param replyTo route string to use
     */
    void setReplyTo(String replyTo);

    /**
     * Determines whether this router supports dynamic route. i.e. routes that
     * are not configured at design time. routes might be pulled from the message
     * or payload.
     */
    boolean isDynamicRoutes();

    /**
     * @param name the route identifier
     * @return the route or null if the route is not registered
     */
    MessageProcessor getRoute(String name);
    
    /**
     * Determines is this router requires a new message copy.
     */
    boolean isRequiresNewMessage();

}
