package org.tommorris.osmcheckin.xapi
import org.tommorris.osmcheckin.Venue


object XAPI {
  import dispatch._
  import com.grum.geocalc._
  private val endpoint = "http://www.overpass-api.de/api/xapi?"
  
  // XAPI.namedObjectsNear(51.5130785, -0.1315878, 500)
  def namedObjectsNear(lat: Double, long: Double, distance: Int) = {
    val bbox = XAPI.boundingBoxAround(lat, long, distance)
    val bbox_desc = List(
      	bbox.getNorthEast.getLongitude.toString, // left
      	bbox.getSouthWest.getLatitude.toString,  // bottom
      	bbox.getSouthWest.getLongitude.toString, // right
      	bbox.getNorthEast.getLatitude.toString   // top
      ).mkString("[bbox=", ",", "]")
      
    val query = List("*", bbox_desc, "[name=*]")
    println(query.mkString)
    val resp = Http(url(endpoint + query.mkString) OK as.xml.Elem)
    val results = resp()
    (results \ "_").filter {elem => (elem.label == "node" || elem.label == "way") }.
      map(Venue(_, results)).
      filter(_.tags.contains("name")).
      filter(x => Venue.filterVenue(x)).
      sortBy(_.distanceFrom(lat, long))
  }

  def namedObjectsNear(lat: Double, long: Double): Seq[Venue] = {
    def call(lat: Double, long: Double, distance: List[Int]): Seq[Venue] = {
      val res = XAPI.namedObjectsNear(lat, long, distance.head)
      if (res.size > 20) res else call(lat, long, distance.tail)
    }
    val searchSizeList = List(100, 300, 1000, 3000)
    call(lat, long, searchSizeList)
  }
  
  /**
   * @param lat Latitude (WGS84)
   * @param long Longitude (WGS84)
   * @param distance Distance in metres
   */
  private def boundingBoxAround(lat: Double, long: Double, distance: Int) = {
    val point = new Point(new DegreeCoordinate(lat), new DegreeCoordinate(long))
    EarthCalc.getBoundingArea(point, distance)
  }
}

object TypeFormatter {
  def formatType(venue: Venue): Option[String] = {
    var out: Option[String] = None
    // if shop
    if (venue.hasTag("shop")) {
      out = Some("a " + venue.tags("shop") + " shop")
    }
    out
  }
}
