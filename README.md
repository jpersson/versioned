Versioned
=========

This is so far nothing more than an experiment of a proxy for processes that deal with requests over sockets. The goal is something that will ease deployment of new versions of such such process.

## Background

Having several processes dealing with requests over http, smtp or whatever socket protocol is often a pain when it comes to deploying new versions of them. The machine can of course be taken out of the load balancer during deployment but it's not always that easy. You still have to make sure that requests currently being dealt with are allowed to finish before taking down the old version and start the new one. 

## Idea

Wouldn't it be nice to have a local proxy on the machine that knew about all services and their versions. A process and its version could be registered and activated on the fly. 

When a new version was deployed the old one could live until it had no more requests to deal with and them terminate. 

## State

As mentioned above Versioned is so far only an experiment and not to be used
in any other way.

Integration
-----------

All integration is done using the rest-interface which allows a service to register itself, add versions as well as changing the active one. 

```
/services
/services/:serviceID
/services/:serviceID/active
/services/:serviceID/versions
/services/:serviceID/versions/:versionID
```
    
Dependencies
------------

## 3rd Party libs

I've tried to keep the list of dependencies as short as possible but currently the following
libraries are being used.

* [netty](http://netty.io) the wonderful netty lib is used for all socket stuff
* [scalatra](http://scalatra.org) is used for the rest interface
* [lift-json](https://github.com/lift/lift/tree/master/framework/lift-base/lift-json/) for everything json 

## Testing

For testing I use [scala-test](http://www.scalatest.org) for unit-tests.

Feedback
--------

If you have any questions or feedback just send me a message here or on [twitter](http://twitter.com/suraken) and if you want to contribute just send a pull request.

License
-------

Versioned is licensed under the [wtfpl](http://sam.zoy.org/wtfpl/).
