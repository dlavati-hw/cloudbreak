package com.sequenceiq.it.cloudbreak.actor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.config.IntegrationTestConfiguration;

@ContextConfiguration(classes = IntegrationTestConfiguration.class, initializers = ConfigDataApplicationContextInitializer.class)
@Component
public class CloudbreakUserCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakUserCache.class);

    private Map<String, List<CloudbreakUser>> usersByAccount;

    @Value("${integrationtest.user.mow.accountKey:default}")
    private String realUmsUserAccount;

    @Value("${integrationtest.user.mow.environmentKey:dev}")
    private String realUmsUserEnvironment;

    public void setUsersByAccount(Map<String, List<CloudbreakUser>> users) {
        this.usersByAccount = users;
    }

    public Map<String, List<CloudbreakUser>> getUsersByAccount() {
        return usersByAccount;
    }

    public String getRealUmsUserAccount() {
        return realUmsUserAccount;
    }

    public String getRealUmsUserEnvironment() {
        return realUmsUserEnvironment;
    }

    public CloudbreakUser getByName(String name) {
        return getByName(name, getRealUmsUserEnvironment(), getRealUmsUserAccount());
    }

    public CloudbreakUser getByName(String name, String environmentKey, String accountKey) {
        if (usersByAccount == null) {
            initUsers(environmentKey, accountKey);
        }
        CloudbreakUser cloudbreakUser = usersByAccount.values().stream().flatMap(Collection::stream)
                .filter(user -> user.getDisplayName().equals(name)).findFirst()
                .orElseThrow(() -> new TestFailException(String.format("There is no real UMS test user with name %s", name)));
        LOGGER.info(" Requested real UMS user \nname: {} \ncrn: {} \naccessKey: {} \nsecretKey: {} \nadmin: {} ", cloudbreakUser.getDisplayName(),
                cloudbreakUser.getCrn(), cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey(), cloudbreakUser.getAdmin());
        return cloudbreakUser;
    }

    public String getAdminAccessKeyByAccountId(String accountId) {
        return getAdminAccessKeyByAccountId(accountId, getRealUmsUserEnvironment(), getRealUmsUserAccount());
    }

    public String getAdminAccessKeyByAccountId(String accountId, String environmentKey, String accountKey) {
        if (usersByAccount == null) {
            initUsers(environmentKey, accountKey);
        }
        String userAccessKey = usersByAccount.get(accountId).stream()
                .filter(CloudbreakUser::getAdmin).findFirst()
                .orElseThrow(() -> new TestFailException(String.format("There is no real UMS account admin for account %s", accountId))).getAccessKey();
        LOGGER.info(" Requested real UMS admin accessKey: {} for account: {} ", userAccessKey, accountId);
        return userAccessKey;
    }

    public void initUsers(String environmentKey, String accountKey) {
        String userConfigPath = "ums-users/api-credentials.json";
        LOGGER.info("Real UMS environment: {} and account: {}", environmentKey, accountKey);
        try {
            String accountId = null;
            List<CloudbreakUser> cloudbreakUsers = new ArrayList<CloudbreakUser>();
            JSONObject usersByEnvAndAcc = new JSONObject(FileReaderUtils.readFileFromClasspathQuietly(userConfigPath));
            JSONArray devEnvironment = usersByEnvAndAcc.getJSONArray(environmentKey);
            for (int i = 0; i < devEnvironment.length(); i++) {
                JSONObject jsonObject1 = (JSONObject) devEnvironment.get(i);
                JSONArray jsonarray1 = (JSONArray) jsonObject1.get(accountKey);
                for (int j = 0; j < jsonarray1.length(); j++) {
                    accountId = Crn.fromString(((JSONObject) jsonarray1.get(j)).getString("crn")).getAccountId();
                    String displayName = ((JSONObject) jsonarray1.get(j)).getString("displayName");
                    String desc = ((JSONObject) jsonarray1.get(j)).getString("desc");
                    String crn = ((JSONObject) jsonarray1.get(j)).getString("crn");
                    String accessKey = ((JSONObject) jsonarray1.get(j)).getString("accessKey");
                    String secretKey = ((JSONObject) jsonarray1.get(j)).getString("secretKey");
                    boolean admin = Boolean.parseBoolean(((JSONObject) jsonarray1.get(j)).getString("admin"));
                    cloudbreakUsers.add(new CloudbreakUser(accessKey, secretKey, displayName, crn, desc, admin));
                }
            }
            setUsersByAccount(Map.of(accountId, cloudbreakUsers));
        } catch (JSONException e) {
            throw new RuntimeException(String.format("Can't read file: %s It's possible you did run make fetch-secrets", userConfigPath));
        }
        usersByAccount.values().stream().flatMap(Collection::stream).forEach(user -> {
            LOGGER.info(" Initialized real UMS user \nname: {} \ncrn: {} \naccessKey: {} \nsecretKey: {} \nadmin: {} ", user.getDisplayName(), user.getCrn(),
                    user.getAccessKey(), user.getSecretKey(), user.getAdmin());
            CloudbreakUser.validateRealUmsUser(user);
        });
    }

    public boolean isInitialized() {
        return usersByAccount != null && !usersByAccount.isEmpty();
    }
}