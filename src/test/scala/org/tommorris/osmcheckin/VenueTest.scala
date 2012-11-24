package org.tommorris.osmcheckin
import org.specs2.mutable._

class VenueSpec extends Specification {
  val example = <osm version="0.6">
    <node lon="-0.1430911" lat="51.5238028" id="105">
      <tag v="pub" k="amenity"></tag>
      <tag v="Potlatch 0.10b" k="created_by"></tag>
      <tag v="The Green Man" k="name"></tag>
    </node>
  </osm>

  "it should parse tags appropriately" should {
    val ven = Venue((example \ "node")(0), example)    
    ven.hasTag("amenity") must beTrue
    ven.hasTag("name") must beTrue
  }

  "it should return locations" should {
    val ven = Venue((example \ "node")(0), example)
    ven.location() must beEqualTo(Pair(51.5238028.toDouble, -0.1430911.toDouble))
  }
}

// vim: set ts=2 sw=2 et:
