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

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
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
 *   blocked guest to the standard login page. A blocked guest lands on login.view (the gate runs before
 *   the id is looked up), while an allowed guest renders the real chart at the same URL.
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
    private static final String SHOW_PRECURSOR_LIST = "showPrecursorList";
    private static final String PRECURSOR_CHROMATOGRAM_CHART = "precursorChromatogramChart";

    private static final Locator MASTER_SWITCH = Locator.id("tms-require-login-master");

    // Captured during setup and read from the @Test method.
    private static String proteinUrl;   // showProtein detail page
    private static String peptideUrl;   // showPeptide detail page
    private static String chartUrl;     // precursorChromatogramChart image endpoint
    private static String requiresLoginUrl;  // PrecursorAllChromatogramsChartAction, always blocked for guests
    private static String precursorListUrl;        // showPrecursorList HTML page (a QueryView action)
    private static String precursorListExportUrl;  // its TSV export URL (dispatched before the HTML view)

    // The site-wide Guest Access state as it was before this test, so the @After restore can put it back
    // exactly (these are root-container properties shared across the whole server, not scoped to this test's
    // folder). Restored in @After rather than doCleanup so it runs even when project cleanup is skipped
    // (clean=false).
    private static boolean origMasterEnabled;
    private static List<String> origCheckedKeys;

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @BeforeClass
    public static void initProject() throws Exception
    {
        TargetedMSGuestAccessTest init = getCurrentTest();
        init.doSetup();
    }

    @LogMethod
    private void doSetup() throws Exception
    {
        // Remember the site-wide Guest Access settings before touching anything, so they can be restored after the test.
        captureOriginalGuestAccessSettings();

        setupFolder(FolderType.Experiment);
        importData(SKY_FILE);

        // Capture the URLs we will later request as a guest.
        goToDashboard();
        clickAndWait(Locator.linkWithText(SKY_FILE));

        // The document's default page is the precursor list (showPrecursorList, a QueryView action). Capture
        // it and its TSV export URL.
        precursorListUrl = getCurrentRelativeURL();
        precursorListExportUrl = precursorListUrl + (precursorListUrl.contains("?") ? "&" : "?") + "exportType=exportRowsTsv";

        clickAndWait(Locator.linkWithText(TARGET_PROTEIN));
        proteinUrl = getCurrentRelativeURL();

        clickAndWait(Locator.linkWithText(TARGET_PEPTIDE));
        peptideUrl = getCurrentRelativeURL();

        // Look up a real precursorChromInfoId from the database and build the chart URL from it. A real id
        // renders an actual chart in the master-off "allowed" case; the gate still runs before the id lookup,
        // so the master-on case redirects to login regardless of the id.
        SelectRowsCommand chromInfoQuery = new SelectRowsCommand("targetedms", "PrecursorChromInfo");
        chromInfoQuery.setColumns(List.of("Id"));
        chromInfoQuery.setMaxRows(1);
        SelectRowsResponse chromInfoRows = chromInfoQuery.execute(createDefaultConnection(), getCurrentContainerPath());
        assertFalse("Expected at least one PrecursorChromInfo row", chromInfoRows.getRows().isEmpty());
        Number chromInfoId = (Number) chromInfoRows.getRows().getFirst().get("Id");
        chartUrl = WebTestHelper.buildURL("targetedms", getCurrentContainerPath(), PRECURSOR_CHROMATOGRAM_CHART,
                Map.of("id", String.valueOf(chromInfoId.longValue())));

        // Peptide -> precursor "all chromatograms" page, which always requires a login for guests.
        clickAndWait(Locator.linkWithImage("TransitionGroupLib.png"));
        requiresLoginUrl = getCurrentRelativeURL();

        // Make the data public so a guest has read access. The guest-access gate is what we are testing,
        // not the underlying read permission.
        goToProjectHome(getProjectName());
        new ApiPermissionsHelper(this).setSiteGroupPermissions("Guests", READER_ROLE);
    }

    /** Read the current site-wide Guest Access state from the settings page (must be signed in as site admin). */
    private void captureOriginalGuestAccessSettings()
    {
        beginAt(WebTestHelper.buildURL("targetedms", "/", "guestAccessSettings"));
        origMasterEnabled = MASTER_SWITCH.findElement(getDriver()).isSelected();
        origCheckedKeys = new ArrayList<>();
        for (WebElement box : Locator.tagWithClass("input", "tms-require-login-action").findElements(getDriver()))
        {
            if (box.isSelected())
                origCheckedKeys.add(box.getAttribute("value"));
        }
    }

    @After
    public void restoreGuestAccessSettings()
    {
        // Put the site-wide Guest Access settings back the way the test found them. This runs in @After,
        // not doCleanup, so it still happens when project cleanup is skipped (clean=false).
        if (origCheckedKeys == null)
            return; // setup did not get far enough to capture the originals

        // The test signs out at the end, and the settings page requires a site admin, so sign back in first.
        ensureSignedInAsPrimaryTestUser();

        // The per-action checkboxes only post while the master switch is on, so re-apply the original
        // selections with it on, then set the master switch back to its original value.
        saveGuestAccessSettings(true, origCheckedKeys.toArray(new String[0]));
        if (!origMasterEnabled)
            saveGuestAccessSettings(false);
    }

    @Test
    public void testGuestAccessToggle()
    {
        // 1. Master OFF: establish the baseline explicitly rather than trusting the ambient site-wide state.
        //    The master switch is a root-container property shared across the whole server, so a prior aborted
        //    run could leave it on; set it off here (we are still signed in as the site admin) so the "guests
        //    can view" checks below are deterministic. Guests can then view the pages and the chart, while the
        //    always-on page stays blocked.
        saveGuestAccessSettings(false);
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
        assertGuestAllowedPage(peptideUrl, TARGET_PEPTIDE);
        assertGuestChartAllowed(chartUrl);

        // 5. A QueryView action's export URL is a separate direct request, not a link reached through the HTML
        //    page: showPrecursorList.view?...&exportType=exportRowsTsv serves a TSV. QueryViewAction.getView
        //    dispatches that export branch before it would reach getHtmlView, so a client requesting the export
        //    URL directly (e.g. a crawler replaying or constructing it) bypasses a gate placed in getHtmlView.
        //    Gating showPrecursorList in getView closes that: a guest is sent to login for both the page and a
        //    direct export request.
        assertGuestAllowedPage(precursorListUrl, TARGET_PROTEIN);
        assertGuestNotLoginGated(precursorListExportUrl);
        signIn();
        saveGuestAccessSettings(true, SHOW_PRECURSOR_LIST);
        signOut();
        assertGuestPageBlocked(precursorListUrl);
        assertGuestPageBlocked(precursorListExportUrl);
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

    /** A URL a guest is not login-gated on: the login view is not shown (the response may be data or not-found). */
    private void assertGuestNotLoginGated(String url)
    {
        beginAt(url);
        assertFalse("Guest was unexpectedly shown the login view at " + url, getBodyText().contains(LOGIN_MESSAGE));
    }

    /** A chart-image endpoint a guest can reach: an actual chart image is served, not a login redirect. */
    private void assertGuestChartAllowed(String url)
    {
        beginAt(url);
        assertFalse("Guest was unexpectedly redirected to login for " + url,
                getCurrentRelativeURL().contains("login.view"));
        // Confirm a real chart image was served (not a not-found page). This also verifies the chart id is
        // valid for this folder: a wrong id would render an HTML not-found page instead of an image.
        String contentType = (String) executeScript("return document.contentType;");
        assertTrue("Expected a chart image at " + url + " but got content type " + contentType,
                contentType != null && contentType.startsWith("image/"));
    }

    /** A chart-image endpoint blocked for a guest: redirected to the standard login page. */
    private void assertGuestChartBlocked(String url)
    {
        beginAt(url);
        assertTrue("Expected a redirect to the login page for " + url + ", but landed on " + getCurrentRelativeURL(),
                getCurrentRelativeURL().contains("login.view"));
    }
}
