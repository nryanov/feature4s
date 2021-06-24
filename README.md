# feature4s
[![GitHub license](https://img.shields.io/github/license/nryanov/feature4s)](https://github.com/nryanov/feature4s/blob/master/LICENSE.txt)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.nryanov.feature4s/feature4s-core_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.nryanov.feature4s/feature4s-core_2.13)

feature4s is a small library which implement the [Feature Toggles](https://martinfowler.com/articles/feature-toggles.html) pattern for scala.

## Get started
```sbt
libraryDependencies ++= Seq(
  "com.nryanov.feature4s" %% "<feature4s-backend>" % "[version]" 
)
```

```scala
import feature4s.redis.lettuce._
import io.lettuce.core.RedisClient

object Application {
  def main(args: Array[String]): Unit = {
    val client = RedisClient.create(s"redis://localhost:6379")
    val namespace = "features"
    val featureRegistry = LettuceSyncFeatureRegistry.useConnection(client.connect(), namespace)
   
    val myFeature = featureRegistry.register("featureName", enable = false, Some("description"))
   
    if (myFeature.isEnable()) {
      // if feature is enable logic
    } else {
      // if feature is disable logic    
    } 
    
    client.close()
  }
}
```

### OpenAPI documentation 
API for feature4s is build using [tapir](https://github.com/softwaremill/tapir). 
Choose a preferred backend and json-lib and then add the following dependencies: 
```sbt
libraryDependencies ++= Seq(
  "com.nryanov.feature4s" %% "<feature4s-tapir-backend>" % "[version]"
  "com.nryanov.feature4s" %% "<feature4s-tapir-json>" % "[version]" 
)
```

**Supported http servers**
- [akka](https://github.com/akka/akka-http)
- [http4s](https://github.com/http4s/http4s) (+ zio-http4s)

For more information about using these servers with [tapir](https://github.com/softwaremill/tapir) see: [doc](https://tapir.softwaremill.com/en/latest/server/logic.html)

**Supported json integrations**
- [circe](https://github.com/circe/circe)
- [json4s](https://github.com/json4s/json4s) 
- [spray-json](https://github.com/spray/spray-json)
- [tethys](https://github.com/tethys-json/tethys)

Json codecs for internal feature4s structures can be added like this:
```scala
import features.tapir.json.[circe/json4s/sprayjson/tethys]._
```  

For json4s you also need to choose serialization backend: native or jackson.
```sbt
libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-native" % "{latestVersion}"
// or
  "org.json4s" %% "json4s-jackson" % "{latestVersion}" 
)
``` 

After that just add a format:
```scala
implicit val formats: Formats = Serialization.formats(NoTypeHints)
``` 

### Examples
`Hello world` examples with OpenAPI docs can be found in the [examples](examples/) folder.
 
## Backends
Backend | Client | 
------------ | ------------- 
Aerospike | [aerospike-client-java](https://github.com/aerospike/aerospike-client-java) 
Redis | [jedis](https://github.com/redis/jedis) <br> [lettuce](https://github.com/lettuce-io/lettuce-core) <br> [redisson](https://github.com/redisson/redisson)
Zookeeper | [curator](https://github.com/apache/curator)

## Implementations
Class | Effect | 
------------ | ------------- 
`AerospikeSyncFeatureRegistry` | None (`Identity`) 
`AerospikeCatsFeatureRegistry` | `F[_]: cats.effect.Sync: cats.effect.ContextShift` 
`AerospikeZioFeatureRegistry` | `zio.Task`
`JedisClusterSyncFeatureRegistry` | None (`Identity`)   
`JedisClusterCatsFeatureRegistry` | `F[_]: cats.effect.Sync: cats.effect.ContextShift` 
`JedisClusterZioFeatureRegistry` | `zio.Task`   
`JedisSyncFeatureRegistry (JedisPool && JedisSentinel)` | None (`Identity`)   
`JedisCatsFeatureRegistry (JedisPool && JedisSentinel)` | `F[_]: cats.effect.Sync: cats.effect.ContextShift` 
`JedisZioFeatureRegistry (JedisPool && JedisSentinel)` | `zio.Task` 
`LettuceSyncFeatureRegistry` | None (`Identity`)  
`LettuceAsyncFeatureRegistry` | `scala.concurrent.Future` 
`LettuceCatsFeatureRegistry` | `F[_]: cats.effect.Sync: cats.effect.ContextShift` 
`LettuceCatsAsyncFeatureRegistry` | `F[_]: cats.effect.Concurrent` 
`LettuceZioFeatureRegistry` | `zio.Task` 
`LettuceZioAsyncFeatureRegistry` | `zio.Task` 
`RedissonSyncFeatureRegistry` | None (`Identity`)  
`RedissonAsyncFeatureRegistry` | `scala.concurrent.Future` 
`RedissonCatsFeatureRegistry` | `F[_]: cats.effect.Sync: cats.effect.ContextShift` 
`RedissonCatsAsyncFeatureRegistry` | `F[_]: cats.effect.Concurrent` 
`RedissonZioFeatureRegistry` | `zio.Task` 
`RedissonZioAsyncFeatureRegistry` | `zio.Task`   
`ZookeeperSyncFeatureRegistry` | None (`Identity`)   
`ZookeeperCatsFeatureRegistry` | `F[_]: cats.effect.Sync: cats.effect.ContextShift` 
`ZookeeperZioFeatureRegistry` | `zio.Task`  
 