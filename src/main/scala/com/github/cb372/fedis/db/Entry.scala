package com.github.cb372.fedis.db

import com.twitter.util.Time

/**
 * Author: chris
 * Created: 6/5/12
 */

trait HasExpiry {
  val expiresAt: Option[Time]
}

sealed trait Entry extends HasExpiry

case class Str(value: Array[Byte], expiresAt: Option[Time] = None) extends Entry
