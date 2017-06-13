package com.snapswap.obp

import org.scalatest.{Matchers, OneInstancePerTest, WordSpec}
import spray.json._
import OBP._

class ObpSpec
  extends WordSpec
    with Matchers
    with OneInstancePerTest {

  "model" should {
    "correct parse add account request" in {
      val json = addAccountRaw.parseJson
      val obj = json.convertTo[CreateAccount]

      obj.label shouldBe "Test Account"
    }
  }

  private val addAccountRaw =
    """{"user_id":"38a16668-351f-44ee-8d74-24e710774af2","label":"Test Account","type":"CURRENT","balance":{"currency":"EUR","amount":"0"},"branch_id":"1234","account_routing":{"scheme":"OBP","address":"UK123456"}}"""
}