package com.avsystem.commons
package misc

import org.scalatest.FunSuite

class SealedEnumTest extends FunSuite {
  sealed trait SomeEnum
  object SomeEnum extends SealedEnumCompanion[SomeEnum] {
    case object First extends SomeEnum
    case object Second extends SomeEnum
    case object Third extends SomeEnum
    case object Fourth extends SomeEnum

    val values: List[SomeEnum] = caseObjects
  }

  test("case objects listing test") {
    import SomeEnum._
    assert(values.toSet == Set(First, Second, Third, Fourth))
  }

  sealed abstract class SomeNamedEnum(val name: String) extends NamedEnum
  object SomeNamedEnum extends NamedEnumCompanion[SomeNamedEnum] {
    case object First extends SomeNamedEnum("p")
    case object Second extends SomeNamedEnum("d")
    case object Third extends SomeNamedEnum("t")
    case object Fourth extends SomeNamedEnum("c")

    val values: List[SomeNamedEnum] = caseObjects
  }

  test("named enum map by name test") {
    import SomeNamedEnum._
    assert(byName == Map("p" -> First, "d" -> Second, "t" -> Third, "c" -> Fourth))
  }
}
