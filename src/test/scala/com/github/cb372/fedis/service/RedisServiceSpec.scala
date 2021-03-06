package com.github.cb372.fedis.service

import org.scalatest._
import org.scalatest.matchers._
import com.github.cb372.fedis.{Session, SessionAndCommand}
import com.twitter.finagle.redis.protocol._
import com.twitter.finagle.Service
import com.github.cb372.fedis.db.DummyKeyValueStoreTask
import com.twitter.util.{Future, FuturePool, NullTimer}
import com.twitter.finagle.redis.util.{CBToString, StringToBuf, StringToChannelBuffer}
import com.github.cb372.fedis.util.ImplicitConversions._

class RedisServiceSpec extends FlatSpec with ShouldMatchers {

  trait Fixture {
    val mockFuturePool = FuturePool.immediatePool
    val mockTimer = new NullTimer
    val mockReaper = new DummyKeyValueStoreTask

    def createService = new RedisService(mockFuturePool, mockTimer, mockReaper)
  }

  behavior of "RedisService"

  it should "have a db with index 0" in new Fixture {
    val service = createService
    val cmd = SessionAndCommand(Session(false, 0), Exists(StringToBuf("foo")))
    val reply = service.apply(cmd)
    reply.toJavaFuture.get() should equal(IntegerReply(0))
  }

  it should "have a db with index 15" in new Fixture {
    val service = createService
    val cmd = SessionAndCommand(Session(false, 15), Exists(StringToBuf("foo")))
    val reply = service.apply(cmd)
    reply.toJavaFuture.get() should equal(IntegerReply(0))
  }

  it should "hold separate state for each DB" in new Fixture {
    val service = createService
   // val key = StringToChannelBuffer("foo")
    val key = StringToBuf("foo")
    val value = "abc"

    // SET foo abc in DB 1
    service.apply(
      //SessionAndCommand(Session(false, 1), Set(key, StringToBuf(value)))
      SessionAndCommand(Session(false, 1), Set(key, StringToBuf(value)))
    )

    // GET foo in DBs 1 and 2
    val getInDb1 = service.apply(
      SessionAndCommand(Session(false, 1), Get(key))
    )
    val getInDb2 = service.apply(
      SessionAndCommand(Session(false, 2), Get(key))
    )

    val db1Val = CBToString(getInDb1.toJavaFuture.get.asInstanceOf[BulkReply].message)
    db1Val should equal(value)
    getInDb2.toJavaFuture.get should equal(EmptyBulkReply())
  }
}
