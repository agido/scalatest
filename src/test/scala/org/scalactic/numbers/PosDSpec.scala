/*
 * Copyright 2001-2014 Artima, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scalactic.numbers

import org.scalatest._
import scala.collection.mutable.WrappedArray
import OptionValues._
import org.scalactic.StrictCheckedEquality._

class PosDSpec extends Spec with Matchers {

  object `A PosD` {
    object `should offer a from factory method that` {
      def `returns Some[PosD] if the passed Double is greater than 0`
      {
        PosD.from(50.23).value.value shouldBe 50.23
        PosD.from(100.0).value.value shouldBe 100.0
      }
      def `returns None if the passed Double is NOT greater than 0`
      {
        PosD.from(0.0) shouldBe None
        PosD.from(-0.00001) shouldBe None
        PosD.from(-99.9) shouldBe None
      }
    } 
    def `should have a pretty toString` {
      PosD.from(42.0).value.toString shouldBe "PosD(42.0)"
    }
  }
}

