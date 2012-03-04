package net.noerd.versioned

import java.net.InetSocketAddress

import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelFuture
import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ChannelStateEvent
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.SimpleChannelUpstreamHandler
import org.jboss.netty.channel.socket.ClientSocketChannelFactory

import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.socket.ClientSocketChannelFactory
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory


import org.jboss.netty.channel.Channels.pipeline
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.channel.socket.ClientSocketChannelFactory

/**
 * The ProxyChannelHandler is the netty handler that takes care of the
 * actual proxy work, setting up connection and so on.
 *
 * The implementation of the ProxyChannelHandler was very much inspired by
 * http://netty.io/docs/stable/xref/org/jboss/netty/example/proxy/HexDumpProxyInboundHandler.html
 */
class ProxyChannelHandler(
    var targetPort: Option[ Int ], 
    clientSocketChannelFactory: ClientSocketChannelFactory
)
extends SimpleChannelUpstreamHandler 
with ForwardingChannelHandler {

    private val targetHost: String = "localhost"
    
    protected def outboundChannel( context: Channel ): Channel = context    
        
    override def channelOpen( ctx: ChannelHandlerContext, e: ChannelStateEvent ): Unit = {

        val inboundChannel = e.getChannel;
        
        // if there is no target port defined
        // we close the connection directly
        if( !targetPort.isDefined ) {
            inboundChannel.close()
            return
        }
                
        // Suspend incoming traffic until connected to the remote host.
        inboundChannel.setReadable( false )
        
        val outbound = setupOutboundChannel( inboundChannel )
        ctx.setAttachment( outbound.getChannel ) 
        outbound.addListener( new ChannelFutureListener {
            override def operationComplete( future: ChannelFuture ) {
                if ( future.isSuccess ) inboundChannel.setReadable( true )
                else inboundChannel.close()
            }
        } )
    }
    
    private def setupOutboundChannel( inboundChannel: Channel ): ChannelFuture = {

        val cb = new ClientBootstrap( clientSocketChannelFactory )
        cb.getPipeline.addLast( "handler", new OutboundHandler( inboundChannel ) )
        cb.connect( new InetSocketAddress( targetHost, targetPort.get ) )        
    }
}

class OutboundHandler( protected val outboundChannel: Channel ) 
extends SimpleChannelUpstreamHandler
with ForwardingChannelHandler {
    
    protected def outboundChannel( context: Channel ): Channel = outboundChannel
}

trait ForwardingChannelHandler { self: SimpleChannelUpstreamHandler =>
    
    protected def outboundChannel( context: Channel ): Channel
    
    override def messageReceived( ctx: ChannelHandlerContext, e: MessageEvent): Unit = {
        
        val outbound = outboundChannel( ctx.getAttachment.asInstanceOf[ Channel ] )
        val msg = e.getMessage.asInstanceOf[ ChannelBuffer ]

        outbound.write( msg )

        if( !outbound.isWritable ) {
            e.getChannel.setReadable( false )
        }
    }
    
    override def channelInterestChanged( ctx: ChannelHandlerContext, e: ChannelStateEvent ): Unit = {

        val outbound = outboundChannel( ctx.getAttachment.asInstanceOf[ Channel ] )
        
        if( e.getChannel.isWritable ) {
            outbound.setReadable( true )
        }
    }
    
    override def channelClosed( ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit = {

        closeOnFlush( outboundChannel( ctx.getAttachment.asInstanceOf[ Channel ] ) )
    }
    
    override def exceptionCaught( ctx: ChannelHandlerContext, e: ExceptionEvent): Unit = {

        e.getCause.printStackTrace()
        closeOnFlush( e.getChannel )
    }
        
    private def closeOnFlush( channel: Channel ) {

        if ( channel.isConnected ) {
            channel.write( ChannelBuffers.EMPTY_BUFFER ).addListener( ChannelFutureListener.CLOSE )
        }
    }    
}
