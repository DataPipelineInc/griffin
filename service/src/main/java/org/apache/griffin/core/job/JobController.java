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

package org.apache.griffin.core.job;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import org.apache.griffin.core.job.entity.AbstractJob;
import org.apache.griffin.core.job.entity.JobHealth;
import org.apache.griffin.core.job.entity.JobInstanceBean;
import org.apache.griffin.core.util.FSUtil;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Api(value = "任务管理模块",tags = "任务管理模块")
public class JobController {

    @Autowired
    private JobService jobService;

    @ApiOperation(value = "获取所有任务列表")
    @RequestMapping(value = "/jobs", method = RequestMethod.GET)
    public List<AbstractJob> getJobs(@RequestParam(value = "type",
            defaultValue = "") String type) {
        return jobService.getAliveJobs(type);
    }

    @ApiOperation(value = "新建一个任务")
    @RequestMapping(value = "/jobs", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public AbstractJob addJob(@RequestBody AbstractJob job) throws Exception {
        return jobService.addJob(job);
    }

    @ApiOperation(value = "获取任务详细信息")
    @RequestMapping(value = "/jobs/config", method = RequestMethod.GET)
    public AbstractJob getJobConfig(@RequestParam("jobId") Long jobId) {
        return jobService.getJobConfig(jobId);
    }

    @ApiOperation(value = "任务动作接口")
    @RequestMapping(value = "/jobs/{id}", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.OK)
    public AbstractJob onActions(
            @PathVariable("id") Long jobId,
            @RequestParam String action) throws Exception {
        return jobService.onAction(jobId, action);
    }

    @ApiOperation(value = "删除一个任务")
    @RequestMapping(value = "/jobs", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteJob(@RequestParam("jobName") String jobName)
            throws SchedulerException {
        jobService.deleteJob(jobName);
    }

    @ApiOperation(value = "删除一个任务")
    @RequestMapping(value = "/jobs/{id}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteJob(@PathVariable("id") Long id)
            throws SchedulerException {
        jobService.deleteJob(id);
    }

    @ApiOperation(value = "获取子任务执行状态")
    @RequestMapping(value = "/jobs/instances", method = RequestMethod.GET)
    public List<JobInstanceBean> findInstancesOfJob(
            @RequestParam("jobId") Long id,
            @RequestParam("page") int page,
            @RequestParam("size") int size) {
        return jobService.findInstancesOfJob(id, page, size);
    }

    @ApiOperation(value = "获取任务的健康情况")
    @RequestMapping(value = "/jobs/health", method = RequestMethod.GET)
    public JobHealth getHealthInfo() {
        return jobService.getHealthInfo();
    }

    @ApiOperation(value = "下载任务执行结果")
    @RequestMapping(path = "/jobs/download", method = RequestMethod.GET)
    public ResponseEntity<Resource> download(
            @RequestParam("jobName") String jobName,
            @RequestParam("ts") long timestamp)
            throws Exception {
        String path = jobService.getJobHdfsSinksPath(jobName, timestamp);
        InputStreamResource resource = new InputStreamResource(
                FSUtil.getMissSampleInputStream(path));
        return ResponseEntity.ok().
                header("content-disposition",
                        "attachment; filename = sampleMissingData.json")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
