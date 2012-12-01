package org.tommorris.osmcheckin

import org.scalatra._
import scalate.ScalateSupport
import org.tommorris.osmcheckin.xapi.XAPI

class OSMCheckinServlet extends ScalatraServlet with ScalateSupport {
    get("/") {
        contentType="text/html"
        ssp("/index")
    }

    get("/lookup") {
        contentType="text/html"
        val venues = XAPI.namedObjectsNear(params("lat").toDouble, params("long").toDouble)
        ssp("/lookup", "venues" -> <ol> { venues.map(v => <li>{v.toHtml}</li>) } </ol>)
    }
}

// vim: set ts=4 sw=4 et:
