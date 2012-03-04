package net.noerd.versioned

import scala.util.Random

import java.io._
import java.net._
import java.net.InetSocketAddress

import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers

class ServiceProxySpec extends FlatSpec with ShouldMatchers with BeforeAndAfterEach {

	behavior of "ServiceProxy"
	
	it should "listen to port defined by the service" in {
        
        val service = Service( "echo", 9999 )
        
        usingServer { server =>
            usingProxy( service, server.port ) { proxy =>
                usingClient( service.port ) { client =>
                
                    val response = client <<! "foo"
                
                    response should equal ( "foo" )
                } 
            }
        }
	}

	it should "be at the most 2.5 slower than direct connections" in {
        
        val service = Service( "echo", 9999 )
        val nbrOfClients = 1000
        
        val directTiming = usingServer { server =>
            timeClientExecution( server.port, nbrOfClients )
        }
        val proxiedTiming = usingServer { server =>
            usingProxy( service, server.port ) { proxy =>
                timeClientExecution( service.port, nbrOfClients )
            }
        }
        val threshold = (directTiming * 2.5).toLong
        
        proxiedTiming should be < (threshold)
	}
    
	it should "enable switching to a different version" in {

        val service = Service( "echo", 9999 )
        
        usingServer { server1 =>
        usingServer { server2 =>
            usingProxy( service, server1.port ) { proxy =>

                timeClientExecution( service.port, 50 )
                
                proxy.activate( Version( "server2", server2.port ) )
                
                timeClientExecution( service.port, 25 )
                
                server1.nbrOfSessions should be (50)
                server2.nbrOfSessions should be (25)
            }
        } }
    }

	it should "remove an old version after it has finshed its requests" in {
        
        assert( false )
    }
}

object timeClientExecution {

    def apply( port: Int, nbrOfClients: Int ): Long = {

        val executor = Executors.newFixedThreadPool( 50 )
        val timeout = 10 * nbrOfClients
        println("Starting test with %d clients (max %d ms)".format( nbrOfClients, timeout ) )
        val started = System.currentTimeMillis
        
        0.until( nbrOfClients ).foreach( i => submitClient( port, executor ) )
        executor.shutdown()
        executor.awaitTermination( timeout, TimeUnit.MILLISECONDS )
        
        val finished = System.currentTimeMillis
        val timing = finished - started
        
        println( "Result: %d ms".format( timing ) )
        timing
    }
    
    private def submitClient( port: Int, executor: ExecutorService ) = {

        executor.submit( new Runnable {
            def run {
                try {
                    usingClient( port ) { client =>
                        val response = client <<! "foo"
                    }
                }
                catch {
                    case e:Exception => {
                        e.printStackTrace
                    }
                }
            }
        } )    
    }
}

object usingServer {
    
    private val random = new scala.util.Random
    private val portRange = 1000 to 9999
    
	def apply[ T ]( block: (TestServer) => T ) = {
		
		val server = new TestServer( newRandomPort )
		
		try {
			block( server )
		}
		finally {
			server.shutdown()
		}
	}
    
    private def newRandomPort: Int = {
        
        portRange( random.nextInt( portRange.length ) )
    }

}

object usingProxy {
    
    val executors = Executors.newCachedThreadPool()
	
	def apply[ T ]( service: Service, targetPort: Int )
    ( block: ( ServiceProxy ) => T ) = {
		
		val proxy = new ServiceProxy( service, executors )
        
        proxy.activate( Version( "version", targetPort ) )
		
		try {
			block( proxy )
		}
		finally {
			proxy.shutdown()
		}
	} 
}

object usingClient {

	def apply[ T ]( port: Int )( block:( Client ) => T ) = {
		
		val client = new Client( port )
        
		try {
			block( client )
		}
		finally {
            client.close()
		}
	}
}


class Client( val port: Int ) {
	
	private val socket = new Socket( "localhost", port )
	private val reader = new BufferedReader( new InputStreamReader( socket.getInputStream ) )
	private val writer = new BufferedWriter( new OutputStreamWriter( socket.getOutputStream ) )

	def <<( line: String ): Unit = {
        
		writer.append( line )
		writer.newLine
		writer.flush
	}
	
	def <<!( line: String ): String = {
        
		this << line
        reader.readLine
	}
		
	def closed: Boolean = socket.isClosed
	def connected: Boolean = socket.isConnected
    def close() = socket.close
}
