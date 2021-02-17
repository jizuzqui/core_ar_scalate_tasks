package com.bbva.ether.bpmaas.runtime.eventlisteners.ar.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseEscaladoDTO implements java.io.Serializable {
    static final long serialVersionUID = 1L;

    @JsonProperty("P_ID_USUARIO")
    private String IdEmpleadoEsc;

    @JsonProperty("P_FEC_ALTA")
    private String FecAltaEsc;

    public String getIdEmpleadoEsc() {
        return IdEmpleadoEsc;
    }

    public void setIdEmpleadoEsc(String idEmpleadoEsc) {
        IdEmpleadoEsc = idEmpleadoEsc;
    }

    public String getFecAltaEsc() {
        return FecAltaEsc;
    }

    public void setFecAltaEsc(String fecAltaEsc) {
        FecAltaEsc = fecAltaEsc;
    }
}
