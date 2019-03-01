package org.apache.griffin.core.job;

import static org.apache.griffin.core.exception.GriffinExceptionMessage.INVALID_CONNECTOR_NAME;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.INVALID_JOB_NAME;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.JOB_IS_NOT_IN_PAUSED_STATUS;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.JOB_IS_NOT_SCHEDULED;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.JOB_KEY_DOES_NOT_EXIST;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.MISSING_BASELINE_CONFIG;
import static org.apache.griffin.core.job.entity.JobType.SIMPLE;
import static org.quartz.JobKey.jobKey;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.Trigger.TriggerState.BLOCKED;
import static org.quartz.Trigger.TriggerState.NORMAL;
import static org.quartz.Trigger.TriggerState.PAUSED;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.TriggerKey.triggerKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.griffin.core.exception.GriffinException;
import org.apache.griffin.core.job.entity.AbstractJob;
import org.apache.griffin.core.job.entity.JobDataSegment;
import org.apache.griffin.core.job.entity.JobHealth;
import org.apache.griffin.core.job.entity.JobInstanceBean;
import org.apache.griffin.core.job.entity.JobState;
import org.apache.griffin.core.job.entity.LivySessionStates;
import org.apache.griffin.core.job.entity.SimpleJob;
import org.apache.griffin.core.job.repo.JobInstanceRepo;
import org.apache.griffin.core.job.repo.SimpleJobRepo;
import org.apache.griffin.core.measure.entity.DataSource;
import org.apache.griffin.core.measure.entity.GriffinMeasure;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @CLassName SimpleJobOperatorImpl
 * @Description Quartz Simple Job Impl
 * @Author goodman
 * @Date 2019-02-28 14:56
 * @Version 1.0
 **/
@Service
public class SimpleJobOperatorImpl implements JobOperator {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(SimpleJobOperatorImpl.class);

    @Autowired
    private SchedulerFactoryBean factory;
    @Autowired
    private JobInstanceRepo instanceRepo;
    @Autowired
    private SimpleJobRepo simpleJobRepo;
    @Autowired
    private JobServiceImpl jobService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AbstractJob add(AbstractJob job, GriffinMeasure measure) throws Exception {
        validateParams(job, measure);
        String qName = jobService.getQuartzName(job);
        String qGroup = jobService.getQuartzGroup(SIMPLE);
        TriggerKey triggerKey = jobService.getTriggerKeyIfValid(qName, qGroup);
        SimpleJob simpleJob = genSimpleJobBean(job, qName, qGroup);
        simpleJob = simpleJobRepo.save(simpleJob);
        jobService.addJob(triggerKey, simpleJob, SIMPLE);
        return job;
    }


    private SimpleJob genSimpleJobBean(AbstractJob job,
                                       String qName,
                                       String qGroup) {
        SimpleJob simpleJob = (SimpleJob) job;
        simpleJob.setMetricName(job.getJobName());
        simpleJob.setGroup(qGroup);
        simpleJob.setName(qName);
        return simpleJob;
    }

    @Override
    public void start(AbstractJob job) throws Exception {
        String name = job.getName();
        String group = job.getGroup();
        Trigger.TriggerState state = getTriggerState(name, group);
        if (state == null) {
            throw new GriffinException.BadRequestException(
                    JOB_IS_NOT_SCHEDULED);
        }
        /* If job is not in paused state,we can't start it
        as it may be RUNNING.*/
        if (state != PAUSED) {
            throw new GriffinException.BadRequestException
                    (JOB_IS_NOT_IN_PAUSED_STATUS);
        }
        JobKey jobKey = jobKey(name, group);
        try {
            factory.getScheduler().resumeJob(jobKey);
        } catch (SchedulerException e) {
            throw new GriffinException.ServiceException(
                    "Failed to start job.", e);
        }
    }

    @Override
    public void stop(AbstractJob job) throws SchedulerException {
        pauseJob((SimpleJob) job, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(AbstractJob job) throws SchedulerException {
        pauseJob((SimpleJob) job, true);
    }

    @Override
    public JobHealth getHealth(JobHealth jobHealth, AbstractJob job) throws SchedulerException {
        List<? extends Trigger> triggers = jobService
                .getTriggers(job.getName(), job.getGroup());
        if (!CollectionUtils.isEmpty(triggers)) {
            jobHealth.setJobCount(jobHealth.getJobCount() + 1);
            if (jobService.isJobHealthy(job.getId())) {
                jobHealth.setHealthyJobCount(
                        jobHealth.getHealthyJobCount() + 1);
            }
        }
        return jobHealth;
    }

    @Override
    public JobState getState(AbstractJob job, String action) throws SchedulerException {
        JobState jobState = new JobState();
        Scheduler scheduler = factory.getScheduler();
        if (job.getGroup() == null || job.getName() == null) {
            return null;
        }
        TriggerKey triggerKey = triggerKey(job.getName(), job.getGroup());
        Trigger.TriggerState triggerState = scheduler.getTriggerState(triggerKey);
        jobState.setState(triggerState.toString());
        jobState.setToStart(getStartStatus(triggerState));
        jobState.setToStop(getStopStatus(triggerState));
        setTriggerTime(job, jobState);
        return jobState;
    }


    private Trigger.TriggerState getTriggerState(String name, String group) {
        try {
            List<? extends Trigger> triggers = jobService.getTriggers(name,
                    group);
            if (CollectionUtils.isEmpty(triggers)) {
                return null;
            }
            TriggerKey key = triggers.get(0).getKey();
            return factory.getScheduler().getTriggerState(key);
        } catch (SchedulerException e) {
            LOGGER.error("Failed to delete job", e);
            throw new GriffinException
                    .ServiceException("Failed to delete job", e);
        }

    }

    private void setTriggerTime(AbstractJob job, JobState jobState)
            throws SchedulerException {
        List<? extends Trigger> triggers = jobService
                .getTriggers(job.getName(), job.getGroup());
        // If triggers are empty, in Griffin it means job is completed whose
        // trigger state is NONE or not scheduled.
        if (CollectionUtils.isEmpty(triggers)) {
            return;
        }
        Trigger trigger = triggers.get(0);
        Date nextFireTime = trigger.getNextFireTime();
        Date previousFireTime = trigger.getPreviousFireTime();
        jobState.setNextFireTime(nextFireTime != null ?
                nextFireTime.getTime() : -1);
        jobState.setPreviousFireTime(previousFireTime != null ?
                previousFireTime.getTime() : -1);
    }


    @Override
    public void one(AbstractJob job) throws SchedulerException {
        JobKey jobKey = jobKey(job.getName(), job.getGroup());
        JobDetail jobDetail = factory.getScheduler().getJobDetail(jobKey);
        TriggerKey triggerKey = new TriggerKey(SIMPLE + "oneJob", SIMPLE + "oneJob");
        Trigger trigger = genTriggerInstance(triggerKey, jobDetail);
        factory.getScheduler().scheduleJob(trigger);
    }

    private Trigger genTriggerInstance(TriggerKey tk, JobDetail jd) {
        return newTrigger().withIdentity(tk).forJob(jd).startNow()
                .withSchedule(simpleSchedule().withIntervalInSeconds(1)
                        .withRepeatCount(0))
                .build();
    }

    private boolean getStartStatus(Trigger.TriggerState state) {
        return state == PAUSED;
    }

    /**
     * only NORMAL or  BLOCKED state of job can be started
     *
     * @param state job state
     * @return true: job can be stopped, false: job is running which cannot be
     * stopped
     */
    private boolean getStopStatus(Trigger.TriggerState state) {
        return state == NORMAL || state == BLOCKED;
    }

    /**
     * @param job    griffin job
     * @param delete if job needs to be deleted,set isNeedDelete true,otherwise
     *               it just will be paused.
     */
    private void pauseJob(SimpleJob job, boolean delete) {
        try {
            pauseJob(job.getGroup(), job.getName());
            pausePredicateJob(job);
            job.setDeleted(delete);
            simpleJobRepo.save(job);
        } catch (Exception e) {
            LOGGER.error("Job schedule happens exception.", e);
            throw new GriffinException.ServiceException("Job schedule " +
                    "happens exception.", e);
        }
    }

    public void deleteJob(String group, String name) throws SchedulerException {
        Scheduler scheduler = factory.getScheduler();
        JobKey jobKey = new JobKey(name, group);
        if (!scheduler.checkExists(jobKey)) {
            LOGGER.info("Job({},{}) does not exist.", jobKey.getGroup(), jobKey
                    .getName());
            return;
        }
        scheduler.deleteJob(jobKey);

    }

    private void pausePredicateJob(SimpleJob job) throws SchedulerException {
        List<JobInstanceBean> instances = instanceRepo.findByJobId(job.getId());
        for (JobInstanceBean instance : instances) {
            if (!instance.isPredicateDeleted()) {
                deleteJob(instance.getPredicateGroup(), instance
                        .getPredicateName());
                instance.setPredicateDeleted(true);
                if (instance.getState().equals(LivySessionStates.State.FINDING)) {
                    instance.setState(LivySessionStates.State.NOT_FOUND);
                }
            }
        }
    }


    private void pauseJob(String group, String name) throws SchedulerException {
        if (StringUtils.isEmpty(group) || StringUtils.isEmpty(name)) {
            return;
        }
        Scheduler scheduler = factory.getScheduler();
        JobKey jobKey = new JobKey(name, group);
        if (!scheduler.checkExists(jobKey)) {
            LOGGER.warn("Job({},{}) does not exist.", jobKey.getGroup(), jobKey
                    .getName());
            throw new GriffinException.NotFoundException
                    (JOB_KEY_DOES_NOT_EXIST);
        }
        scheduler.pauseJob(jobKey);
    }

    private void validateParams(AbstractJob job, GriffinMeasure measure) {
        if (!jobService.isValidJobName(job.getJobName())) {
            throw new GriffinException.BadRequestException(INVALID_JOB_NAME);
        }
        if (!isValidBaseLine(job.getSegments())) {
            throw new GriffinException.BadRequestException
                    (MISSING_BASELINE_CONFIG);
        }
        List<String> names = getConnectorNames(measure);
        if (!isValidConnectorNames(job.getSegments(), names)) {
            throw new GriffinException.BadRequestException
                    (INVALID_CONNECTOR_NAME);
        }
    }

    private boolean isValidBaseLine(List<JobDataSegment> segments) {
        assert segments != null;
        for (JobDataSegment jds : segments) {
            if (jds.isAsTsBaseline()) {
                return true;
            }
        }
        LOGGER.warn("Please set segment timestamp baseline " +
                "in as.baseline field.");
        return false;
    }

    private boolean isValidConnectorNames(List<JobDataSegment> segments,
                                          List<String> names) {
        assert segments != null;
        Set<String> sets = new HashSet<>();
        for (JobDataSegment segment : segments) {
            String dcName = segment.getDataConnectorName();
            sets.add(dcName);
            boolean exist = names.stream().anyMatch(name -> name.equals
                    (dcName));
            if (!exist) {
                LOGGER.warn("Param {} is a illegal string. " +
                        "Please input one of strings in {}.", dcName, names);
                return false;
            }
        }
        if (sets.size() < segments.size()) {
            LOGGER.warn("Connector names in job data segment " +
                    "cannot duplicate.");
            return false;
        }
        return true;
    }

    private List<String> getConnectorNames(GriffinMeasure measure) {
        Set<String> sets = new HashSet<>();
        List<DataSource> sources = measure.getDataSources();
        for (DataSource source : sources) {
            source.getConnectors().forEach(dc -> sets.add(dc.getName()));
        }
        if (sets.size() < sources.size()) {
            LOGGER.warn("Connector names cannot be repeated.");
            return Collections.emptyList();
        }
        return new ArrayList<>(sets);
    }
}
