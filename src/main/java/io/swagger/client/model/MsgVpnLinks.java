/*
 * SEMP (Solace Element Management Protocol)
 *  SEMP (starting in `v2`, see [note 1](#notes)) is a RESTful API for configuring a Solace router.  SEMP uses URIs to address manageable **resources** of the Solace router. Resources are either individual **objects**, or **collections** of objects. The following APIs are provided:   API|Base Path|Purpose|Comments :---|:---|:---|:--- Configuration|/SEMP/v2/config|Reading and writing config state|See [note 2](#notes)    Resources are always nouns, with individual objects being singular and collections being plural. Objects within a collection are identified by an `obj-id`, which follows the collection name with the form `collection-name/obj-id`. Some examples:  <pre> /SEMP/v2/config/msgVpns                       ; MsgVpn collection /SEMP/v2/config/msgVpns/finance               ; MsgVpn object named \"finance\" /SEMP/v2/config/msgVpns/finance/queues        ; Queue collection within MsgVpn \"finance\" /SEMP/v2/config/msgVpns/finance/queues/orderQ ; Queue object named \"orderQ\" within MsgVpn \"finance\" </pre>  ## Collection Resources  Collections are unordered lists of objects (unless described as otherwise), and are described by JSON arrays. Each item in the array represents an object in the same manner as the individual object would normally be represented. The creation of a new object is done through its collection resource.  ## Object Resources  Objects are composed of attributes and collections, and are described by JSON content as name/value pairs. The collections of an object are not contained directly in the object's JSON content, rather the content includes a URI attribute which points to the collection. This contained collection resource must be managed as a separate resource through this URI.  At a minimum, every object has 1 or more identifying attributes, and its own `uri` attribute which contains the URI to itself. Attributes may have any (non-exclusively) of the following properties:   Property|Meaning|Comments :---|:---|:--- Identifying|Attribute is involved in unique identification of the object, and appears in its URI| Required|Attribute must be provided in the request| Read-Only|Attribute can only be read, not written|See [note 3](#notes) Write-Only|Attribute can only be written, not read| Requires-Disable|Attribute can only be changed when object is disabled| Deprecated|Attribute is deprecated, and will disappear in the next SEMP version|    In some requests, certain attributes may only be provided in certain combinations with other attributes:   Relationship|Meaning :---|:--- Requires|Attribute may only be changed by a request if a particular attribute or combination of attributes is also provided in the request Conflicts|Attribute may only be provided in a request if a particular attribute or combination of attributes is not also provided in the request    ## HTTP Methods  The HTTP methods of POST, PUT, PATCH, DELETE, and GET manipulate resources following these general principles:   Method|Resource|Meaning|Request Body|Response Body|Missing Request Attributes :---|:---|:---|:---|:---|:--- POST|Collection|Create object|Initial attribute values|Object attributes and metadata|Set to default PUT|Object|Replace object|New attribute values|Object attributes and metadata|Set to default (but see [note 4](#notes)) PATCH|Object|Update object|New attribute values|Object attributes and metadata | Left unchanged| DELETE|Object|Delete object|Empty|Object metadata|N/A GET|Object|Get object|Empty|Object attributes and metadata|N/A GET|Collection|Get collection|Empty|Object attributes and collection metadata|N/A    ## Common Query Parameters  The following are some common query parameters that are supported by many method/URI combinations. Individual URIs may document additional parameters. Note that multiple query parameters can be used together in a single URI, separated by the ampersand character. For example:  <pre> ; Request for the MsgVpns collection using two hypothetical query parameters ; \"q1\" and \"q2\" with values \"val1\" and \"val2\" respectively /SEMP/v2/config/msgVpns?q1=val1&q2=val2 </pre>  ### select  Include in the response only selected attributes of the object. Use this query parameter to limit the size of the returned data for each returned object, or return only those fields that are desired.  The value of `select` is a comma-separated list of attribute names. Names may include the `*` wildcard. Nested attribute names are supported using periods (e.g. `parentName.childName`). If the list is empty (i.e. `select=`) no attributes are returned; otherwise the list must match at least one attribute name of the object. Some examples:  <pre> ; List of all MsgVpn names /SEMP/v2/config/msgVpns?select=msgVpnName  ; Authentication attributes of MsgVpn \"finance\" /SEMP/v2/config/msgVpns/finance?select=authentication*  ; Access related attributes of Queue \"orderQ\" of MsgVpn \"finance\" /SEMP/v2/config/msgVpns/finance/queues/orderQ?select=owner,permission </pre>  ### where  Include in the response only objects where certain conditions are true. Use this query parameter to limit which objects are returned to those whose attribute values meet the given conditions.  The value of `where` is a comma-separated list of expressions. All expressions must be true for the object to be included in the response. Each expression takes the form:  <pre> expression  = attribute-name OP value OP          = '==' | '!=' | '<' | '>' | '<=' | '>=' </pre>  `value` may be a number, string, `true`, or `false`, as appropriate for the type of `attribute-name`. Greater-than and less-than comparisons only work for numbers. A `*` in a string `value` is interpreted as a wildcard. Some examples:  <pre> ; Only enabled MsgVpns /SEMP/v2/config/msgVpns?where=enabled==true  ; Only MsgVpns using basic non-LDAP authentication /SEMP/v2/config/msgVpns?where=authenticationBasicEnabled==true,authenticationBasicType!=ldap  ; Only MsgVpns that allow more than 100 client connections /SEMP/v2/config/msgVpns?where=maxConnectionCount>100 </pre>  ### count  Limit the count of objects in the response. This can be useful to limit the size of the response for large collections. The minimum value for `count` is `1` and the default is `10`. There is a hidden maximum as to prevent overloading the system. For example:  <pre> ; Up to 25 MsgVpns /SEMP/v2/config/msgVpns?count=25 </pre>  ### cursor  The cursor, or position, for the next page of objects. Cursors are opaque data that should not be created or interpreted by SEMP clients, and should only be used as described below.  When a request is made for a collection and there may be additional objects available for retrieval that are not included in the initial response, the response will include a `cursorQuery` field containing a cursor. The value of this field can be specified in the `cursor` query parameter of a subsequent request to retrieve the next page of objects. For convenience, an appropriate URI is constructed automatically by the router and included in the `nextPageUri` field of the response. This URI can be used directly to retrieve the next page of objects.  ## Notes  1. This specification defines SEMP starting in `v2`, and not the original SEMP `v1` interface. Request and response formats between `v1` and `v2` are entirely incompatible, although both protocols share a common port configuration on the Solace router. They are differentiated by the initial portion of the URI path, one of either `/SEMP/` or `/SEMP/v2/`. 2. The config API is partially implemented. Only a subset of all configurable objects are available. 3. Read-only attributes may appear in POST and PUT/PATCH requests. However, if a read-only attribute is not marked as identifying, it will be ignored during a PUT/PATCH. 4. For PUT, if the SEMP user is not authorized to modify the attribute, its value is left unchanged rather than set to default. In addition, the values of write-only attributes are not set to their defaults on a PUT. 5. For DELETE, the body of the request currently serves no purpose and will cause an error if not empty. 
 *
 * OpenAPI spec version: 2.8.1.0.8
 * Contact: support_request@solacesystems.com
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package io.swagger.client.model;

import java.util.Objects;
import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * MsgVpnLinks
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2017-04-03T18:09:16.773Z")
public class MsgVpnLinks {
  @SerializedName("aclProfilesUri")
  private String aclProfilesUri = null;

  @SerializedName("authorizationGroupsUri")
  private String authorizationGroupsUri = null;

  @SerializedName("bridgesUri")
  private String bridgesUri = null;

  @SerializedName("clientProfilesUri")
  private String clientProfilesUri = null;

  @SerializedName("clientUsernamesUri")
  private String clientUsernamesUri = null;

  @SerializedName("queuesUri")
  private String queuesUri = null;

  @SerializedName("restDeliveryPointsUri")
  private String restDeliveryPointsUri = null;

  @SerializedName("sequencedTopicsUri")
  private String sequencedTopicsUri = null;

  @SerializedName("uri")
  private String uri = null;

  public MsgVpnLinks aclProfilesUri(String aclProfilesUri) {
    this.aclProfilesUri = aclProfilesUri;
    return this;
  }

   /**
   * The URI of this MsgVpn's aclProfiles collection.
   * @return aclProfilesUri
  **/
  @ApiModelProperty(example = "null", value = "The URI of this MsgVpn's aclProfiles collection.")
  public String getAclProfilesUri() {
    return aclProfilesUri;
  }

  public void setAclProfilesUri(String aclProfilesUri) {
    this.aclProfilesUri = aclProfilesUri;
  }

  public MsgVpnLinks authorizationGroupsUri(String authorizationGroupsUri) {
    this.authorizationGroupsUri = authorizationGroupsUri;
    return this;
  }

   /**
   * The URI of this MsgVpn's authorizationGroups collection.
   * @return authorizationGroupsUri
  **/
  @ApiModelProperty(example = "null", value = "The URI of this MsgVpn's authorizationGroups collection.")
  public String getAuthorizationGroupsUri() {
    return authorizationGroupsUri;
  }

  public void setAuthorizationGroupsUri(String authorizationGroupsUri) {
    this.authorizationGroupsUri = authorizationGroupsUri;
  }

  public MsgVpnLinks bridgesUri(String bridgesUri) {
    this.bridgesUri = bridgesUri;
    return this;
  }

   /**
   * The URI of this MsgVpn's bridges collection.
   * @return bridgesUri
  **/
  @ApiModelProperty(example = "null", value = "The URI of this MsgVpn's bridges collection.")
  public String getBridgesUri() {
    return bridgesUri;
  }

  public void setBridgesUri(String bridgesUri) {
    this.bridgesUri = bridgesUri;
  }

  public MsgVpnLinks clientProfilesUri(String clientProfilesUri) {
    this.clientProfilesUri = clientProfilesUri;
    return this;
  }

   /**
   * The URI of this MsgVpn's clientProfiles collection.
   * @return clientProfilesUri
  **/
  @ApiModelProperty(example = "null", value = "The URI of this MsgVpn's clientProfiles collection.")
  public String getClientProfilesUri() {
    return clientProfilesUri;
  }

  public void setClientProfilesUri(String clientProfilesUri) {
    this.clientProfilesUri = clientProfilesUri;
  }

  public MsgVpnLinks clientUsernamesUri(String clientUsernamesUri) {
    this.clientUsernamesUri = clientUsernamesUri;
    return this;
  }

   /**
   * The URI of this MsgVpn's clientUsernames collection.
   * @return clientUsernamesUri
  **/
  @ApiModelProperty(example = "null", value = "The URI of this MsgVpn's clientUsernames collection.")
  public String getClientUsernamesUri() {
    return clientUsernamesUri;
  }

  public void setClientUsernamesUri(String clientUsernamesUri) {
    this.clientUsernamesUri = clientUsernamesUri;
  }

  public MsgVpnLinks queuesUri(String queuesUri) {
    this.queuesUri = queuesUri;
    return this;
  }

   /**
   * The URI of this MsgVpn's queues collection.
   * @return queuesUri
  **/
  @ApiModelProperty(example = "null", value = "The URI of this MsgVpn's queues collection.")
  public String getQueuesUri() {
    return queuesUri;
  }

  public void setQueuesUri(String queuesUri) {
    this.queuesUri = queuesUri;
  }

  public MsgVpnLinks restDeliveryPointsUri(String restDeliveryPointsUri) {
    this.restDeliveryPointsUri = restDeliveryPointsUri;
    return this;
  }

   /**
   * The URI of this MsgVpn's restDeliveryPoints collection.
   * @return restDeliveryPointsUri
  **/
  @ApiModelProperty(example = "null", value = "The URI of this MsgVpn's restDeliveryPoints collection.")
  public String getRestDeliveryPointsUri() {
    return restDeliveryPointsUri;
  }

  public void setRestDeliveryPointsUri(String restDeliveryPointsUri) {
    this.restDeliveryPointsUri = restDeliveryPointsUri;
  }

  public MsgVpnLinks sequencedTopicsUri(String sequencedTopicsUri) {
    this.sequencedTopicsUri = sequencedTopicsUri;
    return this;
  }

   /**
   * The URI of this MsgVpn's sequencedTopics collection.
   * @return sequencedTopicsUri
  **/
  @ApiModelProperty(example = "null", value = "The URI of this MsgVpn's sequencedTopics collection.")
  public String getSequencedTopicsUri() {
    return sequencedTopicsUri;
  }

  public void setSequencedTopicsUri(String sequencedTopicsUri) {
    this.sequencedTopicsUri = sequencedTopicsUri;
  }

  public MsgVpnLinks uri(String uri) {
    this.uri = uri;
    return this;
  }

   /**
   * The URI of this MsgVpn object.
   * @return uri
  **/
  @ApiModelProperty(example = "null", value = "The URI of this MsgVpn object.")
  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MsgVpnLinks msgVpnLinks = (MsgVpnLinks) o;
    return Objects.equals(this.aclProfilesUri, msgVpnLinks.aclProfilesUri) &&
        Objects.equals(this.authorizationGroupsUri, msgVpnLinks.authorizationGroupsUri) &&
        Objects.equals(this.bridgesUri, msgVpnLinks.bridgesUri) &&
        Objects.equals(this.clientProfilesUri, msgVpnLinks.clientProfilesUri) &&
        Objects.equals(this.clientUsernamesUri, msgVpnLinks.clientUsernamesUri) &&
        Objects.equals(this.queuesUri, msgVpnLinks.queuesUri) &&
        Objects.equals(this.restDeliveryPointsUri, msgVpnLinks.restDeliveryPointsUri) &&
        Objects.equals(this.sequencedTopicsUri, msgVpnLinks.sequencedTopicsUri) &&
        Objects.equals(this.uri, msgVpnLinks.uri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(aclProfilesUri, authorizationGroupsUri, bridgesUri, clientProfilesUri, clientUsernamesUri, queuesUri, restDeliveryPointsUri, sequencedTopicsUri, uri);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MsgVpnLinks {\n");
    
    sb.append("    aclProfilesUri: ").append(toIndentedString(aclProfilesUri)).append("\n");
    sb.append("    authorizationGroupsUri: ").append(toIndentedString(authorizationGroupsUri)).append("\n");
    sb.append("    bridgesUri: ").append(toIndentedString(bridgesUri)).append("\n");
    sb.append("    clientProfilesUri: ").append(toIndentedString(clientProfilesUri)).append("\n");
    sb.append("    clientUsernamesUri: ").append(toIndentedString(clientUsernamesUri)).append("\n");
    sb.append("    queuesUri: ").append(toIndentedString(queuesUri)).append("\n");
    sb.append("    restDeliveryPointsUri: ").append(toIndentedString(restDeliveryPointsUri)).append("\n");
    sb.append("    sequencedTopicsUri: ").append(toIndentedString(sequencedTopicsUri)).append("\n");
    sb.append("    uri: ").append(toIndentedString(uri)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
  
}

