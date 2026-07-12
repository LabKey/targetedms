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
import org.jetbrains.annotations.Nullable;
import org.labkey.api.action.BaseViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.audit.AuditLogService;
import org.labkey.api.audit.provider.SiteSettingsAuditProvider;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.data.PropertyManager.PropertyMap;
import org.labkey.api.data.PropertyManager.WritablePropertyMap;
import org.labkey.api.security.User;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
     * The actions a site admin can choose to require a login. Each entry is keyed by its action class.
     *
     * The pages that draw many charts are checked by default. The single-chart image actions and the
     * precursor table are offered too but are off by default. An admin can also check those to block direct
     * requests to them during a bot attack (they show up in high volume during an attack because the detail
     * pages embed many of them).
     */
    public enum RestrictableAction
    {
        showProtein(TargetedMSController.ShowProteinAction.class, "Protein details page", true),
        showPeptide(TargetedMSController.ShowPeptideAction.class, "Peptide details page", true),
        showMolecule(TargetedMSController.ShowMoleculeAction.class, "Small molecule details page", true),
        showCalibrationCurve(TargetedMSController.ShowCalibrationCurveAction.class, "Calibration curve details page", true),
        showPrecursorList(TargetedMSController.ShowPrecursorListAction.class, "Document details page", false),
        showPeakAreas(TargetedMSController.ShowPeakAreasAction.class, "Peak areas chart", false),
        showRetentionTimesChart(TargetedMSController.ShowRetentionTimesChartAction.class, "Retention times chart", false),
        precursorChromatogramChart(TargetedMSController.PrecursorChromatogramChartAction.class, "Precursor chromatogram", false),
        groupChromatogramChart(TargetedMSController.GroupChromatogramChartAction.class, "Protein chromatogram", false);

        private final Class<? extends BaseViewAction<?>> _actionClass;
        private final String _description;
        private final boolean _defaultChecked;

        RestrictableAction(Class<? extends BaseViewAction<?>> actionClass, String description, boolean defaultChecked)
        {
            _actionClass = actionClass;
            _description = description;
            _defaultChecked = defaultChecked;
        }

        /**
         * The action's registered URL name (e.g. "showCalibrationCurve"), derived from the action class.
         */
        private String getActionName()
        {
            return SpringActionController.getActionName(_actionClass);
        }

        /** Settings-page label, e.g. "Calibration curve details page (showCalibrationCurve)". */
        public String getLabel()
        {
            return _description + " (" + getActionName() + ")";
        }

        private boolean isDefaultChecked()
        {
            return _defaultChecked;
        }

        /**
         * The restrictable action for this action class, or null if the class is not gated.
         */
        @Nullable
        public static RestrictableAction forClass(@NotNull Class<? extends BaseViewAction<?>> actionClass)
        {
            for (RestrictableAction action : values())
            {
                if (action._actionClass.equals(actionClass))
                    return action;
            }
            return null;
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
        // saved true/false is the admin's explicit choice (an explicit uncheck is respected). Absent means
        // never decided - e.g. an action added in a later release - so fall back to the action's default:
        // a new default-checked action is gated once the master switch is on, without waiting for a re-save.
        String saved = props.get(action.name());
        return saved == null ? action.isDefaultChecked() : TRUE.equals(saved);
    }

    /** The set of currently-checked actions (independent of the master toggle). */
    private static Set<RestrictableAction> getCheckedActions()
    {
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
                names.add(action.getActionName());
        }
        return String.join(", ", names);
    }
}
