package com.normation.plugins.apiauthorizations

import bootstrap.liftweb.PluginsInfo
import com.normation.plugins.{SnippetExtensionKey, SnippetExtensionPoint}
import com.normation.rudder.rest.AllApi
import com.normation.rudder.rest.ApiKind
import com.normation.rudder.web.snippet.administration.ApiAccounts
import net.liftweb.common.Loggable
import net.liftweb.util.Helpers._

import scala.xml.NodeSeq

class ApiAccountsExtension extends SnippetExtensionPoint[ApiAccounts] with Loggable {

  val extendsAt = SnippetExtensionKey(classOf[ApiAccounts].getSimpleName)

  def compose(snippet: ApiAccounts) : Map[String, NodeSeq => NodeSeq] = Map(
      "render" -> render _
    , "body"   -> body _
  )


  /*
   * Append to the place where "var apiPath = ..." is defined the var with the lists of all {api name, api id, verb}.
   * The list defined groups of API (the one starting with the same name):
   * [
   *   { "category": "name of category",
   *     "apis"    : [
   *       {
   *         "name": "GetRules"
   *       , "description": "List all Rules"
   *       , "path": "/rules"
   *       , "verb": "GET"
   *       },
   *       ...
   *      ]
   *   },
   *   ...
   * ]
   */
  def render(xml:NodeSeq) = {
    //get all apis and for public one, and create the structure
    import net.liftweb.json._
    import net.liftweb.json.Serialization.write
    import net.liftweb.http.js.JsCmds._
    import net.liftweb.http.js.JE._
    implicit val formats = Serialization.formats(NoTypeHints)

    val categories = ((AllApi.api ++ PluginsInfo.pluginApisDef).filter(x => x.kind == ApiKind.Public || x.kind == ApiKind.General).groupBy(_.path.parts.head).map { case(cat, apis) =>
      JsonCategory(cat.value, apis.map(a => JsonApi(a.name, a.description, a.path.value, a.action.name)).sortBy(_.path))
    }).toList.sortBy(_.category)
    val json = write(categories)

    //now, add declaration of a JS variable: var rudderApis = [{ ... }]
    xml ++ Script(JsRaw(s"""var rudderApis = $json;"""))
  }

  def body(xml:NodeSeq) : NodeSeq = {
    ("name=newAccount *+" #>
      <div>
        <head_merge>
          <link rel="stylesheet" type="text/css" href="/toserve/api-authorizations/media.css" media="screen" data-lift="with-cached-resource" />
          <script type="text/javascript" data-lift="with-cached-resource"  src="/toserve/api-authorizations/api-authorizations.js"></script>
        </head_merge>
        <div id="acl-configuration" ng-if="myNewAccount.authorizationType === 'acl'">
          <!-- load elm app -->
          <div id="apiauthorization-app"></div>
          <script>
          //<![CDATA[
            if($('[ng-controller="AccountCtrl"]').scope()) { // wait for angularjs app to exists
              var account = $('[ng-controller="AccountCtrl"]').scope().myNewAccount;

              // init elm app
              var node = document.getElementById('apiauthorization-app');
              var app = Elm.ApiAuthorizations.embed(node, {
                  token: { id: account.id, acl: account.acl}
                , rudderApis: rudderApis
              });

              //set back seleced acl to angularjs variable
              app.ports.giveAcls.subscribe(function(acl) {
                account.acl = acl;
              });
            }
          // ]]>
          </script>
        </div>
      </div>
    ).apply(xml)
  }

}

/*
 * JSON representation of API (grouped in categories).
 * These class must be top-level, else liftweb-json gets mad and capture of outer()...
 */
final case class JsonApi(name: String, description: String, path: String, verb: String)
final case class JsonCategory(category: String, apis: List[JsonApi])
