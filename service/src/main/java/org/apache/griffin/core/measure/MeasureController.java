/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.measure;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.validation.Valid;
import org.apache.griffin.core.measure.entity.Measure;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1")
@Api(value = "度量管理模块", tags = "度量管理模块")
public class MeasureController {

    @Autowired
    private MeasureService measureService;

    @ApiOperation(value = "获取所有度量规则")
    @RequestMapping(value = "/measures", method = RequestMethod.GET)
    public List<? extends Measure> getAllAliveMeasures(@RequestParam(value =
            "type", defaultValue = "") String type) {
        return measureService.getAllAliveMeasures(type);
    }

    @ApiOperation(value = "根据ID获取度量规则")
    @RequestMapping(value = "/measures/{id}", method = RequestMethod.GET)
    public Measure getMeasureById(@PathVariable("id") long id) {
        return measureService.getMeasureById(id);
    }

    @ApiOperation(value = "根据ID删除度量规则")
    @RequestMapping(value = "/measures/{id}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMeasureById(@PathVariable("id") Long id) throws
            SchedulerException {
        measureService.deleteMeasureById(id);
    }

    @ApiOperation(value = "删除所有度量规则")
    @RequestMapping(value = "/measures", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMeasures() throws SchedulerException {
        measureService.deleteMeasures();
    }

    @ApiOperation(value = "更新度量规则")
    @RequestMapping(value = "/measures", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.OK)
    public Measure updateMeasure(@RequestBody Measure measure) {
        return measureService.updateMeasure(measure);
    }

    @ApiOperation(value = "获取指定用户下的度量规则")
    @RequestMapping(value = "/measures/owner/{owner}", method =
            RequestMethod.GET)
    public List<Measure> getAliveMeasuresByOwner(@PathVariable("owner")
                                                 @Valid String owner) {
        return measureService.getAliveMeasuresByOwner(owner);
    }

    @ApiOperation(value = "新建一个度量规则")
    @RequestMapping(value = "/measures", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public Measure createMeasure(@RequestBody Measure measure) {
        return measureService.createMeasure(measure);
    }
}
