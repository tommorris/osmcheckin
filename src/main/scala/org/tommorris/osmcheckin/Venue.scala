package org.tommorris.osmcheckin

import scala.xml._

/** Venue represents a specific named place that one may wish to check in to.
* 
* The Venue class represents simple nodes.
* 
* Calling <code>Venue.apply</code> (or just <code>Venue(xml)</code>)
* will return either a <code>Venue</code> or a more specific <code>VenueWay</code>
* subclass if appropriate.
*/
class Venue(obj: Node, context: Node) {
  import scala.collection.mutable.{Map => MutableMap}
  import com.grum.geocalc._
  
  /** Provides Map of key-value String pairs, mirrors OSM tags. */
  val tags: Map[String, String] = {
    val keymap = MutableMap[String, String]()
    (obj \ "tag").foreach {tag =>
      keymap.put(tag.attribute("k").get.toString, tag.attribute("v").get.toString)
    }
    keymap.toMap
  }
  
  /** Checks to see if venue has specific tag. */
  def hasTag(tag: String) = tags.contains(tag)
  
  /** Returns tuple of latitude, longitude of venue object. */
  def location(): (Double, Double) = (obj.attribute("lat").get.first.text.toDouble, obj.attribute("lon").get.first.text.toDouble)

  /** Value of the name tag. */
  def name(): String = tags.get("name").get
  
  /** Calculates distance from lat, long point in metres. */
  def distanceFrom(lat: Double, long: Double) = {
    val there = new Point(new DegreeCoordinate(lat), new DegreeCoordinate(long))
    val here = new Point(new DegreeCoordinate(location._1), new DegreeCoordinate(location._2))
    EarthCalc.getDistance(here, there)
  }

  /** Formats Wikipedia URL.
   * 
   * Returns None if not available.
   */
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
  
  /** Provides HTML with hCard representing venue. */
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

  /** URL of the OpenStreetMap object (node, way etc.) */
  def url(): String = {
    "http://openstreetmap.org/" + obj.label + "/" + obj.attribute("id").get.toString
  }

  /** Provides string description of venue (e.g. "pub", "Italian restaurant"). */
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
          case Some("chinese") => "Chinese restaurant"
          case Some("vietnamese") => "Vietnamese restaurant"
          case Some(x: String) => x + " restaurant"
          case None => "restaurant"
        }
      case Some("cafe") => "cafe"
      case Some("library") => "library"
      case Some("place_of_worship") =>
        tags.get("religion") match {
          case Some("christian") =>
            tags.get("denomination") match {
              case Some("church_of_england") => "Church of England church"
              case Some(x: String) => x + " church"
              case None => "church"
            }
          case Some("jewish") => "synagogue"
          case Some("islam") => "mosque"
          case Some("buddhism") => "Buddhist temple"
          case Some(x: String) => x + " place of worship"
          case None => "place of worship"
        }
      case Some(x) => x
      case _ => ""
    }
  }
}
object Venue {
  def apply(obj: scala.xml.Node, context: scala.xml.Node) = {
    if (obj.label == "way") { new VenueWay(obj, context) } else { new Venue(obj, context) }
  }

  /** Predicate filter for venues.
    * Returns true if it is a valid venue, false if not. */
  def filterVenue(v: Venue) = {
    // postive filters
    (v.hasTag("amenity") ||
      v.hasTag("tourism") ||
      v.hasTag("shop")) &&
    // negative filters
    !(
     (v.hasTag("amenity") && v.tags("amenity") == "bicycle_rental")
    )
  }
}

/** VenueWays are venues that are represented as ways rather than nodes.. */
class VenueWay(obj: scala.xml.Node, context:scala.xml.Node) extends Venue(obj, context) {
  /** Returns a collection of all the nodes that make up the way. */
  def nodes() = (obj \ "nd").map {node =>
      (context \ "node").find { _.attribute("id") == node.attribute("ref") }.get
    }
  
  /** Calculates centroid location of a closed way object using simple mean. */
  override def location() = {
    val latitude = nodes().foldLeft(0.toDouble)((a, b) => a + b.attribute("lat").get.first.text.toDouble) / nodes.size
    val longitude = nodes().foldLeft(0.toDouble)((a, b) => a + b.attribute("lon").get.first.text.toDouble) / nodes.size
    (latitude, longitude)
  }
}
