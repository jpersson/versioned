package net.noerd.versioned

import java.net.InetSocketAddress
import java.util.concurrent.Executor

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


class SocketProxy( val localPort: Int, val executor: Executor ) {
    
    private var _targetPort: Option[ Int ] = None
    private val serverBootstrap = new ServerBootstrap( 
        new NioServerSocketChannelFactory( executor, executor )
    )
    private val clientSocketChannelFactory = (
        new NioClientSocketChannelFactory( executor, executor )
    )
    private val proxyChannelHandler = (
        new ProxyChannelHandler( targetPort, clientSocketChannelFactory )
    )
    private val channel = {
        serverBootstrap.setPipelineFactory( newChannelPipelineFactory )
        serverBootstrap.bind( new InetSocketAddress( localPort ) )
    }
    
    def targetPort: Option[ Int ] = _targetPort
    
    def targetPort_=( port: Option[ Int ] ): Unit = {
        
        proxyChannelHandler.targetPort = port
        _targetPort = port
    }
    
    def shutdown() = {
        
        channel.unbind.await
    }
    
    private def newChannelPipelineFactory = new ChannelPipelineFactory {
        override def getPipeline: ChannelPipeline = {
            
            val p = pipeline()
            p.addLast("handler", proxyChannelHandler )
            p
        }
    }
}

object SocketProxy {
    
    def apply( localPort: Int, executor: Executor ): SocketProxy = {
        
        new SocketProxy( localPort, executor )
    }
}