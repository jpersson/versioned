package net.noerd.versioned

import org.scalatra._
import net.liftweb.json._
import net.liftweb.json.Serialization.{ read, write }

class VersionedServlet extends ScalatraServlet {
    
    private val registry = ServiceProxyRegistry()
    implicit private val formats = Serialization.formats( NoTypeHints )
    
    val Services = "/services"
    val Service = "/services/:serviceID"
    val Versions = "/services/:serviceID/versions"
    val Version = "/services/:serviceID/versions/:versionID"
    val ActiveVersion = "/services/:serviceID/active"

    get( Services ) {
      
        write( registry.services )
    }
    
    post( Services ) {

        registry.register( read[ Service ]( request.body ) )
    }    

    get( Service ) {
      
        serviceProxy( params( "serviceID" ) ).map { service =>
            write( service )
        }.getOrElse( halt( 404 ) )
    }
    
    delete( Service ) {
        
        halt( 501 )
    }
    
    get( Versions ) {
      
        serviceProxy( params( "serviceID" ) ).map { service =>
            Some( write( service.versions ) )
        }.getOrElse( halt( 404 ) )
    }

    post( Versions ) {
        serviceProxy( params( "serviceID" ) ).map { service =>
            service.register( read[ Version]( request.body ) )
        }.getOrElse( halt( 404 ) )
    }    

    get( Version ) {

        serviceProxy( params( "serviceID" ) ).flatMap { service =>
            service.versions.find( _.id == params( "versionID" ) ).flatMap{ version =>
                Some( write( version ) )
            }
        }.getOrElse( halt( 404 ) )
    }
    
  
    get( ActiveVersion ) {
        
        serviceProxy( params( "serviceID" ) ).flatMap { service =>
            service.active.flatMap{ version =>
                Some( write( version ) )
            }
        }.getOrElse( halt( 404 ) )
    }  
    
    put( ActiveVersion ) {
        
        serviceProxy( params( "serviceID" ) ).map { service =>
            service.activate( read[ Version]( request.body ) )
        }.getOrElse( halt( 404 ) )
    }
    
    private def serviceProxy( id: String ): Option[ ServiceProxy ] = registry.proxy( id )   
}