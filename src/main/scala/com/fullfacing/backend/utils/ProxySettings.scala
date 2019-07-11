package com.fullfacing.backend.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.ClientTransport
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.settings.ConnectionPoolSettings
import com.softwaremill.sttp.SttpBackendOptions

object ProxySettings {

  /* Sets up the Connection Pool with the correct settings. */
  def connectionSettings(actorSystem: ActorSystem,
                         options: SttpBackendOptions,
                         customConnectionPoolSettings: Option[ConnectionPoolSettings]): ConnectionPoolSettings = {

    val connectionPoolSettings: ConnectionPoolSettings = customConnectionPoolSettings
      .getOrElse(ConnectionPoolSettings(actorSystem))
      .withUpdatedConnectionSettings(_.withConnectingTimeout(options.connectionTimeout))

    connectionPoolSettings
  }

  /* Includes the proxy if one exists. */
  def includeProxy(connectionPoolSettings: ConnectionPoolSettings, options: SttpBackendOptions): ConnectionPoolSettings = {

    options.proxy.fold(connectionPoolSettings) { p =>
      val clientTransport = p.auth match {
        case Some(auth) => ClientTransport.httpsProxy(p.inetSocketAddress, BasicHttpCredentials(auth.username, auth.password))
        case None       => ClientTransport.httpsProxy(p.inetSocketAddress)
      }
      connectionPoolSettings.withTransport(clientTransport)
    }
  }
}
