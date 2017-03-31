package com.dronegcs.console_plugin.mission_editor;


import com.dronedb.persistence.scheme.Mission;

/**
 * Created by oem on 3/25/17.
 */
public interface ClosableMissionEditor extends MissionEditor {

    Mission open(Mission mission);

    Mission open(String missionName);

    Mission close(boolean shouldSave);
}
