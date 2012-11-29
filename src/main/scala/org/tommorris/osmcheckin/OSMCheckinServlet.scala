package org.tommorris.osmcheckin

import org.scalatra._
import scalate.ScalateSupport
import org.tommorris.osmcheckin.xapi.XAPI

class OSMCheckinServlet extends ScalatraServlet with ScalateSupport {
    get("/") {
        <html>
          <head><title>Hello world</title></head>
          <body>
            <div></div>
          </body>
        </html>
    }

    get("/lookup") {
        val venues = XAPI.namedObjectsNear(params("lat").toDouble, params("long").toDouble, 100)
        <html>
          <head><title>Lookup</title>
          </head>
          <body>
            <ol>
              { venues.map(venue => <li>{venue.toHtml}</li>) }
            </ol>
          </body>
        </html>
    }
}

// vim: set ts=4 sw=4 et:
