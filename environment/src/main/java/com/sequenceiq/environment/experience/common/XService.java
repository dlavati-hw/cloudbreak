package com.sequenceiq.environment.experience.common;

import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.throwIfTrue;
import static java.util.stream.Collectors.toSet;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;
import com.sequenceiq.environment.experience.Experience;
import com.sequenceiq.environment.experience.ExperienceSource;
import com.sequenceiq.environment.experience.config.ExperienceServicesConfig;

@Service
public class XService implements Experience {

    private static final Logger LOGGER = LoggerFactory.getLogger(XService.class);

    private static final String DEFAULT_EXPERIENCE_PROTOCOL = "https";

    private final CommonExperienceConnectorService experienceConnectorService;

    private final Set<CommonExperience> configuredExperiences;

    private final CommonExperienceValidator experienceValidator;

    private final String experienceProtocol;

    public XService(@Value("${experience.scan.protocol}") String experienceProtocol, CommonExperienceConnectorService experienceConnectorService,
            ExperienceServicesConfig config, CommonExperienceValidator experienceValidator) {
        this.experienceValidator = experienceValidator;
        this.configuredExperiences = identifyConfiguredExperiences(config);
        this.experienceConnectorService = experienceConnectorService;
        this.experienceProtocol = StringUtils.isEmpty(experienceProtocol) ? DEFAULT_EXPERIENCE_PROTOCOL : experienceProtocol;
        LOGGER.debug("Experience connection protocol set to: {}", this.experienceProtocol);
    }

    @Override
    public boolean hasExistingClusterForEnvironment(EnvironmentExperienceDto environment) {
        LOGGER.debug("About to find connected experiences for environment which is in the following tenant: " + environment.getAccountId());
        Set<String> activeExperienceNames = environmentHasActiveExperience(environment.getCrn());
        if (activeExperienceNames.size() > 0) {
            String combinedNames = String.join(",", activeExperienceNames);
            LOGGER.info("The following experiences has connected to this env: [env: {}, experience(s): {}]", environment.getName(), combinedNames);
            return true;
        }
        return false;
    }

    @Override
    public ExperienceSource getSource() {
        return ExperienceSource.BASIC;
    }

    @Override
    public void deleteConnectedExperiences(EnvironmentExperienceDto dto) {

    }

    /**
     * Checks all the configured experiences for any existing workspace which has a connection with the given environment.
     * If so, it will return the set of the names of the given experience.
     *
     * @param environmentCrn the resource crn of the environment. It must not be null or empty.
     * @return the name of the experiences which has an active workspace for the given environment.
     * @throws IllegalArgumentException if environmentCrn is null or empty
     */
    private Set<String> environmentHasActiveExperience(@NotNull String environmentCrn) {
        throwIfTrue(StringUtils.isEmpty(environmentCrn), () -> new IllegalArgumentException("Unable to check environment - experience relation, since the " +
                "given environment crn is null or empty!"));
        Set<String> affectedExperiences;
        affectedExperiences = configuredExperiences
                .stream()
                .map(xp -> isExperienceActiveForEnvironment(xp.getName(), xp, environmentCrn))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toSet());
        return affectedExperiences;
    }

    private Optional<String> isExperienceActiveForEnvironment(String experienceName, CommonExperience xp, String environmentCrn) {
        LOGGER.debug("Checking whether the environment (crn: {}) has an active experience (name: {}) or not.", environmentCrn, experienceName);
        Set<String> entries = collectExperienceEntryNamesWhenItHasActiveWorkspaceForEnv(xp, environmentCrn);
        if (!entries.isEmpty()) {
            String entryNames = String.join(",", entries);
            LOGGER.info("The following experience ({}) has an active entry for the given environment! [entries: {}, environmentCrn: {}]",
                    experienceName, entryNames, environmentCrn);
            return Optional.of(experienceName);
        }
        return Optional.empty();
    }

    private Set<String> collectExperienceEntryNamesWhenItHasActiveWorkspaceForEnv(CommonExperience xp, String envCrn) {
        String pathToExperience = experienceProtocol + "://" + xp.getHostAddress() + ":" + xp.getPort() + xp.getInternalEnvEndpoint();
        return experienceConnectorService.getWorkspaceNamesConnectedToEnv(pathToExperience, envCrn);
    }

    private Set<CommonExperience> identifyConfiguredExperiences(ExperienceServicesConfig config) {
        Set<CommonExperience> experiences = config.getExperiences()
                .stream()
                .filter(this::isExperienceConfigured)
                .collect(toSet());
        if (experiences.isEmpty()) {
            LOGGER.info("There are no - properly - configured experience endpoints in environment service! If you would like to check them, specify them" +
                    " in the application.yml!");
            return Collections.emptySet();
        } else {
            String names = String.join(", ", new HashSet<>(experiences.stream().map(CommonExperience::getName).collect(toSet())));
            LOGGER.info("The following experience(s) have given for environment service: {}", names);
            return experiences;
        }
    }

    private boolean isExperienceConfigured(CommonExperience xp) {
        boolean filled = experienceValidator.isExperienceFilled(xp);
        if (!filled) {
            LOGGER.debug("The following experience has not filled properly: {}", xp.getName());
        }
        return filled;
    }

}
