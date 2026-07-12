/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.targetedms;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.audit.AuditLogService;
import org.labkey.api.audit.provider.SiteSettingsAuditProvider;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.data.PropertyManager.PropertyMap;
import org.labkey.api.data.PropertyManager.WritablePropertyMap;
import org.labkey.api.security.User;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Site-admin setting that can require a login for slow targetedms pages that get hit by bots. A master
 * switch (off by default) plus one flag per action are stored as site properties on the root container.
 * When the master switch is on, each flagged action sends a guest to the login page instead of rendering.
 * When it is off, guests can open the pages as before.
 *
 * This setting does not change actions that already require a login:
 * PrecursorAllChromatogramsChartAction, MoleculePrecursorAllChromatogramsChartAction, ShowTransitionListAction
 */
public class GuestAccessManager
{
    // Site properties live in this category on the root container.
    private static final String CATEGORY = "TargetedMSGuestAccess";
    private static final String MASTER_KEY = "masterEnabled";
    private static final String TRUE = Boolean.TRUE.toString();

    /**
     * The actions a site admin can choose to require a login.
     * defaultChecked is used when nothing has been saved yet.
     *
     * The pages that draw many charts are checked by default. The single-chart image actions and the
     * precursor table are offered too but are off by default, so guests keep seeing inline charts and lists
     * during normal operation. An admin can also check those to block direct requests to them during an
     * bot attack (they show up in high volume because the detail pages embed many of them).
     */
    public enum RestrictableAction
    {
        showProtein("Protein details page (showProtein)", true),
        showPeptide("Peptide details page (showPeptide)", true),
        showMolecule("Small molecule details page (showMolecule)", true),
        showCalibrationCurves("Calibration curves page (showCalibrationCurves)", true),
        showPrecursorList("Document details page (showPrecursorList)", false),
        showPeakAreas("Peak areas chart (showPeakAreas)", false),
        showRetentionTimesChart("Retention times chart (showRetentionTimesChart)", false),
        precursorChromatogramChart("Precursor chromatogram (precursorChromatogramChart)", false),
        groupChromatogramChart("Protein chromatogram (groupChromatogramChart)", false);

        private final String _label;
        private final boolean _defaultChecked;

        RestrictableAction(String label, boolean defaultChecked)
        {
            _label = label;
            _defaultChecked = defaultChecked;
        }

        public String getLabel()
        {
            return _label;
        }

        public boolean isDefaultChecked()
        {
            return _defaultChecked;
        }
    }

    private GuestAccessManager()
    {
    }

    private static PropertyMap getProperties()
    {
        return PropertyManager.getProperties(ContainerManager.getRoot(), CATEGORY);
    }

    /** True when the master switch is on. */
    public static boolean isMasterEnabled()
    {
        return TRUE.equals(getProperties().get(MASTER_KEY));
    }

    /**
     * The saved (or default) checkbox state for an action, independent of the master toggle. Used to render
     * the settings page.
     */
    public static boolean isActionChecked(@NotNull RestrictableAction action)
    {
        String saved = getProperties().get(action.name());
        return saved == null ? action.isDefaultChecked() : TRUE.equals(saved);
    }

    /**
     * True when a guest opening this page should be sent to the login page: the master switch is on AND
     * this page's checkbox is checked. This is the single method the actions consult.
     */
    public static boolean isRestricted(@NotNull RestrictableAction action)
    {
        PropertyMap props = getProperties();
        if (!TRUE.equals(props.get(MASTER_KEY)))
            return false;
        String saved = props.get(action.name());
        return saved == null ? action.isDefaultChecked() : TRUE.equals(saved);
    }

    /** The set of currently-checked actions (independent of the master toggle). */
    public static Set<RestrictableAction> getCheckedActions()
    {
        // Read the property map once and reuse it rather than re-fetching per action.
        PropertyMap props = getProperties();
        Set<RestrictableAction> checked = EnumSet.noneOf(RestrictableAction.class);
        for (RestrictableAction action : RestrictableAction.values())
        {
            String saved = props.get(action.name());
            if (saved == null ? action.isDefaultChecked() : TRUE.equals(saved))
                checked.add(action);
        }
        return checked;
    }

    /**
     * Persist the settings on the root container and, if anything changed, write a site-settings audit entry
     * recording who changed it and the new state.
     */
    public static void save(@NotNull User user, boolean masterEnabled, @NotNull Set<RestrictableAction> checkedActions)
    {
        boolean oldMaster = isMasterEnabled();
        Set<RestrictableAction> oldChecked = getCheckedActions();

        // When the master switch is off, the per-action checkboxes are disabled on the settings page and
        // do not post, so preserve the previously saved per-action selections rather than overwriting them.
        Set<RestrictableAction> newChecked = masterEnabled ? checkedActions : oldChecked;

        WritablePropertyMap props = PropertyManager.getWritableProperties(ContainerManager.getRoot(), CATEGORY, true);
        props.put(MASTER_KEY, Boolean.toString(masterEnabled));
        for (RestrictableAction action : RestrictableAction.values())
            props.put(action.name(), Boolean.toString(newChecked.contains(action)));
        props.save();

        if (masterEnabled != oldMaster || !newChecked.equals(oldChecked))
        {
            // When the master switch is off, no actions are enforced even though per-action selections are
            // preserved, so report the effective (enforced) set rather than the saved checkboxes.
            String enforced = masterEnabled ? describe(newChecked) : "(none - master switch off)";
            String comment = "Targeted MS Guest Access settings updated. Master switch: " + (masterEnabled ? "on" : "off")
                    + ". Actions requiring login for guests: " + enforced + ".";
            SiteSettingsAuditProvider.SiteSettingsAuditEvent event =
                    new SiteSettingsAuditProvider.SiteSettingsAuditEvent(ContainerManager.getRoot(), comment);
            AuditLogService.get().addEvent(user, event);
        }
    }

    private static String describe(Set<RestrictableAction> actions)
    {
        if (actions.isEmpty())
            return "(none)";
        // Report in enum declaration order for a stable, readable list.
        List<String> names = new ArrayList<>();
        for (RestrictableAction action : RestrictableAction.values())
        {
            if (actions.contains(action))
                names.add(action.name());
        }
        return names.stream().collect(Collectors.joining(", "));
    }
}
