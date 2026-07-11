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
package org.labkey.test.tests.targetedms;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.WebElement;

import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.util.PermissionsHelper.READER_ROLE;

/**
 * Tests the site-admin "Targeted MS Guest Access" setting (GuestAccessSettingsAction).
 *
 * With the master switch off (the default), guests can view the slow pages on public data as before.
 * With the master switch on, each checked action requires a login. Unchecking an action lets guests use
 * it again.
 *
 * Two kinds of action are covered, and they signal a blocked guest differently:
 * - Detail pages (showProtein, showPeptide) show an inline login view at the same URL, whose text is
 *   "Login to view this data".
 * - Chart-image endpoints (precursorChromatogramChart) cannot return that HTML, so they redirect a
 *   blocked guest to the standard login page. This is tested with a dummy id, because the gate runs
 *   before the id is looked up: a blocked guest lands on login.view, while an allowed guest stays on the
 *   chart URL (a "not found" page for the dummy id).
 *
 * This does not change the pages that always require a login for guests (e.g. the precursor
 * "all chromatograms" page), which stay blocked no matter what the switch is set to.
 */
@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 3)
public class TargetedMSGuestAccessTest extends TargetedMSTest
{
    private static final String SKY_FILE = "MRMer.zip";
    private static final String TARGET_PROTEIN = "YAL038W";
    private static final String TARGET_PEPTIDE = "LTSLNVVAGSDLR";
    private static final String LOGIN_MESSAGE = "Login to view this data";

    // Action keys as stored/posted by the settings page (must match the GuestAccessManager enum names).
    private static final String SHOW_PROTEIN = "showProtein";
    private static final String SHOW_PEPTIDE = "showPeptide";
    private static final String PRECURSOR_CHROMATOGRAM_CHART = "precursorChromatogramChart";

    private static final Locator MASTER_SWITCH = Locator.id("tms-require-login-master");

    // Captured during setup and read from the @Test method, which runs on a different instance than
    // @BeforeClass setup, so these must be static (instance fields would be null in the test method).
    private static String proteinUrl;   // showProtein detail page
    private static String peptideUrl;   // showPeptide detail page
    private static String chartUrl;     // precursorChromatogramChart image endpoint (dummy id)
    private static String requiresLoginUrl;  // PrecursorAllChromatogramsChartAction, always blocked for guests

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @BeforeClass
    public static void initProject()
    {
        TargetedMSGuestAccessTest init = getCurrentTest();
        init.doSetup();
    }

    @LogMethod
    private void doSetup()
    {
        setupFolder(FolderType.Experiment);
        importData(SKY_FILE);

        // Capture the URLs we will later request as a guest.
        goToDashboard();
        clickAndWait(Locator.linkWithText(SKY_FILE));
        clickAndWait(Locator.linkWithText(TARGET_PROTEIN));
        proteinUrl = getCurrentRelativeURL();

        // A chart-image endpoint in this same folder. A dummy id is enough because the login gate runs
        // before the id is looked up.
        chartUrl = WebTestHelper.buildURL("targetedms", getCurrentContainerPath(), PRECURSOR_CHROMATOGRAM_CHART,
                Map.of("id", "1"));

        clickAndWait(Locator.linkWithText(TARGET_PEPTIDE));
        peptideUrl = getCurrentRelativeURL();

        // Peptide -> precursor "all chromatograms" page, which always requires a login for guests.
        clickAndWait(Locator.linkWithImage("TransitionGroupLib.png"));
        requiresLoginUrl = getCurrentRelativeURL();

        // Make the data public so a guest has read access. The guest-access gate is what we are testing,
        // not the underlying read permission.
        goToProjectHome(getProjectName());
        new ApiPermissionsHelper(this).setSiteGroupPermissions("Guests", READER_ROLE);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        // Leave the site-wide switch off so it can't leak into other tests' guest access. The framework
        // already signs back in as the site admin before calling doCleanup, so we can just save. Best
        // effort: don't let a failure here block the project deletion in super.doCleanup.
        try
        {
            saveGuestAccessSettings(false);
        }
        catch (Exception ignored)
        {
        }
        super.doCleanup(afterTest);
    }

    @Test
    public void testGuestAccessToggle()
    {
        // 1. Master OFF (default): guests can view the pages and the chart; the always-on page is blocked.
        signOut();
        assertGuestAllowedPage(proteinUrl, TARGET_PROTEIN);
        assertGuestAllowedPage(peptideUrl, TARGET_PEPTIDE);
        assertGuestChartAllowed(chartUrl);
        assertGuestPageBlocked(requiresLoginUrl);

        // 2. Master ON, gate the two pages and the chart endpoint: guests are sent to login on all three.
        signIn();
        saveGuestAccessSettings(true, SHOW_PROTEIN, SHOW_PEPTIDE, PRECURSOR_CHROMATOGRAM_CHART);
        signOut();
        assertGuestPageBlocked(proteinUrl);
        assertGuestPageBlocked(peptideUrl);
        assertGuestChartBlocked(chartUrl);
        assertGuestPageBlocked(requiresLoginUrl);

        // 3. Gate only showPeptide: showProtein and the chart open back up, showPeptide stays blocked.
        signIn();
        saveGuestAccessSettings(true, SHOW_PEPTIDE);
        signOut();
        assertGuestAllowedPage(proteinUrl, TARGET_PROTEIN);
        assertGuestChartAllowed(chartUrl);
        assertGuestPageBlocked(peptideUrl);
        assertGuestPageBlocked(requiresLoginUrl); // always-on page stays blocked regardless of the switch

        // 4. Master OFF again: back to fully open.
        signIn();
        saveGuestAccessSettings(false);
        signOut();
        assertGuestAllowedPage(proteinUrl, TARGET_PROTEIN);
        assertGuestChartAllowed(chartUrl);
    }

    /**
     * Set the master switch and, when on, gate exactly the given action keys (any others are unchecked).
     * Must be signed in as a site admin. The per-action checkboxes are disabled by the page's script until
     * the master switch is on, so set the master switch first.
     */
    @LogMethod
    private void saveGuestAccessSettings(boolean masterEnabled, String... checkedActionKeys)
    {
        beginAt(WebTestHelper.buildURL("targetedms", "/", "guestAccessSettings"));
        setCheckbox(MASTER_SWITCH, masterEnabled);
        if (masterEnabled)
        {
            // Start from a known state: uncheck every action, then check just the requested ones.
            for (WebElement box : Locator.tagWithClass("input", "tms-require-login-action").findElements(getDriver()))
            {
                if (box.isSelected())
                    box.click();
            }
            for (String key : checkedActionKeys)
                setCheckbox(Locator.checkboxByNameAndValue("restrictedActions", key), true);
        }
        clickButton("Save");
    }

    /** A detail page a guest can view: no login view, and the expected content actually rendered. */
    private void assertGuestAllowedPage(String url, String expectedContent)
    {
        beginAt(url);
        // The login view renders "Login" as a link, so the phrase is split by a tag in the HTML source.
        // Check the rendered body text (as TargetedMSExperimentTest.verifyNoGuestAccessMessage does).
        assertFalse("Guest was unexpectedly shown the login view at " + url, getBodyText().contains(LOGIN_MESSAGE));
        assertTextPresent(expectedContent);
    }

    /** A detail page blocked for a guest: the inline login view is shown at the same URL. */
    private void assertGuestPageBlocked(String url)
    {
        beginAt(url);
        assertTrue("Expected the guest login view at " + url, getBodyText().contains(LOGIN_MESSAGE));
    }

    /** A chart-image endpoint a guest can reach: not redirected to the login page. */
    private void assertGuestChartAllowed(String url)
    {
        beginAt(url);
        assertFalse("Guest was unexpectedly redirected to login for " + url,
                getCurrentRelativeURL().contains("login.view"));
    }

    /** A chart-image endpoint blocked for a guest: redirected to the standard login page. */
    private void assertGuestChartBlocked(String url)
    {
        beginAt(url);
        assertTrue("Expected a redirect to the login page for " + url + ", but landed on " + getCurrentRelativeURL(),
                getCurrentRelativeURL().contains("login.view"));
    }
}
