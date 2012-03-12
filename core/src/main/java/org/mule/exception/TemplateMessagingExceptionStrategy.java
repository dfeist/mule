/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.exception;

import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.exception.MessagingExceptionHandlerAcceptor;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.management.stats.FlowConstructStatistics;
import org.mule.message.DefaultExceptionPayload;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.routing.requestreply.ReplyToPropertyRequestReplyReplier;

public abstract class TemplateMessagingExceptionStrategy extends AbstractExceptionListener implements MessagingExceptionHandlerAcceptor
{

    private MessageProcessorChain configuredMessageProcessors;
    private MessageProcessor replyToMessageProcessor = new ReplyToPropertyRequestReplyReplier();
    private String expression;
    private boolean handleException;

    final public MuleEvent handleException(Exception exception, MuleEvent event)
    {
        FlowConstruct flowConstruct = event.getFlowConstruct();
        fireNotification(exception);
        logException(exception);
        processStatistics(event);
        event.getMessage().setExceptionPayload(new DefaultExceptionPayload(exception));
        event = beforeRouting(exception, event);
        event = route(event, exception);
        processOutboundRouterStatistics(flowConstruct);
        event = afterRouting(exception, event);
        markExceptionAsHandledIfRequired(exception);
        if (event != null)
        {
            processReplyTo(event, exception);
            closeStream(event.getMessage());
            nullifyExceptionPayloadIfRequired(event);
        }
        return event;
    }

    protected void markExceptionAsHandledIfRequired(Exception exception)
    {
        if (exception instanceof MessagingException && handleException)
        {
            ((MessagingException)exception).setHandled(handleException);
        }
    }

    protected void processReplyTo(MuleEvent event, Exception e)
    {
        try
        {
            replyToMessageProcessor.process(event);
        }
        catch (MuleException ex)
        {
            logFatal(event,ex);
        }
    }

    protected void nullifyExceptionPayloadIfRequired(MuleEvent event)
    {
    }

    private void processStatistics(MuleEvent event)
    {
        FlowConstructStatistics statistics = event.getFlowConstruct().getStatistics();
        if (statistics != null && statistics.isEnabled())
        {
            statistics.incExecutionError();
        }
    }

    protected MuleEvent route(MuleEvent event, Exception t)
    {
        if (!getMessageProcessors().isEmpty())
        {
            try
            {
                event.getMessage().setExceptionPayload(new DefaultExceptionPayload(t));
                MuleEvent result = configuredMessageProcessors.process(event);                
                return result;
            }
            catch (Exception e)
            {
                logFatal(event, e);
            }
        }
        return event;
    }


    @Override
    protected void doInitialise(MuleContext muleContext) throws InitialisationException
    {
        super.doInitialise(muleContext);
        DefaultMessageProcessorChainBuilder defaultMessageProcessorChainBuilder = new DefaultMessageProcessorChainBuilder(this.flowConstruct);
        try
        {
            configuredMessageProcessors = defaultMessageProcessorChainBuilder.chain(getMessageProcessors()).build();
        }
        catch (MuleException e)
        {
            throw new InitialisationException(e, this);
        }
    }


    public void setExpression(String expression)
    {
        this.expression = expression;
    }

    public boolean accept(MuleEvent event)
    {
        return acceptsAll() || muleContext.getExpressionManager().evaluateBoolean(expression,event.getMessage());
    }

    @Override
    public boolean acceptsAll()
    {
        return expression == null;
    }

    protected MuleEvent afterRouting(Exception exception, MuleEvent event)
    {
        return event;
    }

    protected MuleEvent beforeRouting(Exception exception, MuleEvent event)
    {
        return event;
    }

    public void setHandleException(boolean handleException)
    {
        this.handleException = handleException;
    }
}