package com.normation.plugins.apiauthorizations

import com.normation.plugins.{SnippetExtensionKey, SnippetExtensionPoint}
import com.normation.rudder.web.snippet.UserInformation
import net.liftweb.common.Loggable
import net.liftweb.util.Helpers._

import scala.xml.NodeSeq

class UserInformationExtension extends SnippetExtensionPoint[UserInformation] with Loggable {

  val extendsAt = SnippetExtensionKey(classOf[UserInformation].getSimpleName)

  def compose(snippet: UserInformation) : Map[String, NodeSeq => NodeSeq] = Map(
      "userCredentials" -> render _
  )

  def render(xml:NodeSeq) = {
    /* xml is a menu entry which looks like:
      <li class="user user-menu">
        <a href="#">
          <span>
            <span class="hidden-xs glyphicon glyphicon-user"></span>
            admin
          </span>
        </a>
      </li>
     *
     *
     * We want to do:

     <li class="dropdown user user-menu ">
        <a href="#" class="dropdown-toggle" data-toggle="dropdown">
          <span>
            <span class="hidden-xs glyphicon glyphicon-user"></span>
            admin
          </span>
          <i class="fa fa-angle-down" style="margin-left:15px;"></i>
        </a>
        <ul class="dropdown-menu" role="menu">
        ... here elm app ...
        </ul>
      </li>
     */


    val embedAppXml = <ul id="userApiTokenManagement" class="dropdown-menu">
      <head_merge>
        <link rel="stylesheet" type="text/css" href="/toserve/api-authorizations/media.css" media="screen" data-lift="with-cached-resource" />
        <script type="text/javascript" data-lift="with-cached-resource"  src="/toserve/api-authorizations/user-api-token.js"></script>
      </head_merge>
      <li id="user-token-app"></li>
      <script>
      //<![CDATA[
        // init elm app
        var node = document.getElementById('user-token-app');
        var app = Elm.UserApiToken.embed(node, {
          contextPath: contextPath
        });
      // ]]>
      </script>
    </ul>

    (
      "#user-menu [class+]" #> "dropdown"
    & "#user-menu-action [style+]" #> "cursor:pointer"
    & "#user-menu-action [class+]" #> "dropdown-toggle"
    & "#user-menu-action [data-toggle+]" #> "dropdown"
    & "#user-menu-action *+" #>  <i class="fa fa-angle-down" style="margin-left:15px;"></i>
    andThen "#user-menu *+" #> embedAppXml //need to be andThen, else other children mod are erased :/
    ).apply(xml)
  }

}
