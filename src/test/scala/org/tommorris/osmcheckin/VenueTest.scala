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
  val ven = Venue((example \ "node")(0), example)    

  val venBarXml = <osm version="0.6">
    <node lon="-0.1430911" lat="51.5238028" id="106" fake="yes">
      <tag v="bar" k="amenity" /><tag v="yes" k="gay" />
      <tag v="Example Gay Bar" k="name" />
    </node>
  </osm>
  val venBar = Venue((venBarXml \ "node")(0), venBarXml)

  val venRestaurantXml = <osm version="0.6">
    <node lon="-0.1430911" lat="51.5238028" id="107" fake="yes">
      <tag v="restaurant" k="amenity" /><tag v="chinese" k="cuisine" />
      <tag v="Example Restaurant" k="name" />
      <tag v="en:Example Restaurant" k="wikipedia" />
    </node>
  </osm>
  val venRestaurant = Venue((venRestaurantXml \ "node")(0), venRestaurantXml)

  "it should parse tags appropriately" should {
    ven.hasTag("amenity") must beTrue
    ven.hasTag("name") must beTrue
  }

  "it should have a name" should {
    ven.name must beEqualTo("The Green Man")
  }

  "it should return locations" should {
    ven.location() must beEqualTo(Pair(51.5238028.toDouble, -0.1430911.toDouble))
  }

  "it should have a URL" should {
    ven.url() must beEqualTo("http://openstreetmap.org/node/105")
  }

  "it should have a type description string" should {
    ven.venueType() must beEqualTo("pub")
    venBar.venueType() must beEqualTo("gay bar")
    venRestaurant.venueType() must beEqualTo("chinese restaurant")
  }

  "it should format Wikipedia URLs" should {
    venBar.wikipedia() must beEqualTo(None)
    venRestaurant.wikipedia() must beEqualTo(Some("https://en.wikipedia.org/wiki/Example_Restaurant"))
  }
}

// vim: set ts=2 sw=2 et:
