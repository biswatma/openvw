package dev.openvw.data

import dev.openvw.data.CarLook.Paint
import org.junit.Assert.assertEquals
import org.junit.Test

class CarLookTest {
  private fun p(c: String) = CarLook.paint(c)

  @Test fun whiteWithBlackRoofIsWhite() = assertEquals(Paint.WHITE, p("Candy White Deep/Black Pearlescent"))
  @Test fun plainWhiteIsWhite() = assertEquals(Paint.WHITE, p("Candy White"))
  @Test fun silverIsWhite() = assertEquals(Paint.WHITE, p("Reflex Silver"))
  @Test fun blackIsBlack() = assertEquals(Paint.BLACK, p("Deep Black Pearl"))
  @Test fun blackFirstThenWhiteIsBlack() = assertEquals(Paint.BLACK, p("Deep Black/Candy White"))
  @Test fun carbonSteelIsGrey() = assertEquals(Paint.GREY, p("Carbon Steel Grey Matte"))
  @Test fun cherryIsRed() = assertEquals(Paint.RED, p("Wild Cherry Red"))
  @Test fun kingsRedIsRed() = assertEquals(Paint.RED, p("Kings Red Metallic"))
  @Test fun redWithBlackRoofIsRed() = assertEquals(Paint.RED, p("Wild Cherry Red/Deep Black"))
  @Test fun risingBlueIsBlue() = assertEquals(Paint.BLUE, p("Rising Blue Metallic"))
  @Test fun curcumaIsYellow() = assertEquals(Paint.YELLOW, p("Curcuma Yellow"))
  @Test fun unknownColourUsesWhite() = assertEquals(Paint.WHITE, p("Lava Orange"))
  @Test fun emptyUsesWhite() = assertEquals(Paint.WHITE, p(""))
  @Test fun manualPickWins() {
    Store.paintPick = "BLUE"
    try { assertEquals(Paint.BLUE, CarLook.effective("Candy White")) } finally { Store.paintPick = "" }
  }
}
