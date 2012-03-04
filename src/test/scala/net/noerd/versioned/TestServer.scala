package net.noerd.versioned

import java.util.concurrent.Executors
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicLong

import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.ChannelStateEvent
import org.jboss.netty.channel.SimpleChannelUpstreamHandler
import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.Channels

class TestServer( val port: Int ) {
   
    private val bootstrap = new ServerBootstrap(
        new NioServerSocketChannelFactory(
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool()
        )
    )
    private val handler = new TestServerHandler
    private val channel = {
        bootstrap.setPipelineFactory( new ChannelPipelineFactory() {
            override def getPipeline(): ChannelPipeline = {
            
                Channels.pipeline( handler )
            }
        } )
        bootstrap.bind( new InetSocketAddress( port ) )
    }
    
    def nbrOfSessions: Long = handler.nbrOfSessions
    
    def shutdown(): Unit = {
        
        channel.unbind.await
    }		    
}
 
class TestServerHandler extends SimpleChannelUpstreamHandler {
    
    private val sessionCounter = new AtomicLong
    
    def nbrOfSessions: Long = sessionCounter.get

    override def channelOpen( ctx: ChannelHandlerContext, e: ChannelStateEvent) = {
        sessionCounter.incrementAndGet
    }

    override def messageReceived( ctx: ChannelHandlerContext, e: MessageEvent) = {

        e.getChannel.write( e.getMessage() )
    }

    override def exceptionCaught( ctx: ChannelHandlerContext, e: ExceptionEvent ) = {
        
        e.getCause.printStackTrace
        e.getChannel.close()
    }
}