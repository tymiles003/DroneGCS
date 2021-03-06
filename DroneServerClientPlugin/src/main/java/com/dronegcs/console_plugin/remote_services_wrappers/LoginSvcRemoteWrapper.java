package com.dronegcs.console_plugin.remote_services_wrappers;

import com.db.persistence.scheme.KeepAliveResponse;
import com.db.persistence.scheme.LoginRequest;
import com.db.persistence.scheme.LoginResponse;
import com.db.persistence.scheme.LogoutResponse;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.codec.binary.Base64;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.ConnectException;

import static com.db.persistence.scheme.LoginLogoutStatus.FAIL;

@Component
public class LoginSvcRemoteWrapper {

    @Autowired
    private RestClientHelper restClientHelper;

    private boolean keepAliveEnable = false;

    private final static Logger LOGGER = LoggerFactory.getLogger(LoginSvcRemoteWrapper.class);

    public LoginResponse login(LoginRequest loginRestRequest, String pass) {
        LoginResponse loginResponse;
        try {
            String loginTuple = loginRestRequest.getUserName() + ":" + pass;
            String encoding = new String(Base64.encodeBase64(loginTuple.getBytes()));
            restClientHelper.setHashedUsernamePassword(encoding);
            WebResource.Builder builder = restClientHelper.getWebResource("login");
            ObjectMapper mapper = new ObjectMapper();

            LOGGER.debug("Request to be send: {} " + mapper.writeValueAsString(loginRestRequest));
            ClientResponse response = builder.post(ClientResponse.class, mapper.writeValueAsString(loginRestRequest));
            loginResponse = resolveResponse(response, LoginResponse.class);
            LOGGER.debug("loginRestResponse: {}", loginResponse.getDate(), loginResponse.getMessage());

            LOGGER.debug("Activate Keep Alive");
            keepAliveEnable = true;
        }
//        catch (ConnectException e) {
//            LOGGER.error("Connection to server failed", e);
////            throw new RuntimeException(e); //TODO: handle connection issue nicely
//            loginResponse = new LoginResponse();
//            loginResponse.setReturnCode(FAIL);
//            loginResponse.setMessage(e.getMessage());
//            return loginResponse;
//        }
        catch (Exception e) {
//            LOGGER.error("Failed to read object", e);
            //throw new RuntimeException(e);
            loginResponse = new LoginResponse();
            loginResponse.setReturnCode(FAIL);
            loginResponse.setMessage(e.getMessage());
            return loginResponse;
        }

        return loginResponse;
    }

    public LogoutResponse logout() {
        try {
            WebResource.Builder builder = restClientHelper.getWebResource("logout");
            ClientResponse response = builder.post(ClientResponse.class);
            LogoutResponse resp =  resolveResponse(response, LogoutResponse.class);
//            System.out.println("logoutRestResponse: " + resp.getDate() + " " +  resp.getMessage());
            LOGGER.debug("logoutRestResponse: {} {}", resp.getDate(), resp.getMessage());
            return resp;
        }
        catch (ConnectException e) {
            LOGGER.error("Connection to server failed", e);
            throw new RuntimeException(e); //TODO: handle connection issue nicely
        }
        catch (Exception e) {
            LOGGER.error("Failed to read object", e);
            throw new RuntimeException(e);
        }
    }

    @Scheduled(fixedRate=30 * 1000)
    public void loginKeepAlive() throws Exception {
        if (!keepAliveEnable)
            return;
        System.out.println("Tik!");
        WebResource.Builder builder = restClientHelper.getWebResource("keepAlive");
        ClientResponse response = builder.post(ClientResponse.class);
        KeepAliveResponse resp =  resolveResponse(response, KeepAliveResponse.class);
        System.out.println("Server Time: " + resp.getMessage() + " ," + resp.getServerDate() + " ," + resp.getReturnCode());
        //TODO: Get server time
    }

    private <T extends Object> T resolveResponse(ClientResponse response, Class<T> clz) throws Exception {
        ClientResponse.Status status = response.getClientResponseStatus();

        switch (status) {
            case OK:
                break;
            default:
                throw new RuntimeException(status.getReasonPhrase() + "(" + status.getStatusCode() + ")");
        }

        if (!response.hasEntity())
            throw new RuntimeException(status.getReasonPhrase() + ", status:" + status.getStatusCode());

//        System.out.println(response.getHeaders());
//        System.out.println(response.getCookies());
//        System.out.println(response.getAllow());

        String jsonString = response.getEntity(String.class);
//        System.out.println(jsonString);
//        LOGGER.debug("Response: {}", jsonString);

        JSONObject jsonObject = new JSONObject(jsonString);
        T resp = restClientHelper.resolve(jsonObject, clz);
        return resp;
    }
}
