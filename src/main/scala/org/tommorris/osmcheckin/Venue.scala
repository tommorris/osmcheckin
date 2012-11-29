package org.tommorris.osmcheckin

import scala.xml._

class Venue(obj: Node, context: Node) {
  import scala.collection.mutable.{Map => MutableMap}
  import com.grum.geocalc._
  
  val tags: Map[String, String] = {
    val keymap = MutableMap[String, String]()
    (obj \ "tag").foreach {tag =>
      keymap.put(tag.attribute("k").get.toString, tag.attribute("v").get.toString)
    }
    keymap.toMap
  }
  
  def hasTag(tag: String) = tags.contains(tag)
  
  def location(): (Double, Double) = (obj.attribute("lat").get.first.text.toDouble, obj.attribute("lon").get.first.text.toDouble)

  def name(): String = tags.get("name").get
  
  // returns distance in metres
  def distanceFrom(lat: Double, long: Double) = {
    val there = new Point(new DegreeCoordinate(lat), new DegreeCoordinate(long))
    val here = new Point(new DegreeCoordinate(location._1), new DegreeCoordinate(location._2))
    EarthCalc.getDistance(here, there)
  }

  def wikipedia(): Option[String] = {
    tags.get("wikipedia") match {
      case Some(x) if x.startsWith("http") => Some(x)
      case Some(x) =>
        val split = x.split(":")
        val url = "https://" + split.head + ".wikipedia.org/wiki/" + split.tail.mkString(":").replace(" ", "_")
        Some(url)
      case _ => None
    }
  }
  
  def toHtml: scala.xml.Elem = {
    /* TODO:
      - address
      - phone/fax
      - geolocation
      - venue features:
        - wheelchair accessible
        - toilets
        - wifi
    */
    <div class="h-card">
      { if (hasTag("name")) <div class="p-name">{ tags("name") }</div> else None }
      { if (venueType() != "") <div class="p-x-venue-type">{ venueType() }</div> }
      { if (hasTag("website")) <div><a class="p-url" href={ tags("website") }>Website</a></div> }
      { if (hasTag("wikipedia")) <div><a class="p-url" href={ wikipedia().get  }>Wikipedia</a></div> }
    </div>
  }

  def url(): String = {
    "http://openstreetmap.org/" + obj.label + "/" + obj.attribute("id").get.toString
  }

  def venueType(): String = {
    tags.get("amenity") match {
      case Some("pub") => "pub"
      case Some("bar") =>
        // TODO: add sports bar and other types of bar
        tags.get("gay") match {
          case Some(_) => "gay bar"
          case None => "bar"
        }
      case Some("restaurant") =>
        tags.get("cuisine") match {
          case Some(x: String) => x + " restaurant"
          case None => "restaurant"
        }
      case Some("cafe") => "cafe"
      case Some("library") => "library"
      case Some(x) => x
      case _ => ""
    }
  }
}
object Venue {
  def apply(obj: scala.xml.Node, context: scala.xml.Node) = {
    if (obj.label == "way") { new VenueWay(obj, context) } else { new Venue(obj, context) }
  }

  def filterVenue(v: Venue) = {
    List(
      if (v.hasTag("amenity")) true else false,
      if (v.hasTag("tourism")) true else false,
      if (v.hasTag("shop")) true else false
    ).contains(true)
  }
}

class VenueWay(obj: scala.xml.Node, context:scala.xml.Node) extends Venue(obj, context) {
  def nodes() = (obj \ "nd").map {node =>
      (context \ "node").find { _.attribute("id") == node.attribute("ref") }.get
    }
  
  override def location() = {
    val latitude = nodes().foldLeft(0.toDouble)((a, b) => a + b.attribute("lat").get.first.text.toDouble) / nodes.size
    val longitude = nodes().foldLeft(0.toDouble)((a, b) => a + b.attribute("lon").get.first.text.toDouble) / nodes.size
    (latitude, longitude)
  }
}
