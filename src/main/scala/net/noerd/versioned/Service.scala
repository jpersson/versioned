package net.noerd.versioned

import java.net.InetSocketAddress
import java.util.concurrent.Executor
import java.util.concurrent.Executors

import scala.collection.mutable.HashSet
import scala.collection.mutable.{ Set => MSet }
import scala.collection.{ Set => CSet }

case class Service( id: String, port: Int )
case class Version( id: String, port: Int )

/**
 * The ServiceProxy is a socket proxy for the service. When created
 * it sets up a server socket on the service port which can be used 
 * as the external port and proxies all socket communication to the 
 * local port specified by the active version.
 *
 * @param service being proxied
 * @param executor handling all requests
 */
class ServiceProxy( val service: Service, executor: Executor ) {

    private var activeVersion: Option[ Version ] = None
    private val socketProxy = new SocketProxy( service.port, executor )
    private val _versions: MSet[ Version] = new HashSet[ Version ]
    
    /**
     * Returns the active version if one is available
     */
    def active: Option[ Version ] = activeVersion
    
    /**
     * Returns all registrered versions
     */ 
    def versions: CSet[ Version ] = _versions

    /**
     * Registers the given version with the service, but will 
     * not make the version active. See activate() for that
     */
    def register( version: Version ): Unit = _versions += version
    
    /**
     * Makes the given version the active one which will be used
     * for all new incoming request to the service. The previously
     * active version will stay around until all requests have been 
     * processed.
     */
    def activate( version: Version ): Unit = {
        
        if( !isActive( version ) ) {
            register( version )
            activeVersion = Some( version )
            socketProxy.targetPort = activeVersion.map( _.port )
        }
    }
    
    private def isActive( version: Version ): Boolean = {
    
        activeVersion.exists( _.equals( version ) )
    }
    
    /**
     * Will shutdown the servet socket and close all current connections
     * making the proxied service unavailable.
     */
    def shutdown() = {
        
        socketProxy.shutdown()
    }
}

object ServiceProxy {
    
    /**
     * Creates a new ServiceProxy for the given service using a new
     * executor from Executors.newCachedThreadPool().
     */
    def apply( service: Service ): ServiceProxy = {
        
        new ServiceProxy( service, Executors.newCachedThreadPool() )
    }
}

