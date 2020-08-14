package com.bbva.ether.bpmaas.runtime.eventlisteners.ar;

import com.bbva.ether.bpmaas.runtime.connector.ar.CoreRest;
import com.bbva.ether.bpmaas.runtime.eventlisteners.ar.model.ResponseEscaladoDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jbpm.services.task.commands.TaskContext;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.jbpm.services.task.events.DefaultTaskEventListener;
import org.kie.api.task.TaskEvent;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.User;
import org.kie.internal.task.api.TaskModelProvider;

import org.jbpm.workflow.instance.WorkflowProcessInstance;

import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ARCustomTaskEventListener extends DefaultTaskEventListener  {

    private static final String ENV_AR_END_POINT = System.getenv("SERVICE_AR_CALLBACK_ENDPOINT");
    private static final String ENV_AR_CONNECTION_TIMEOUT = System.getenv("SERVICE_AR_CALLBACK_CONNECTION_TIMEOUT");
    private static final String ENV_AR_READ_TIMEOUT = System.getenv("SERVICE_AR_CALLBACK_READ_TIMEOUT");

    private static final String ENV_AR_ID_SOCIEDAD = System.getenv("SERVICE_AR_ID_SOCIEDAD"); //02
    private static final String ENV_AR_ID_PROCESO = System.getenv("SERVICE_AR_ID_PROCESO"); //54

    private static final String PATH_ESCALACION = "/api/bpmaasmeta4he/v1/sp/obtenerEscalacionProceso";
    private static final String QUERY_PARAM_FIJO;

    //Variables Proceso
    private static final String CLASS_NAME = "com.bbva.arhe.horasextras.lstAprobador";
    private static final String VAR_PROCESS_APPROVER = "lstAprobadores";
    private static final String VAR_PROCESS_EMPLOYEE = "idUsuarioEmpleado";
    private static final String VAR_PROCESS_EMPDATE = "fechaAltaUsuarioEmpleado";

    //Propiedades DataObject
    private static final String PROP_OBJECT_APPROVER = "aprobadores";
    private static final String PROP_OBJECT_EMPSUP = "idEmpleadoSup";
    private static final String PROP_OBJECT_EMPSUPDATE = "fechaAltaEmpleadoSup";

    private static final int CONNECTION_TIMEOUT;
    private static final int READ_TIMEOUT;


    private RuntimeManager runtimeManager = null;
    private ClassLoader classLoader;

    public ARCustomTaskEventListener(){}
    public ARCustomTaskEventListener(RuntimeManager runtimeManager, ClassLoader classLoader)
    {
        this.runtimeManager = runtimeManager;
        this.classLoader = classLoader;
    }

    static {
        CONNECTION_TIMEOUT 	= ENV_AR_CONNECTION_TIMEOUT != null ?  Integer.valueOf(ENV_AR_CONNECTION_TIMEOUT) : 3000;
        READ_TIMEOUT 		= ENV_AR_READ_TIMEOUT != null ? Integer.valueOf(ENV_AR_READ_TIMEOUT) : 3000;
        QUERY_PARAM_FIJO    = "?P_ID_SOCIEDAD_EMP=" + ENV_AR_ID_SOCIEDAD + "&P_ID_SOCIEDAD_SUP=" + ENV_AR_ID_SOCIEDAD + "&P_PROCESO=" + ENV_AR_ID_PROCESO;
    }

    @Override
    public void afterTaskReassignedEvent(TaskEvent event) {

        Task ti = event.getTask();
        ArrayList<User> listUsers = getPotentialOwners(ti.getTaskData().getProcessInstanceId());

        //Para obtener Task DataInputs (ti.getTaskData().getTaskInputVariables())
        //TaskContext context = (TaskContext)event.getTaskContext();
        //context.loadTaskVariables(ti);

        for (User objUserN : listUsers) {
            ti.getPeopleAssignments().getPotentialOwners().add(objUserN);
            System.out.println("List potOwners[" + objUserN.getId() + "]");
        }

    }

    private ArrayList<User> getPotentialOwners(long  processInstanceId) {
        ArrayList<User> listUsers = new ArrayList<>();

        RuntimeEngine engine = runtimeManager.getRuntimeEngine(null);
        KieSession ksession = engine.getKieSession();
        WorkflowProcessInstance processInstance = (WorkflowProcessInstance)ksession.getProcessInstance(processInstanceId);
        String idUsuarioEmpleado = (String)processInstance.getVariable(VAR_PROCESS_EMPLOYEE);
        String fechaAltaUsuarioEmpleado = (String)processInstance.getVariable(VAR_PROCESS_EMPDATE);
        Object lstAprobadores = processInstance.getVariable(VAR_PROCESS_APPROVER);
        String idEmpleadoSup = null;
        String fechaAltaEmpleadoSup = null;

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode nodeRoot = objectMapper.valueToTree(lstAprobadores);
        ArrayNode nodeAprobadores = (ArrayNode)nodeRoot.get(PROP_OBJECT_APPROVER);
        User objUser;

        if(nodeAprobadores != null && nodeAprobadores.size() > 0) {
            for (final JsonNode objNode : nodeAprobadores) {
                //Set last User
                idEmpleadoSup = objNode.get(PROP_OBJECT_EMPSUP).asText();
                fechaAltaEmpleadoSup = objNode.get(PROP_OBJECT_EMPSUPDATE).asText();
                //
                objUser = TaskModelProvider.getFactory().newUser(idEmpleadoSup);
                listUsers.add(objUser);
            }

            ResponseEscaladoDTO objEscalado = getEscalation(idUsuarioEmpleado, fechaAltaUsuarioEmpleado, idEmpleadoSup, fechaAltaEmpleadoSup);
            objUser = TaskModelProvider.getFactory().newUser("A" + objEscalado.getIdEmpleadoEsc());
            listUsers.add(objUser);

            ObjectNode newNode = objectMapper.createObjectNode();
            newNode.put(PROP_OBJECT_EMPSUP, "A" + objEscalado.getIdEmpleadoEsc());
            newNode.put(PROP_OBJECT_EMPSUPDATE, objEscalado.getFecAltaEsc());
            nodeAprobadores.add(newNode);

            //Update Process variable
            try {
                Class<?> clazz = Class.forName(CLASS_NAME, true, classLoader);

                processInstance.setVariable(VAR_PROCESS_APPROVER, objectMapper.treeToValue(nodeRoot, clazz));

            }
            catch (IOException ie) {
                throw new RuntimeException(ie.getMessage());
            }
            catch (ClassNotFoundException nex) {
                throw new RuntimeException(nex.getMessage());
            }

        }
        else {
            throw new RuntimeException("Exception lstAprobadores.aprobadores list is empty");
        }

        return listUsers;
    }


    private ResponseEscaladoDTO getEscalation(String idEmpleado, String fecAltaEmpleado, String idEmpSup, String fecAltaEmpSup) {

        Map<String, Object> response = new HashMap<String, Object>();
        String strQueryParams = "&P_ID_EMPLEADO_EMP=" + idEmpleado.replace("A", "")
                + "&P_FEC_ALTA_EMP=" + fecAltaEmpleado
                + "&P_ID_EMPLEADO_SUP=" + idEmpSup.replace("A", "")
                + "&P_FEC_ALTA_SUP=" + fecAltaEmpSup;
        String strPath = PATH_ESCALACION + QUERY_PARAM_FIJO + strQueryParams;
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
            throw new RuntimeException(ex);
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
            restClient.setConnectionTimeout(CONNECTION_TIMEOUT);
            restClient.setReadTimeout(READ_TIMEOUT);
        }
        catch(Throwable ex) {
            throw new RuntimeException(ex);
        }

        return restClient;
    }

}