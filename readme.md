# AR Custom TaskEventListener
Implementación de DefaultTaskEventListener para obtener en tiempo de ejecución el usuario al que se tiene que escalar una tarea al expirar el tiempo de Reasignacion.

**IMPORTANTE:** Este TaskEventListener se apoya en una serie de pares clave-valor necesarios para su uso. El listado es el siguiente (los valores sirven como ejemplo):
	
* export SERVICE_AR_CALLBACK_ENDPOINT='http://dvi006-muleesb.arg.igrupobbva:9101'
* export SERVICE_AR_CALLBACK_CONNECTION_TIMEOUT='30000'
* export SERVICE_AR_CALLBACK_READ_TIMEOUT='30000'
* export SERVICE_AR_ID_SOCIEDAD='02'
* export SERVICE_AR_ID_PROCESO='54'
