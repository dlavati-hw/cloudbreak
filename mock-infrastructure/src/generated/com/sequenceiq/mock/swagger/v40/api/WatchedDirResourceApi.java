/**
 * NOTE: This class is auto generated by the swagger code generator program (2.4.16).
 * https://github.com/swagger-api/swagger-codegen
 * Do not edit the class manually.
 */
package com.sequenceiq.mock.swagger.v40.api;

import com.sequenceiq.mock.swagger.model.ApiWatchedDir;
import com.sequenceiq.mock.swagger.model.ApiWatchedDirList;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-11-16T21:48:33.802+01:00")

@Api(value = "WatchedDirResource", description = "the WatchedDirResource API")
@RequestMapping(value = "/{mockUuid}/api/v40")
public interface WatchedDirResourceApi {

    Logger log = LoggerFactory.getLogger(WatchedDirResourceApi.class);

    default Optional<ObjectMapper> getObjectMapper() {
        return Optional.empty();
    }

    default Optional<HttpServletRequest> getRequest() {
        return Optional.empty();
    }

    default Optional<String> getAcceptHeader() {
        return getRequest().map(r -> r.getHeader("Accept"));
    }

    @ApiOperation(value = "Adds a directory to the watching list.", nickname = "addWatchedDirectory", notes = "Adds a directory to the watching list. <p> Available since API v14. Only available with Cloudera Manager Enterprise Edition. <p>", response = ApiWatchedDir.class, authorizations = {
        @Authorization(value = "basic")
    }, tags={ "WatchedDirResource", })
    @ApiResponses(value = { 
        @ApiResponse(code = 201, message = "Success", response = ApiWatchedDir.class) })
    @RequestMapping(value = "/clusters/{clusterName}/services/{serviceName}/watcheddir",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.POST)
    default ResponseEntity<ApiWatchedDir> addWatchedDirectory(@ApiParam(value = "The unique id of CB cluster (works in CB test framework only)",required=true) @PathVariable("mockUuid") String mockUuid,@ApiParam(value = "",required=true) @PathVariable("clusterName") String clusterName,@ApiParam(value = "The service name.",required=true) @PathVariable("serviceName") String serviceName,@ApiParam(value = "The directory to be added."  )  @Valid @RequestBody ApiWatchedDir body) {
        if(getObjectMapper().isPresent() && getAcceptHeader().isPresent()) {
            if (getAcceptHeader().get().contains("application/json")) {
                try {
                    return new ResponseEntity<>(getObjectMapper().get().readValue("{  \"path\" : \"...\"}", ApiWatchedDir.class), HttpStatus.NOT_IMPLEMENTED);
                } catch (IOException e) {
                    log.error("Couldn't serialize response for content type application/json", e);
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        } else {
            log.warn("ObjectMapper or HttpServletRequest not configured in default WatchedDirResourceApi interface so no example is generated");
        }
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }


    @ApiOperation(value = "Lists all the watched directories.", nickname = "listWatchedDirectories", notes = "Lists all the watched directories. <p> Available since API v14. Only available with Cloudera Manager Enterprise Edition. <p>", response = ApiWatchedDirList.class, authorizations = {
        @Authorization(value = "basic")
    }, tags={ "WatchedDirResource", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Success", response = ApiWatchedDirList.class) })
    @RequestMapping(value = "/clusters/{clusterName}/services/{serviceName}/watcheddir",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    default ResponseEntity<ApiWatchedDirList> listWatchedDirectories(@ApiParam(value = "The unique id of CB cluster (works in CB test framework only)",required=true) @PathVariable("mockUuid") String mockUuid,@ApiParam(value = "",required=true) @PathVariable("clusterName") String clusterName,@ApiParam(value = "The service name.",required=true) @PathVariable("serviceName") String serviceName) {
        if(getObjectMapper().isPresent() && getAcceptHeader().isPresent()) {
            if (getAcceptHeader().get().contains("application/json")) {
                try {
                    return new ResponseEntity<>(getObjectMapper().get().readValue("{  \"items\" : [ {    \"path\" : \"...\"  }, {    \"path\" : \"...\"  } ]}", ApiWatchedDirList.class), HttpStatus.NOT_IMPLEMENTED);
                } catch (IOException e) {
                    log.error("Couldn't serialize response for content type application/json", e);
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        } else {
            log.warn("ObjectMapper or HttpServletRequest not configured in default WatchedDirResourceApi interface so no example is generated");
        }
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }


    @ApiOperation(value = "Removes a directory from the watching list.", nickname = "removeWatchedDirectory", notes = "Removes a directory from the watching list. <p> Available since API v14. Only available with Cloudera Manager Enterprise Edition. <p>", response = ApiWatchedDir.class, authorizations = {
        @Authorization(value = "basic")
    }, tags={ "WatchedDirResource", })
    @ApiResponses(value = { 
        @ApiResponse(code = 204, message = "Success", response = ApiWatchedDir.class) })
    @RequestMapping(value = "/clusters/{clusterName}/services/{serviceName}/watcheddir/{directoryPath}",
        produces = { "application/json" }, 
        method = RequestMethod.DELETE)
    default ResponseEntity<ApiWatchedDir> removeWatchedDirectory(@ApiParam(value = "The unique id of CB cluster (works in CB test framework only)",required=true) @PathVariable("mockUuid") String mockUuid,@ApiParam(value = "",required=true) @PathVariable("clusterName") String clusterName,@ApiParam(value = "The directory path to be removed.",required=true) @PathVariable("directoryPath") String directoryPath,@ApiParam(value = "The service name.",required=true) @PathVariable("serviceName") String serviceName) {
        if(getObjectMapper().isPresent() && getAcceptHeader().isPresent()) {
            if (getAcceptHeader().get().contains("application/json")) {
                try {
                    return new ResponseEntity<>(getObjectMapper().get().readValue("{  \"path\" : \"...\"}", ApiWatchedDir.class), HttpStatus.NOT_IMPLEMENTED);
                } catch (IOException e) {
                    log.error("Couldn't serialize response for content type application/json", e);
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        } else {
            log.warn("ObjectMapper or HttpServletRequest not configured in default WatchedDirResourceApi interface so no example is generated");
        }
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

}