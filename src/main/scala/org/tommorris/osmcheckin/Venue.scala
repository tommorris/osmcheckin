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
class Venue(obj: Node, context: Node, srcLat: Double, srcLong: Double) {
  import scala.collection.mutable.{Map => MutableMap}
  import com.grum.geocalc._

  val distanceFromSearch: Double = distanceFrom(srcLat, srcLong)
  
  /** Provides Map of key-value String pairs, mirrors OSM tags. */
  val tags: Map[String, String] = {
    val keymap = MutableMap[String, String]()
    (obj \ "tag").foreach {tag =>
      keymap.put(tag.attribute("k").get.toString, org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4(tag.attribute("v").get.toString))
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

  def addressHtml: Option[scala.xml.Elem] = {
    if (hasTag("addr:street"))
      Some(
      <div class="h-adr p-street-address">
        <span class="p-street-address">{ if (hasTag("addr:housenumber")) tags("addr:housenumber") } { tags("addr:street") }</span>
        { if (hasTag("addr:city")) <div class="p-locality">{ tags("addr:city") }</div> }
        { if (hasTag("postal_code")) <div class="p-postal-code">{ tags("postal_code") }</div> }
      </div>
      )
    else
      None
  }

  def website: Option[String] = {
    if (hasTag("website")) {
      if (tags("website").startsWith("http://") || tags("website").startsWith("https://"))
        Some(tags("website"))
      else
        Some("http://" + tags("website"))
    } else None
  }
  
  /** Provides HTML with hCard representing venue. */
  def toHtml: scala.xml.Elem = {
    import scala.xml.Utility._
    /* TODO:
      - address
      - phone/fax
      - geolocation
      - venue features:
        - wheelchair accessible
        - toilets
        - wifi
    */
    <div class="h-card vcard">
      { if (hasTag("name")) <div class="p-name fn">{ tags("name") }</div> else None }
      <div class="icons">
        { if (hasTag("wikipedia")) <a class="u-url url" href={ wikipedia().get }><img src="//upload.wikimedia.org/wikipedia/commons/thumb/2/2c/Tango_style_Wikipedia_Icon.svg/30px-Tango_style_Wikipedia_Icon.svg.png" /></a> }
        { if (website.isDefined) <a class="u-url url" href={ website.get }><img src="//upload.wikimedia.org/wikipedia/commons/thumb/7/74/Internet-web-browser.svg/30px-Internet-web-browser.svg.png" /></a> }
        <a href={url}><img src="http://upload.wikimedia.org/wikipedia/commons/thumb/b/b0/Openstreetmap_logo.svg/30px-Openstreetmap_logo.svg.png" /></a>
      </div>
      { if (venueType() != "") <div class="p-category category p-x-venue-type">{ venueType() }</div> }
      { if (addressHtml.isDefined) addressHtml.get }
      <div class="distance">{ "%.1f".format(distanceFromSearch).replaceFirst("""\.0$""", "") }m away</div>
    </div>
  }

  /** URL of the OpenStreetMap object (node, way etc.) */
  def url(): String = {
    "http://openstreetmap.org/browse/" + obj.label + "/" + obj.attribute("id").get.toString
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
          case Some(x: String) => x.replaceAll(";", "/") + " restaurant"
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
  def apply(obj: scala.xml.Node, context: scala.xml.Node, srcLat: Double, srcLong: Double) = {
    if (obj.label == "way") { new VenueWay(obj, context, srcLat, srcLong) } else { new Venue(obj, context, srcLat, srcLong) }
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
class VenueWay(obj: scala.xml.Node, context:scala.xml.Node, srcLat: Double, srcLong: Double) extends Venue(obj, context, srcLat, srcLong) {
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
