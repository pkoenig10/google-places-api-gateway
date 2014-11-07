# Google Places API Gateway

A simple Java API management gateway for the Google Place Search API.  The gateway writes all requests and response to a database and allows users to query this database to analyze gateway usage.  The gateway also supports user authentication to only allow access to registered users and to track user activity on the gateway.

Documenatation for the Google Place Search API can be found at https://developers.google.com/places/documentation/search

## Installation and Setup

To run the gateway simply pull the source code from this repostory.  Open the Java project and run the file `Gateway.java`.  The default configuration listens on port 8080 and writes searches and results to a local SQLite database.  The port number can be changed in the source code.  The database connection information can be changed in the `gateway.properties` file, which is read by the gateway on startup.

###Database Configuration

The connection database requires three tables

 * `searches` - Contains searches
 * `results` - Contains the results of searches
 * `users` - Contains users authentication information
 
### Searches table

The following command creates a correctly formatted `searches` table on an SQLite database

```
CREATE TABLE searches (
id serial primary key,
sessionid uuid not null,
timestamp timestamp not null,
searchtype text,
username text, 
query text,
key text,
location text,
radius text,
keyword text,
language text,
minprice text,
maxprice text,
name text,
opennow text,
rankby text,
types text,
pagetoken text,
zagatselected text);
```

### Results table

The following command creates a correctly formatted `results` table on an SQLite database

```
CREATE TABLE results (
id serial primary key,
sessionid uuid not null,
timestamp timestamp not null,
username text,
placeid text,
lat real,
lng real);
```

### Users table

The following command creates a correctly formatted `users` table on an SQLite database

```
CREATE TABLE users (
id serial primary key,
sessionid uuid not null,
username text not null,
salt text not null,
passhash text not null);
```

## Search Requests

Search request allow for using the Google Place Search API to search for places based on a number of parameters.  The Google Place Search API consists of three types of searches

 * Nearby Search
 * Text Search
 * Radar Search

A gateway place search request is a HTTP request of the following form

```
http://<hostname>:<port>/google-places-api-gateway/<searchtype>?<parameters>
```

`hostname` is the hostname of the gateway.

`port` is the port the gateway is listening on.

`seachtype` is one of the following values

 * `nearbysearch` - Indicates a Nearby Search request using the Google Plcae Search API.
 * `textsearch` - Indicates a Text Search request using the Google Plcae Search API.
 * `nearbysearch` - Indicates a Radar Search request using the Google Plcae Search API.
  
`parameters` are the parameters for the search.  For each search type, the gateway requires all required parameters and supports all optional parameters detailed in the [Google Place Search API documentation](https://developers.google.com/places/documentation/search).  In addition the gateway also supports the following optional parameters

 * `username` - A username to authenticate the user and allow access to the gateway.  If you specifiy a `username` parameter, you must also specifiy a `password` parameter.
 * `password` - A password to authenticate the user and allow access to the gateway.  If you specifiy a `password` parameter, you must also specifiy a `username` parameter.

## Query Requests

Query requests allow for querying the database of past searches and results.  There are two types of gateway queries

 * Search Query - A query on the search requests handled by the gateway.
 * Result Query - A query on the search results returned by the gateway.

### Search Query

A gateway search query request is a HTTP request of the following form

```
http://<hostname>:<port>/google-places-api-gateway/searchquery?<parameters>
```

`hostname` is the hostname of the gateway.

`port` is the port the gateway is listening on.
 
`parameters` are the parameters for the query.  For any specified parameter, the query will only return records with values equal to that parameter.  A Search Query supports all required and optional search parameters for any type of search detailed in the [Google Place Search API documentation](https://developers.google.com/places/documentation/search).  In addition the gateway also supports the following Search Query parameters

 * `sessionid` - A universally unique identifier (UUID) used to identify a single session consisting of a search and a collection of results
 * `username` - The username of the user who made the search request.  This value will be `null` for anonymous requests.
 * `searchtype` - The type of search.  Valid values include `nearby`, `text', and 'radar'.
   
### Result Query

A gateway result query request is a HTTP request of the following form

```
http://<hostname>:<port>/google-places-api-gateway/resultquery?<parameters>
```

`hostname` is the hostname of the gateway.

`port` is the port the gateway is listening on.
 
`parameters` are the parameters for the query.  For any specified parameter, the query will only return records with values equal to that parameter.  The gateway supports the following Result Query parameters

 * `sessionid` - A universally unique identifier (UUID) used to identify a single session consisting of a search and a collection of results
 * `username` - The username of the user who made the search request.  This value will be `null` for anonymous requests.
 * `placeid` - A unique identifier for a place.
 * `lat` - The latitude of a result.
 * `lng` - The longitude of a result.
 
## User Requests

The gateway supports user authentication to only allow access to registered users.  The gateway can be configured to allow or deny requests from users who do not provide credentials in their request.  User request allow for new users to be added to the database.

A gateway Add User request is a HTTP request of the following form

```
http://<hostname>:<port>/google-places-api-gateway/adduser?newusername=<username>&newpassword=<password>
```

`hostname` is the hostname of the gateway.

`port` is the port the gateway is listening on.

`username` is the username of the user to be added

`password` is the password of the user to be added

The gateway will return an error response if the new username already exists in the database.

## Output

All output is returned in JavaScript Object Notation (JSON).  All repsonses have the following fields
 
 * `results` - An array of results of a search or query request.  For users request this field will be an empty array.
 * `status` - The status of the request.  See Status Codes below for more information.
 * `error_message` - A more detailed error message.

The output for search requests is simply forwarded repsonse from the Google Place Search API.  The output is identical to the output detailed in the [Google Place Search API documentation](https://developers.google.com/places/documentation/search#PlaceSearchResponses).

The output for query requests contains the results of the query are placed in the `results` field.  The `status` field indiciates whether the query was successul.

The output for query requests contains an empty `results` field.  The `status` field indiciates whether the request was successful.

### Status Codes

The output for search requests contains status codes detailed in the [Google Place Search API documentation](https://developers.google.com/places/documentation/search##PlaceSearchStatusCodes).  In addition the gateway may also output the following status codes

 * `OK` - Indicates that no errors occured.
 * `GATEWAY_INVALID_REQUEST` - Indicates that the gateway could not read the HTTP request.  Ensure that your HTTP request is being sent properly.
 * `GATEWAY_INVALID_URL` - Indicates that the gateway does support the path provided in the URL.  Ensure that your request URL matches one of the above formats.
 * `GATEWAY_AUTHENTICATION_FAILED` - Indicates that user authentication failed.  Ensure that you are prividing valid credendtials have registered with the gateway.
 * `GATEWAY_SEARCH_ERROR` - Indicates that an error occured while performing a search request.  Ensure that your request includes all required parameters and only contains supported parameters.
 * `GATEWAY_QUERY_ERROR` - Indicates that an error occured while performing a query request.  Ensure that your request only contains supported parameters.
 * `GATEWAY_ADD_USER_ERROR` - Indicates that an error occured while attempting to register a new user.  This is often because the request username is already taken.  Try submitting another request with a different username.
