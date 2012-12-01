package org.tommorris.osmcheckin.xapi
import org.tommorris.osmcheckin.Venue


object XAPI {
  import dispatch._
  import com.grum.geocalc._
  private val endpoint = "http://www.overpass-api.de/api/xapi?"
  
  /** Finds named objects within <code>distance</code> metres of the specified location.
   * 
   * Example:
   * 
   * <code>XAPI.namedObjectsNear(51.5130785, -0.1315878, 500)</code>
   * 
   * Will find venues within 500 metres of Old Compton Street, Soho, London.
   */
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
    val resultsFiltered = (results \ "_").filter {elem => (elem.label == "node" || elem.label == "way") }.
      map(Venue(_, results, lat, long)).
      filter(_.tags.contains("name")).
      filter(x => Venue.filterVenue(x)).
      sortBy(_.distanceFrom(lat, long))

    //resultsFiltered.foreach(x => x.distanceFromSearch = Some(x.distanceFrom(lat, long)))

    resultsFiltered
  }

  /** Recursively calls API until it returns at least 30 venues. */
  def namedObjectsNear(lat: Double, long: Double): Seq[Venue] = {
    def call(lat: Double, long: Double, distance: List[Int]): Seq[Venue] = {
      val res = XAPI.namedObjectsNear(lat, long, distance.head)
      if (res.size > 30)
        res
      else {
        if (distance.tail.size == 0)
          List[Venue]()
        else
          call(lat, long, distance.tail)
      }
    }
    val distances = List(50, 100, 300, 1000, 3000, 10000)
    call(lat, long, distances)
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
