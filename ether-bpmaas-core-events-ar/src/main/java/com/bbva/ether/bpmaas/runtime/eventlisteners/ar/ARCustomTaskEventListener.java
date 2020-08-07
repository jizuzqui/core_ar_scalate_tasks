package com.bbva.ether.bpmaas.runtime.eventlisteners.ar;

import org.jbpm.services.task.events.DefaultTaskEventListener;
import org.kie.api.task.TaskEvent;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.User;
import org.kie.internal.task.api.TaskContext;
import org.kie.internal.task.api.TaskModelProvider;
import org.kie.internal.task.api.TaskPersistenceContext;
import org.jbpm.services.task.audit.impl.model.AuditTaskImpl;
import org.jbpm.services.task.utils.ClassUtil;

import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.server.api.model.admin.TaskReassignment;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ARCustomTaskEventListener extends DefaultTaskEventListener  {

    public RuntimeManager runtimeManager = null;

    public ARCustomTaskEventListener(){}
    public ARCustomTaskEventListener(RuntimeManager runtimeManager)
    {
        this.runtimeManager = runtimeManager;
    }

    @Override
    public void beforeTaskReassignedEvent(TaskEvent event) {
        Task ti = event.getTask();

        System.out.println("DRH0-before");
        User objUser = TaskModelProvider.getFactory().newUser("PRU0002");
        ti.getPeopleAssignments().getPotentialOwners().add(objUser);
    }

    @Override
    public void afterTaskReassignedEvent(TaskEvent event) {

        Task ti = event.getTask();
        TaskPersistenceContext persistenceContext = ((TaskContext)event.getTaskContext()).getPersistenceContext();
        AuditTaskImpl auditTaskImpl = getAuditTask(persistenceContext, ti);

        System.out.println("DRH0-after-->" + ti.getTaskData().getActualOwner().getId());

        //auditTaskImpl.setActualOwner("PRU0002");

        //persistenceContext.persistTask(task);

        User objUser = TaskModelProvider.getFactory().newUser("PRU0002");
        //ti.getPeopleAssignments().getBusinessAdministrators().add(objUser);
        ti.getPeopleAssignments().getPotentialOwners().add(objUser);

        //persistenceContext.updateTask(ti);

        //RuntimeEngine engine = runtimeManager.getRuntimeEngine(null);
        //KieSession ksession = engine.getKieSession();

    }

    protected AuditTaskImpl getAuditTask(TaskPersistenceContext persistenceContext, Task ti) {
        AuditTaskImpl auditTaskImpl = persistenceContext.queryWithParametersInTransaction("getAuditTaskById", true,
                persistenceContext.addParametersToMap("taskId", ti.getId()),
                ClassUtil.<AuditTaskImpl>castClass(AuditTaskImpl.class));

        return auditTaskImpl;
    }

}
