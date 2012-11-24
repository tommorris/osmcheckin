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
  
  // returns distance in metres
  def distanceFrom(lat: Double, long: Double) = {
    val there = new Point(new DegreeCoordinate(lat), new DegreeCoordinate(long))
    val here = new Point(new DegreeCoordinate(location._1), new DegreeCoordinate(location._2))
    EarthCalc.getDistance(here, there)
  }
  
  def toHtml: scala.xml.Elem = {
    <div class="h-card">
      { if (hasTag("name")) <div class="p-name">{ tags("name") }</div> else None }
      { }
    </div>
  }
}
object Venue {
  def apply(obj: scala.xml.Node, context: scala.xml.Node) = {
    if (obj.label == "way") { new VenueWay(obj, context) } else { new Venue(obj, context) }
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
