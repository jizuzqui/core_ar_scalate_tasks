package com.bbva.ether.bpmaas.runtime.eventlisteners.ar;

import com.bbva.ether.bpmaas.runtime.connector.ar.CoreRest;
import com.bbva.ether.bpmaas.runtime.eventlisteners.ar.model.ResponseEscaladoDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.UserTaskService;
import org.jbpm.services.api.service.ServiceRegistry;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.jbpm.services.task.events.DefaultTaskEventListener;
import org.kie.api.task.TaskEvent;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.User;
import org.kie.internal.task.api.TaskModelProvider;
import org.jbpm.workflow.instance.WorkflowProcessInstance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ARCustomTaskEventListener extends DefaultTaskEventListener  {

    private static Logger logger = LoggerFactory.getLogger(ARCustomTaskEventListener.class);

    private static final String ENV_AR_END_POINT = System.getenv("SERVICE_AR_DEFAULT_ENDPOINT");

    private static final String PATH_ESCALACION = "/api/bpmaasorqhe/v1/obtenerEscalacionHEMule";

    //Variables Proceso
    private static final String CLASS_NAME = "com.bbva.arhe.horasextras.lstAprobador";
    private static final String VAR_PROCESS_APPROVER = "lstaprobadores";
    private static final String VAR_PROCESS_EMPLOYEE = "idUsuarioEmpleado";

    //Propiedades DataObject
    private static final String PROP_OBJECT_APPROVER = "aprobadores";
    private static final String PROP_OBJECT_EMPSUP = "idEmpleadoSup";
    private static final String PROP_OBJECT_EMPSUPDATE = "fechaAltaEmpleadoSup";

    private ClassLoader classLoader;

    public ARCustomTaskEventListener(){}
    public ARCustomTaskEventListener(ClassLoader classLoader)
    {
        this.classLoader = classLoader;
    }

    @Override
    public void afterTaskReassignedEvent(TaskEvent event) {

        Task ti = event.getTask();
        //Obtener siguiente usuario a Escalar y actualiza variable de proceso (listaAprobadores)
        ArrayList<User> listUsers = getPotentialOwners(ti.getTaskData().getProcessInstanceId());
        String initialAssignUser = listUsers.get(0).getId();

        logger.info("ProcessId-TaskId[" + ti.getTaskData().getProcessInstanceId() + "-" +ti.getId() + "] InitiallyAssignedUser: " + initialAssignUser);

        UserTaskService userTaskService = (UserTaskService)ServiceRegistry.get().service(ServiceRegistry.USER_TASK_SERVICE);

        for (User objUserN : listUsers) {
            userTaskService.delegate(ti.getId(), initialAssignUser, objUserN.getId());
        }

        logger.info("ProcessId-TaskId[" + ti.getTaskData().getProcessInstanceId() + "-" +ti.getId() + "] New List potOwners: " + ti.getPeopleAssignments().getPotentialOwners());

    }

    private ArrayList<User> getPotentialOwners(long  processInstanceId) {
        ArrayList<User> listUsers = new ArrayList<>();

        ProcessService adminProcessService = (ProcessService) ServiceRegistry.get().service(ServiceRegistry.PROCESS_SERVICE);
        WorkflowProcessInstance processInstance = (WorkflowProcessInstance)adminProcessService.getProcessInstance(processInstanceId);
        String idUsuarioEmpleado = (String)processInstance.getVariable(VAR_PROCESS_EMPLOYEE);
        Object lstAprobadores = processInstance.getVariable(VAR_PROCESS_APPROVER);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode nodeRoot = objectMapper.valueToTree(lstAprobadores);
        ArrayNode nodeAprobadores = (ArrayNode)nodeRoot.get(PROP_OBJECT_APPROVER);
        User objUser;

        if(nodeAprobadores != null && nodeAprobadores.size() > 0) {
            //Get last User
            String idEmpleadoSup = nodeAprobadores.get(nodeAprobadores.size() - 1).get(PROP_OBJECT_EMPSUP).asText();

            //Add All Users
            for (final JsonNode objNode : nodeAprobadores) {
                objUser = TaskModelProvider.getFactory().newUser(objNode.get(PROP_OBJECT_EMPSUP).asText());
                listUsers.add(objUser);
            }

            //Get Next User Escalado
            ResponseEscaladoDTO objEscalado = getEscalation(idUsuarioEmpleado, idEmpleadoSup);

            if(objEscalado != null) {
                objUser = TaskModelProvider.getFactory().newUser(objEscalado.getIdEmpleadoEsc());
                listUsers.add(objUser);

                //Add New User Escalado
                ObjectNode newNode = objectMapper.createObjectNode();
                newNode.put(PROP_OBJECT_EMPSUP, objEscalado.getIdEmpleadoEsc());
                newNode.put(PROP_OBJECT_EMPSUPDATE, objEscalado.getFecAltaEsc());
                nodeAprobadores.add(newNode);

                //Update Process variable
                try {
                    Class<?> clazz = Class.forName(CLASS_NAME, true, classLoader);

                    processInstance.setVariable(VAR_PROCESS_APPROVER, objectMapper.treeToValue(nodeRoot, clazz));

                } catch (IOException ie) {
                    throw new RuntimeException(ie.getMessage());
                } catch (ClassNotFoundException nex) {
                    throw new RuntimeException(nex.getMessage());
                }
            }
        }
        else {
            throw new RuntimeException("Exception lstAprobadores.aprobadores list is empty");
        }

        return listUsers;
    }


    private ResponseEscaladoDTO getEscalation(String idEmpleado, String idEmpSup) {

        Map<String, Object> response = new HashMap<String, Object>();
        String strQueryParams = "?LEGAJO_EMPLEADO="+ idEmpleado +"&LEGAJO_AUTORIZANTE=" + idEmpSup;
        String strPath = PATH_ESCALACION + strQueryParams;
        ResponseEscaladoDTO objEscalado = null;

        try {
            response = getRestClientGET(strPath).invoke();
            int responseStatus = (int)response.get("ResponseStatus");


            if(responseStatus == 200) {
                ObjectMapper objectMapper = new ObjectMapper();
                objEscalado = (ResponseEscaladoDTO)objectMapper.readValue(response.get("ResponseRawBody").toString(), ResponseEscaladoDTO.class);
            }
            else
                throw new RuntimeException("Exception when getting Escalado [" + response.get("ResponseRawBody").toString() + "]");

        }
        catch(Throwable ex) {
            logger.info("Error", ex);
            //throw new RuntimeException(ex);
        }

        return objEscalado;
    }

    private CoreRest getRestClientGET(String path) {
        CoreRest restClient = null;
        try {
            restClient = new CoreRest(
                    ENV_AR_END_POINT,
                    path,
                    ContentType.APPLICATION_JSON.getMimeType(),
                    HttpGet.METHOD_NAME,
                    null);
        }
        catch(Throwable ex) {
            throw new RuntimeException(ex);
        }

        return restClient;
    }

}