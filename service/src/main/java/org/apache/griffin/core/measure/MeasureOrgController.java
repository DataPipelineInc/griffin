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
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1")
@Api(value = "度量管理(组织)模块", tags = "度量管理(组织)模块")
public class MeasureOrgController {

    @Autowired
    private MeasureOrgService measureOrgService;

    @ApiOperation(value = "获取所有组织名字")
    @RequestMapping(value = "/org", method = RequestMethod.GET)
    public List<String> getOrgs() {
        return measureOrgService.getOrgs();
    }

    /**
     * @param org organization name
     * @return list of metric name, and a metric is the result of executing the
     * job sharing the same name with measure.
     */

    @ApiOperation(value = "根据组织名字获取度量规则")
    @RequestMapping(value = "/org/{org}", method = RequestMethod.GET)
    public List<String> getMetricNameListByOrg(@PathVariable("org") String org) {
        return measureOrgService.getMetricNameListByOrg(org);
    }

    @ApiOperation(value = "获取所有度量规则的名字")
    @RequestMapping(value = "/org/measure/names", method = RequestMethod.GET)
    public Map<String, List<String>> getMeasureNamesGroupByOrg() {
        return measureOrgService.getMeasureNamesGroupByOrg();
    }
}
