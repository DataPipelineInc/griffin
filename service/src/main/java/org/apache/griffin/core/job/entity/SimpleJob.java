package org.apache.griffin.core.job.entity;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * @CLassName SimpleJob
 * @Description Quartz Simple 任务类型
 * @Author goodman
 * @Date 2019-02-28 11:54
 * @Version 1.0
 **/
@Entity
@DiscriminatorValue("griffinSimpleJob")
public class SimpleJob extends AbstractJob {
    @Override
    public String getType() {
        return JobType.SIMPLE.getName();
    }

    public SimpleJob() {
    }
}
