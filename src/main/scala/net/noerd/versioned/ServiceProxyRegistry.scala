package net.noerd.versioned

import scala.collection.mutable.HashSet
import scala.collection.mutable.{ Set => MSet }

/**
 * Just a quick hack to get the rest-resources up and running.
 * Need to be reworked.
 */
class ServiceProxyRegistry {
    
    private val proxies: MSet[ ServiceProxy ] = new HashSet

    def services: Seq[ Service ] = proxies.map( _.service ).toSeq
    
    def proxy( id: String ): Option[ ServiceProxy ] = {
        
        proxies.find( _.service.id == id )
    }
    
    def register( service: Service ): Unit = {
        
        proxies += ServiceProxy( service )
    }
}

object ServiceProxyRegistry {
    
    private val instance = new ServiceProxyRegistry
    
    def apply(): ServiceProxyRegistry = instance
}
